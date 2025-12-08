@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger
import com.altcraft.sdk.additional.StringBuilder
import com.altcraft.sdk.additional.StringBuilder.deletedMobileEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedPushEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedSubscriptionsMsg
import com.altcraft.sdk.core.Retry
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.*
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * RoomRequestUnitTest
 *
 * Positive scenarios:
 *  - test_1: entityInsert inserts SubscribeEntity via DAO.
 *  - test_2: entityInsert inserts PushEventEntity via DAO.
 *  - test_3: entityDelete deletes SubscribeEntity via DAO.
 *  - test_4: entityDelete deletes PushEventEntity via DAO.
 *  - test_5: isRetryLimit SubscribeEntity over limit → emits event & deletes.
 *  - test_6: isRetryLimit SubscribeEntity not over → increases retryCount.
 *  - test_7: isRetryLimit PushEventEntity over limit → emits event & deletes (with uid).
 *  - test_8: isRetryLimit PushEventEntity not over → increases retryCount.
 *  - test_9: clearOldPushEventsFromRoom when >500 → deletes oldest 100 and logs.
 *  - test_10: clearOldPushEventsFromRoom when <=500 → no action.
 *  - test_11: entityInsert inserts MobileEventEntity via DAO.
 *  - test_12: entityDelete deletes MobileEventEntity via DAO.
 *  - test_13: isRetryLimit MobileEventEntity over limit → emits event & deletes (with name).
 *  - test_14: isRetryLimit MobileEventEntity not over → increases retryCount.
 *  - test_15: clearOldMobileEventsFromRoom when >500 → deletes oldest 100 and logs.
 *  - test_16: clearOldMobileEventsFromRoom when <=500 → no action.
 *  - test_19: clearOldSubscriptionsFromRoom when >500 → deletes oldest 100 and logs.
 *  - test_20: clearOldSubscriptionsFromRoom when <=500 → no action.
 *  - test_21: roomOverflowControl calls all cleanup functions.
 *  - test_22: allMobileEventsByTag returns events and completes snapshot.
 *  - test_23: allSubscriptionsByTag returns subscriptions and completes snapshot.
 *
 * Negative scenarios:
 *  - test_17: entityInsert with unsupported type → emits Events.error.
 *  - test_18: entityDelete with unsupported type → emits Events.error.
 */
class RoomRequestUnitTest {

    private lateinit var ctx: Context
    private lateinit var sdkDB: SDKdb
    private lateinit var dao: DAO

    private lateinit var mobileEventsSnapshotDeferred: CompletableDeferred<Unit>
    private lateinit var subscribeSnapshotDeferred: CompletableDeferred<Unit>

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        mockkObject(SDKdb)
        sdkDB = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        every { SDKdb.getDb(any()) } returns sdkDB
        every { sdkDB.request() } returns dao

        mockkObject(Events)
        every { Events.event(any(), any<Pair<Int, String>>(), any()) } answers {
            val fn = firstArg<String>()
            val pair = secondArg<Pair<Int, String>>()
            val value = thirdArg<Map<String, Any?>?>()
            DataClasses.Event(fn, pair.first, pair.second, value)
        }
        every { Events.error(any(), any<Pair<Int, String>>()) } returns
                DataClasses.Error("fn", 400, "err", null)
        every { Events.error(any(), any<Throwable>()) } returns
                DataClasses.Error("fn", 400, "err", null)

        mockkObject(Logger)
        every { Logger.log(any()) } returns Unit

        mockkObject(StringBuilder)
        every { deletedPushEventsMsg(any()) } answers { "push-${firstArg<Int>()}" }
        every { deletedMobileEventsMsg(any()) } answers { "mobile-${firstArg<Int>()}" }
        every { deletedSubscriptionsMsg(any()) } answers { "sub-${firstArg<Int>()}" }

