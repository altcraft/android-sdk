@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger
import com.altcraft.sdk.additional.StringBuilder
import com.altcraft.sdk.additional.StringBuilder.deletedMobileEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedPushEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedSubscriptionsMsg
import com.altcraft.sdk.core.InitialOperations
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.Constants.TYPE
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.RequestEntity
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.sdk_events.Events
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        mockkObject(InitialOperations)
        mobileEventsSnapshotDeferred = mockk(relaxed = true)
        subscribeSnapshotDeferred = mockk(relaxed = true)
        every { InitialOperations.mobileEventsDbSnapshotTaken } returns mobileEventsSnapshotDeferred
        every { InitialOperations.pushSubscribeDbSnapshotTaken } returns subscribeSnapshotDeferred
    }

    @After
    fun tearDown() = unmockkAll()

    private fun sub(
        requestId: String = "sub-uid",
        retry: Int = 1,
        max: Int = 3
    ) = SubscribeEntity(
        requestID = requestId,
        userTag = "tag",
        status = SUBSCRIBED,
        sync = null,
        profileFields = JsonNull,
        customFields = JsonNull,
        cats = null,
        replace = null,
        skipTriggers = null,
        time = System.currentTimeMillis(),
        retryCount = retry,
        maxRetryCount = max
    )

    private fun push(
        requestId: String = "push-req",
        uid: String = "push-uid",
        retry: Int = 1,
        max: Int = 3
    ) = PushEventEntity(
        requestID = requestId,
        uid = uid,
        type = "opened",
        time = System.currentTimeMillis(),
        retryCount = retry,
        maxRetryCount = max
    )

    private fun mobile(
        requestId: String = "mobile-req",
        name: String = "evt",
        retry: Int = 1,
        max: Int = 3
    ) = MobileEventEntity(
        requestID = requestId,
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
        val e: RequestEntity = sub()
        coEvery { dao.insertSubscribe(any()) } throws RuntimeException("insert failed")

        RoomRequest.entityInsert(ctx, e)

        verify { Events.error(eq("entityInsert"), any<Throwable>()) }
    }

    /** test_3: entityDelete deletes SubscribeEntity via DAO */
    @Test
    fun entityDelete_subscribe_callsDao() = runBlocking {
        val e = sub(requestId = "X")
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deleteSubscribeById("X") }
    }

    /** test_4: entityDelete deletes PushEventEntity via DAO */
    @Test
    fun entityDelete_push_callsDao() = runBlocking {
        val e = push(requestId = "Y", uid = "uid-Y")
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deletePushEventById("Y") }
    }

    /** test_12: entityDelete deletes MobileEventEntity via DAO */
    @Test
    fun entityDelete_mobile_callsDao() = runBlocking {
        val e = mobile(requestId = "req-777")
        RoomRequest.entityDelete(sdkDB, e)
        coVerify { dao.deleteMobileEventById("req-777") }
    }

    /** test_18: entityDelete with unsupported type → emits Events.error */
    @Test
    fun entityDelete_unsupported_emitsError() = runBlocking {
        val e = sub(requestId = "err-id")
        coEvery { dao.deleteSubscribeById(any()) } throws RuntimeException("delete failed")

        RoomRequest.entityDelete(sdkDB, e)

        verify { Events.error(eq("entityDelete"), any<Throwable>()) }
    }

    /** test_5: isRetryLimit SubscribeEntity over limit → emits event & deletes */
    @Test
    fun isRetryLimit_subscribe_overLimit_deletes_and_emitsEvent() = runBlocking {
        val e = sub(requestId = "req-sub", retry = 5, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertTrue("Return value must be true", result)
        coVerify { dao.deleteSubscribeById(e.requestID) }
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
        val e = sub(requestId = "req-sub-2", retry = 2, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertFalse("Return value must be false", result)
        coVerify { dao.increaseSubscribeRetryCount(e.requestID, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deleteSubscribeById(any()) }
    }

    /** test_7: isRetryLimit PushEventEntity over limit → emits event & deletes (with uid) */
    @Test
    fun isRetryLimit_push_overLimit_deletes_and_emitsEvent_withUid() = runBlocking {
        val e = push(requestId = "req-push", uid = "uid-over", retry = 10, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertTrue("Return value must be true", result)
        coVerify { dao.deletePushEventById(e.requestID) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { (code, msg) ->
                    code == 484 && msg.contains(e.uid)
                },
                match<Map<String, Any?>> { value ->
                    value[UID] == e.uid && value[TYPE] == e.type
                }
            )
        }
    }

    /** test_8: isRetryLimit PushEventEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_push_notOver_incrementsRetry() = runBlocking {
        val e = push(requestId = "req-push-2", uid = "uid-not-over", retry = 0, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertFalse("Return value must be false", result)
        coVerify { dao.increasePushEventRetryCount(e.requestID, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deletePushEventById(any()) }
    }

    /** test_13: isRetryLimit MobileEventEntity over limit → emits event & deletes (with name) */
    @Test
    fun isRetryLimit_mobile_overLimit_deletes_and_emitsEvent_withName() = runBlocking {
        val e = mobile(requestId = "req-99", name = "purchase", retry = 9, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertTrue("Return value must be true", result)
        coVerify { dao.deleteMobileEventById("req-99") }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 487 },
                match<Map<String, Any?>> { it[NAME] == "purchase" }
            )
        }
    }

    /** test_14: isRetryLimit MobileEventEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_mobile_notOver_incrementsRetry() = runBlocking {
        val e = mobile(requestId = "req-5", name = "open", retry = 1, max = 3)

        val result = RoomRequest.isRetryLimit(sdkDB, e)

        assertFalse("Return value must be false", result)
        coVerify { dao.increaseMobileEventRetryCount("req-5", 2) }
        coVerify(exactly = 0) { dao.deleteMobileEventById(any()) }
    }

    /** test_9: clearOldPushEventsFromRoom when >500 → deletes oldest 100 and logs */
    @Test
    fun clearOldPushEventsFromRoom_overThreshold_deletesOldest100_and_logs() = runBlocking {
        coEvery { dao.getPushEventCount() } returnsMany listOf(501, 401)

        val oldest = (1..100).map {
            PushEventEntity(
                requestID = "req-p$it",
                uid = "u$it",
                type = "t"
            )
        }
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
                requestID = "req-m$it",
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

        val oldest = (1..100).map { sub(requestId = "s$it") }
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

    /** test_21: roomOverflowControl calls all cleanup functions */
    @Test
    fun roomOverflowControl_callsAllCleanupMethods() = runBlocking {
        coEvery { dao.getSubscribeCount() } returnsMany listOf(600, 450)
        val oldestSubs = (1..3).map { sub(requestId = "s$it") }
        coEvery { dao.getOldestSubscriptions(100) } returns oldestSubs
        coJustRun { dao.deleteSubscriptions(oldestSubs) }

        coEvery { dao.getMobileEventCount() } returnsMany listOf(700, 500)
        val oldestMobiles = (1..3).map { mobile(requestId = "req-m$it", name = "m$it") }
        coEvery { dao.getOldestMobileEvents(100) } returns oldestMobiles
        coJustRun { dao.deleteMobileEvents(oldestMobiles) }

        coEvery { dao.getPushEventCount() } returnsMany listOf(800, 480)
        val oldestPushes = (1..3).map {
            PushEventEntity(
                requestID = "req-p$it",
                uid = "p$it",
                type = "type"
            )
        }
        coEvery { dao.getOldestPushEvents(100) } returns oldestPushes
        coJustRun { dao.deletePushEvents(oldestPushes) }

        RoomRequest.roomOverflowControl(sdkDB)

        coVerify { dao.getSubscribeCount() }
        coVerify { dao.getOldestSubscriptions(100) }
        coVerify { dao.deleteSubscriptions(oldestSubs) }

        coVerify { dao.getMobileEventCount() }
        coVerify { dao.getOldestMobileEvents(100) }
        coVerify { dao.deleteMobileEvents(oldestMobiles) }

        coVerify { dao.getPushEventCount() }
        coVerify { dao.getOldestPushEvents(100) }
        coVerify { dao.deletePushEvents(oldestPushes) }
    }

    /** test_22: allMobileEventsByTag returns events and completes snapshot */
    @Test
    fun allMobileEventsByTag_returnsEvents_andCompletesSnapshot() = runBlocking {
        val tag = "user-tag"
        val list = listOf(
            mobile(requestId = "req-1", name = "evt1"),
            mobile(requestId = "req-2", name = "evt2")
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
            sub(requestId = "s1"),
            sub(requestId = "s2")
        )

        coEvery { dao.allSubscriptionsByTag(tag) } returns list

        val result = RoomRequest.allSubscriptionsByTag(sdkDB, tag)

        assertEquals(list, result)
        coVerify { dao.allSubscriptionsByTag(tag) }
        verify { subscribeSnapshotDeferred.complete(Unit) }
    }
}