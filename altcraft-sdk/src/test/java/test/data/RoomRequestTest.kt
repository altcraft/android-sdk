package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.room.*
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

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
 *  - test_11: existSubscriptions returns true/false as DAO reports.
 *  - test_12: existPushEvents returns true/false as DAO reports.
 *
 * Negative scenarios:
 *  - test_13: entityInsert with unsupported type → emits Events.error.
 *  - test_14: entityDelete with unsupported type → emits Events.error.
 *  - test_15: existSubscriptions on DAO exception → returns false + emits Events.error.
 *  - test_16: existPushEvents on DAO exception → returns false + emits Events.error.
 *
 * Notes:
 *  - Pure JVM unit tests (no Android runtime).
 *  - SDKdb.getDb(context) and DAO are mocked.
 *  - Events.event/error and SubFunction.logger are stubbed.
 *  - Suspend DAO calls are verified with coVerify.
 */

private const val MSG_RET_VAL_TRUE = "Return value must be true"
private const val MSG_RET_VAL_FALSE = "Return value must be false"

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

    // Helpers
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

    // ---------- entityInsert ----------

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

    /** test_13: entityInsert with unsupported type → emits Events.error */
    @Test
    fun entityInsert_unsupported_emitsError() = runBlocking {
        RoomRequest.entityInsert(ctx, "unsupported")
        verify { Events.error(eq("entityInsert"), any(), any()) }
    }

    // ---------- entityDelete ----------

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

    /** test_14: entityDelete with unsupported type → emits Events.error */
    @Test
    fun entityDelete_unsupported_emitsError() = runBlocking {
        RoomRequest.entityDelete(sdkDB, 12345)
        verify { Events.error(eq("entityDelete"), any(), any()) }
    }

    // ---------- isRetryLimit: SubscribeEntity ----------

    /** test_5: isRetryLimit SubscribeEntity over limit → emits event & deletes */
    @Test
    fun isRetryLimit_subscribe_overLimit_deletes_and_emitsEvent() = runBlocking {
        val e = sub(retry = 5, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertTrue(MSG_RET_VAL_TRUE, result)
        coVerify { dao.deleteSubscribeByUid(e.uid) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 420 },
                any()
            )
        }
    }

    /** test_6: isRetryLimit SubscribeEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_subscribe_notOver_incrementsRetry() = runBlocking {
        val e = sub(retry = 2, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertFalse(MSG_RET_VAL_FALSE, result)
        coVerify { dao.increaseSubscribeRetryCount(e.uid, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deleteSubscribeByUid(any()) }
    }

    // ---------- isRetryLimit: PushEventEntity ----------

    /** test_7: isRetryLimit PushEventEntity over limit → emits event & deletes (with uid) */
    @Test
    fun isRetryLimit_push_overLimit_deletes_and_emitsEvent_withUid() = runBlocking {
        val e = push(retry = 10, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertTrue(MSG_RET_VAL_TRUE, result)
        coVerify { dao.deletePushEventByUid(e.uid) }
        verify {
            Events.event(
                eq("isRetryLimit"),
                match<Pair<Int, String>> { it.first == 421 },
                match { it["uid"] == e.uid }
            )
        }
    }

    /** test_8: isRetryLimit PushEventEntity not over → increases retryCount */
    @Test
    fun isRetryLimit_push_notOver_incrementsRetry() = runBlocking {
        val e = push(retry = 0, max = 3)
        val result = RoomRequest.isRetryLimit(sdkDB, e)
        assertFalse(MSG_RET_VAL_FALSE, result)
        coVerify { dao.increasePushEventRetryCount(e.uid, e.retryCount + 1) }
        coVerify(exactly = 0) { dao.deletePushEventByUid(any()) }
    }

    // ---------- clearOldPushEventsFromRoom ----------

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
        verify { SubFunction.logger(match { it.contains("deleted", ignoreCase = true) == true }) }
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

    // ---------- existSubscriptions / existPushEvents ----------

    /** test_11: existSubscriptions returns true/false as DAO reports */
    @Test
    fun existSubscriptions_true_false_ok() = runBlocking {
        coEvery { dao.subscriptionsExistsByTag("A") } returns true
        coEvery { dao.subscriptionsExistsByTag("B") } returns false

        assertTrue(MSG_RET_VAL_TRUE, RoomRequest.existSubscriptions(ctx, "A"))
        assertFalse(MSG_RET_VAL_FALSE, RoomRequest.existSubscriptions(ctx, "B"))
    }

    /** test_15: existSubscriptions on DAO exception → returns false + emits Events.error */
    @Test
    fun existSubscriptions_onError_returnsFalse_andEmitsError() = runBlocking {
        coEvery { dao.subscriptionsExistsByTag(any()) } throws IllegalStateException("boom")
        val ok = RoomRequest.existSubscriptions(ctx, "X")
        assertFalse(MSG_RET_VAL_FALSE, ok)
        verify { Events.error(eq("existSubscription"), any(), any()) }
    }

    /** test_12: existPushEvents returns true/false as DAO reports */
    @Test
    fun existPushEvents_true_false_ok() = runBlocking {
        coEvery { dao.pushEventsExists() } returnsMany listOf(true, false)

        assertTrue(MSG_RET_VAL_TRUE, RoomRequest.existPushEvents(ctx))
        assertFalse(MSG_RET_VAL_FALSE, RoomRequest.existPushEvents(ctx))
    }

    /** test_16: existPushEvents on DAO exception → returns false + emits Events.error */
    @Test
    fun existPushEvents_onError_returnsFalse_andEmitsError() = runBlocking {
        coEvery { dao.pushEventsExists() } throws RuntimeException("err")
        val ok = RoomRequest.existPushEvents(ctx)
        assertFalse(MSG_RET_VAL_FALSE, ok)
        verify { Events.error(eq("existPushEvents"), any(), any()) }
    }
}