        mockkObject(Retry)
        mobileEventsSnapshotDeferred = mockk(relaxed = true)
        subscribeSnapshotDeferred = mockk(relaxed = true)
        every { Retry.mobileEventsDbSnapshotTaken } returns mobileEventsSnapshotDeferred
        every { Retry.pushSubscribeDbSnapshotTaken } returns subscribeSnapshotDeferred
    }

    @After
    fun tearDown() = unmockkAll()

    private fun sub(uid: String = "sub-uid", retry: Int = 1, max: Int = 3) = SubscribeEntity(
        userTag = "tag",
        status = SUBSCRIBED,
        sync = null,
        profileFields = JsonNull,
        customFields = JsonNull,
        cats = null,
        replace = null,
        skipTriggers = null,
        uid = uid,
        time = System.currentTimeMillis() / 1000,
        retryCount = retry,
        maxRetryCount = max
    )

    private fun push(uid: String = "push-uid", retry: Int = 1, max: Int = 3) = PushEventEntity(
        uid = uid,
        type = "opened",
        time = System.currentTimeMillis() / 1000,
        retryCount = retry,
        maxRetryCount = max
    )

    private fun mobile(
        id: Long = 0L,
        name: String = "evt",
        retry: Int = 1,
        max: Int = 3
    ) = MobileEventEntity(
        id = id,
        userTag = "tag",
        timeZone = 0,
        time = System.currentTimeMillis(),
        sid = "sid",
        altcraftClientID = "cid",
        eventName = name,
        payload = null,
        matching = null,
        matchingType = null,
        profileFields = null,
        subscription = null,
        sendMessageId = "smid",
        utmTags = null,
        retryCount = retry,
        maxRetryCount = max
    )

    /** test_1: entityInsert inserts SubscribeEntity via DAO */
    @Test
    fun entityInsert_subscribe_callsDao() = runBlocking {
        val e = sub()
        RoomRequest.entityInsert(ctx, e)
        coVerify { dao.insertSubscribe(e) }
    }

    /** test_2: entityInsert inserts PushEventEntity via DAO */
    @Test
    fun entityInsert_push_callsDao() = runBlocking {
        val e = push()
        RoomRequest.entityInsert(ctx, e)
        coVerify { dao.insertPushEvent(e) }
    }

    /** test_11: entityInsert inserts MobileEventEntity via DAO */
    @Test
    fun entityInsert_mobile_callsDao() = runBlocking {
        val e = mobile()
        RoomRequest.entityInsert(ctx, e)
        coVerify { dao.insertMobileEvent(e) }
    }

    /** test_17: entityInsert with unsupported type → emits Events.error */
    @Test
    fun entityInsert_unsupported_emitsError() = runBlocking {
        RoomRequest.entityInsert(ctx, "unsupported")
        verify { Events.error(eq("entityInsert"), any<Pair<Int, String>>()) }
    }

    /** test_3: entityDelete deletes SubscribeEntity via DAO */
    @Test
    fun entityDelete_subscribe_callsDao() = runBlocking {
        val e = sub(uid = "X")
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deleteSubscribeByUid("X") }
    }

    /** test_4: entityDelete deletes PushEventEntity via DAO */
    @Test
    fun entityDelete_push_callsDao() = runBlocking {
        val e = push(uid = "Y")
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deletePushEventByUid("Y") }
    }

    /** test_12: entityDelete deletes MobileEventEntity via DAO */
    @Test
    fun entityDelete_mobile_callsDao() = runBlocking {
        val e = mobile(id = 777)
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deleteMobileEventById(777) }
    }

    /** test_18: entityDelete with unsupported type → emits Events.error */
    @Test
    fun entityDelete_unsupported_emitsError() = runBlocking {
        RoomRequest.entityDelete(sdkDB, 12345)
        verify { Events.error(eq("entityDelete"), any<Pair<Int, String>>()) }
    }

    /** test_5: isRetryLimit SubscribeEntity over limit → emits event & deletes */
    @Test
    fun isRetryLimit_subscribe_overLimit_deletes_and_emitsEvent() = runBlocking {
        val e = sub(retry = 5, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertTrue("Return value must be true", result)
        coVerify { dao.deleteSubscribeByUid(e.uid) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 480 },
                any()
            )
        }
    }

    /** test_6: isRetryLimit SubscribeEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_subscribe_notOver_incrementsRetry() = runBlocking {
        val e = sub(retry = 2, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertFalse("Return value must be false", result)
        coVerify { dao.increaseSubscribeRetryCount(e.uid, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deleteSubscribeByUid(any()) }
    }

    /** test_7: isRetryLimit PushEventEntity over limit → emits event & deletes (with uid) */
    @Test
    fun isRetryLimit_push_overLimit_deletes_and_emitsEvent_withUid() = runBlocking {
        val e = push(retry = 10, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertTrue("Return value must be true", result)
        coVerify { dao.deletePushEventByUid(e.uid) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 484 },
                match { it["uid"] == e.uid }
            )
        }
    }

    /** test_8: isRetryLimit PushEventEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_push_notOver_incrementsRetry() = runBlocking {
        val e = push(retry = 0, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertFalse("Return value must be false", result)
        coVerify { dao.increasePushEventRetryCount(e.uid, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deletePushEventByUid(any()) }
    }

    /** test_13: isRetryLimit MobileEventEntity over limit → emits event & deletes (with name) */
    @Test
    fun isRetryLimit_mobile_overLimit_deletes_and_emitsEvent_withName() = runBlocking {
        val e = mobile(id = 99L, name = "purchase", retry = 9, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertTrue("Return value must be true", result)
        coVerify { dao.deleteMobileEventById(99L) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 485 },
                match<Map<String, Any?>> { it[NAME] == "purchase" }
            )
        }
    }

    /** test_14: isRetryLimit MobileEventEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_mobile_notOver_incrementsRetry() = runBlocking {
        val e = mobile(id = 5L, name = "open", retry = 1, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertFalse("Return value must be false", result)
        coVerify { dao.increaseMobileEventRetryCount(5L, 2) }
        coVerify(exactly = 0) { dao.deleteMobileEventById(any()) }
    }

    /** test_9: clearOldPushEventsFromRoom when >500 → deletes oldest 100 and logs */
    @Test
    fun clearOldPushEventsFromRoom_overThreshold_deletesOldest100_and_logs() = runBlocking {
        coEvery { dao.getPushEventCount() } returnsMany listOf(501, 401)
        val oldest = (1..100).map { PushEventEntity(uid = "u$it", type = "t") }
        coEvery { dao.getOldestPushEvents(100) } returns oldest
        coJustRun { dao.deletePushEvents(oldest) }

        RoomRequest.clearOldPushEventsFromRoom(sdkDB)

        coVerify { dao.getPushEventCount() }
        coVerify { dao.getOldestPushEvents(100) }
        coVerify { dao.deletePushEvents(oldest) }
        verify { deletedPushEventsMsg(401) }
        verify { Logger.log("push-401") }
    }

    /** test_10: clearOldPushEventsFromRoom when <=500 → no action */
    @Test
    fun clearOldPushEventsFromRoom_underThreshold_noop() = runBlocking {
        coEvery { dao.getPushEventCount() } returns 120

        RoomRequest.clearOldPushEventsFromRoom(sdkDB)

        coVerify { dao.getPushEventCount() }
        coVerify(exactly = 0) { dao.getOldestPushEvents(any()) }
        coVerify(exactly = 0) { dao.deletePushEvents(any()) }
        verify(exactly = 0) { deletedPushEventsMsg(any()) }
        verify(exactly = 0) { Logger.log(any()) }
    }

    /** test_15: clearOldMobileEventsFromRoom when >500 → deletes oldest 100 and logs */
    @Test
    fun clearOldMobileEventsFromRoom_overThreshold_deletesOldest100_and_logs() = runBlocking {
        coEvery { dao.getMobileEventCount() } returnsMany listOf(700, 350)

        val oldest = (1..100).map {
            MobileEventEntity(
                id = it.toLong(),
                userTag = "tag$it",
                timeZone = 0,
                time = System.currentTimeMillis(),
                sid = "sid$it",
                altcraftClientID = "cid$it",
                eventName = "evt$it",
                payload = null,
                matching = null,
                matchingType = null,
                profileFields = null,
                subscription = null,
                sendMessageId = "smid$it",
                utmTags = null
            )
        }
        coEvery { dao.getOldestMobileEvents(100) } returns oldest
        coJustRun { dao.deleteMobileEvents(oldest) }

        RoomRequest.clearOldMobileEventsFromRoom(sdkDB)

        coVerify { dao.getMobileEventCount() }
        coVerify { dao.getOldestMobileEvents(100) }
        coVerify { dao.deleteMobileEvents(oldest) }
        verify { deletedMobileEventsMsg(350) }
        verify { Logger.log("mobile-350") }
    }

    /** test_16: clearOldMobileEventsFromRoom when <=500 → no action */
    @Test
    fun clearOldMobileEventsFromRoom_underThreshold_noop() = runBlocking {
        coEvery { dao.getMobileEventCount() } returns 42

        RoomRequest.clearOldMobileEventsFromRoom(sdkDB)

        coVerify { dao.getMobileEventCount() }
        coVerify(exactly = 0) { dao.getOldestMobileEvents(any()) }
        coVerify(exactly = 0) { dao.deleteMobileEvents(any()) }
        verify(exactly = 0) { deletedMobileEventsMsg(any()) }
        verify(exactly = 0) { Logger.log(any()) }
    }

    /** test_19: clearOldSubscriptionsFromRoom when >500 → deletes oldest 100 and logs */
    @Test
    fun clearOldSubscriptionsFromRoom_overThreshold_deletesOldest100_and_logs() = runBlocking {
        coEvery { dao.getSubscribeCount() } returnsMany listOf(550, 430)
        val oldest = (1..100).map { sub(uid = "s$it") }
        coEvery { dao.getOldestSubscriptions(100) } returns oldest
        coJustRun { dao.deleteSubscriptions(oldest) }

        RoomRequest.clearOldSubscriptionsFromRoom(sdkDB)

        coVerify { dao.getSubscribeCount() }
        coVerify { dao.getOldestSubscriptions(100) }
        coVerify { dao.deleteSubscriptions(oldest) }
        verify { deletedSubscriptionsMsg(430) }
        verify { Logger.log("sub-430") }
    }

    /** test_20: clearOldSubscriptionsFromRoom when <=500 → no action */
    @Test
    fun clearOldSubscriptionsFromRoom_underThreshold_noop() = runBlocking {
        coEvery { dao.getSubscribeCount() } returns 120

        RoomRequest.clearOldSubscriptionsFromRoom(sdkDB)

        coVerify { dao.getSubscribeCount() }
        coVerify(exactly = 0) { dao.getOldestSubscriptions(any()) }
        coVerify(exactly = 0) { dao.deleteSubscriptions(any()) }
        verify(exactly = 0) { deletedSubscriptionsMsg(any()) }
        verify(exactly = 0) { Logger.log(any()) }
    }

    /**
     * test_21: roomOverflowControl calls all cleanup functions.
     *
     * Here we do NOT mock RoomRequest itself to avoid coroutine machinery issues.
     * Instead we stub DAO so that each inner cleanup branch is executed at least once.
     */
    @Test
    fun roomOverflowControl_callsAllCleanupMethods() = runBlocking {
        // Subscriptions branch
        coEvery { dao.getSubscribeCount() } returnsMany listOf(600, 450)
        val oldestSubs = (1..3).map { sub(uid = "s$it") }
        coEvery { dao.getOldestSubscriptions(100) } returns oldestSubs
        coJustRun { dao.deleteSubscriptions(oldestSubs) }

        // Mobile events branch
        coEvery { dao.getMobileEventCount() } returnsMany listOf(700, 500)
        val oldestMobiles = (1..3).map {
            mobile(
                id = it.toLong(),
                name = "m$it"
            )
        }
        coEvery { dao.getOldestMobileEvents(100) } returns oldestMobiles
        coJustRun { dao.deleteMobileEvents(oldestMobiles) }

        // Push events branch
        coEvery { dao.getPushEventCount() } returnsMany listOf(800, 480)
        val oldestPushes = (1..3).map { PushEventEntity(uid = "p$it", type = "type") }
        coEvery { dao.getOldestPushEvents(100) } returns oldestPushes
        coJustRun { dao.deletePushEvents(oldestPushes) }

        RoomRequest.roomOverflowControl(sdkDB)

        // Subscriptions cleanup was invoked
        coVerify { dao.getSubscribeCount() }
        coVerify { dao.getOldestSubscriptions(100) }
        coVerify { dao.deleteSubscriptions(oldestSubs) }

        // Mobile events cleanup was invoked
        coVerify { dao.getMobileEventCount() }
        coVerify { dao.getOldestMobileEvents(100) }
        coVerify { dao.deleteMobileEvents(oldestMobiles) }

        // Push events cleanup was invoked
        coVerify { dao.getPushEventCount() }
        coVerify { dao.getOldestPushEvents(100) }
        coVerify { dao.deletePushEvents(oldestPushes) }
    }

    /** test_22: allMobileEventsByTag returns events and completes snapshot */
    @Test
    fun allMobileEventsByTag_returnsEvents_andCompletesSnapshot() = runBlocking {
        val tag = "user-tag"
        val list = listOf(
            mobile(id = 1L, name = "evt1"),
            mobile(id = 2L, name = "evt2")
        )

        coEvery { dao.allMobileEventsByTag(tag) } returns list

        val result = RoomRequest.allMobileEventsByTag(sdkDB, tag)

        assertEquals(list, result)
        coVerify { dao.allMobileEventsByTag(tag) }
        verify { mobileEventsSnapshotDeferred.complete(Unit) }
    }

    /** test_23: allSubscriptionsByTag returns subscriptions and completes snapshot */
    @Test
    fun allSubscriptionsByTag_returnsSubscriptions_andCompletesSnapshot() = runBlocking {
        val tag = "user-tag"
        val list = listOf(
            sub(uid = "s1"),
            sub(uid = "s2")
        )

        coEvery { dao.allSubscriptionsByTag(tag) } returns list

        val result = RoomRequest.allSubscriptionsByTag(sdkDB, tag)

        assertEquals(list, result)
        coVerify { dao.allSubscriptionsByTag(tag) }
        verify { subscribeSnapshotDeferred.complete(Unit) }
    }
}
