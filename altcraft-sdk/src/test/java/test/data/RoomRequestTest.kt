@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.*
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
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
 *
 * Negative scenarios:
 *  - test_17: entityInsert with unsupported type → emits Events.error.
 *  - test_18: entityDelete with unsupported type → emits Events.error.
 */
class RoomRequestUnitTest {

    private lateinit var ctx: Context
    private lateinit var sdkDB: SDKdb
    private lateinit var dao: DAO

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
        every { Events.error(any(), any(), any()) } returns
                DataClasses.Error("fn", 400, "err", null)

        mockkObject(SubFunction)
        every { SubFunction.logger(any()) } returns 0
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
        verify { Events.error(eq("entityInsert"), any(), any()) }
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
        verify { Events.error(eq("entityDelete"), any(), any()) }
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
                match<Pair<Int, String>> { it.first == 484 }, // PUSH_EVENT_RETRY_LIMIT
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
                match<Pair<Int, String>> { it.first == 485 }, // MOBILE_EVENT_RETRY_LIMIT
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
        verify { SubFunction.logger(match { it.contains("Deleted 100 oldest push events") }) }
    }

    /** test_10: clearOldPushEventsFromRoom when <=500 → no action */
    @Test
    fun clearOldPushEventsFromRoom_underThreshold_noop() = runBlocking {
        coEvery { dao.getPushEventCount() } returns 120

        RoomRequest.clearOldPushEventsFromRoom(sdkDB)

        coVerify { dao.getPushEventCount() }
        coVerify(exactly = 0) { dao.getOldestPushEvents(any()) }
        coVerify(exactly = 0) { dao.deletePushEvents(any()) }
        verify(exactly = 0) { SubFunction.logger(any()) }
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
        verify { SubFunction.logger(match { it.contains("Deleted 100 oldest mobile events") }) }
    }

    /** test_16: clearOldMobileEventsFromRoom when <=500 → no action */
    @Test
    fun clearOldMobileEventsFromRoom_underThreshold_noop() = runBlocking {
        coEvery { dao.getMobileEventCount() } returns 42

        RoomRequest.clearOldMobileEventsFromRoom(sdkDB)

        coVerify { dao.getMobileEventCount() }
        coVerify(exactly = 0) { dao.getOldestMobileEvents(any()) }
        coVerify(exactly = 0) { dao.deleteMobileEvents(any()) }
        verify(exactly = 0) { SubFunction.logger(any()) }
    }
}
