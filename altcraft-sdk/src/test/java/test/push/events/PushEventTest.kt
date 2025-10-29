@file:Suppress("SpellCheckingInspection")

package test.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PushEventTest
 *
 * Positive scenarios:
 *  - test_1: sendPushEvent with null UID → logs error, no DB insert, no worker
 *  - test_2: sendPushEvent offline + RetryError → inserts entity and starts worker
 *  - test_3: sendPushEvent online + RetryError → inserts entity and starts worker
 *  - test_4: sendPushEvent success → no insert and no worker
 *  - test_5: isRetry with success → deletes entity and returns false
 *  - test_6: isRetry with RetryError → increases retry count, no delete, returns true
 *  - test_7: sendAllPushEvents parallel success → deletes both entities
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushEventTest {

    private companion object {
        const val FUNC_SEND = "sendPushEvent"
        const val UID_1 = "uid-1"
        const val TYPE_OPEN = "open"
    }

    private lateinit var ctx: Context
    private lateinit var db: SDKdb
    private lateinit var dao: DAO
    private lateinit var entity: PushEventEntity

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0

        ctx = mockk(relaxed = true)
        entity = PushEventEntity(UID_1, TYPE_OPEN)

        mockkObject(Request)
        mockkObject(LaunchFunctions)
        mockkObject(Events)
        mockkObject(SDKdb.Companion)

        db = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        every { SDKdb.getDb(ctx) } returns db
        every { db.request() } returns dao
    }

    @After
    fun tearDown() = unmockkAll()

    private fun mockOnline(isOnline: Boolean) {
        mockkObject(com.altcraft.sdk.additional.SubFunction)
        every { com.altcraft.sdk.additional.SubFunction.isOnline(ctx) } returns isOnline
    }

    /** - test_1: sendPushEvent with null UID → logs error, no DB insert, no worker. */
    @Test
    fun sendPushEvent_nullUid_triggersError_noInsert() = runTest {
        mockOnline(true)

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, null)

        verify { Events.error(FUNC_SEND, any<Exception>()) }
        coVerify(exactly = 0) { dao.insertPushEvent(any()) }
        verify(exactly = 0) { LaunchFunctions.startPushEventCoroutineWorker(any()) }
        coVerify(exactly = 0) { Request.pushEventRequest(any(), any()) }
    }

    /** - test_2: sendPushEvent offline + RetryError → inserts entity and starts worker. */
    @Test
    fun sendPushEvent_offline_enqueuesAndStartsWorker() = runTest {
        mockOnline(false)
        coEvery { Request.pushEventRequest(ctx, any()) } returns DataClasses.RetryError("eventRequest")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        verify { Events.error(FUNC_SEND, any()) }
        coVerify(exactly = 1) { dao.insertPushEvent(match { it.uid == UID_1 && it.type == TYPE_OPEN }) }
        verify(exactly = 1) { LaunchFunctions.startPushEventCoroutineWorker(ctx) }
    }

    /** - test_3: sendPushEvent online + RetryError → inserts entity and starts worker. */
    @Test
    fun sendPushEvent_retry_insertsAndStartsWorker() = runTest {
        mockOnline(true)
        coEvery { Request.pushEventRequest(ctx, any()) } returns DataClasses.RetryError("eventRequest")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        coVerify(exactly = 1) { dao.insertPushEvent(match { it.uid == UID_1 && it.type == TYPE_OPEN }) }
        verify(exactly = 1) { LaunchFunctions.startPushEventCoroutineWorker(ctx) }
        verify(exactly = 0) { Events.error(FUNC_SEND, any()) }
    }

    /** - test_4: sendPushEvent success → no DB insert and no worker start. */
    @Test
    fun sendPushEvent_success_doesNotInsertOrStartWorker() = runTest {
        mockOnline(true)
        coEvery { Request.pushEventRequest(ctx, any()) } returns DataClasses.Event("ok")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        coVerify(exactly = 0) { dao.insertPushEvent(any()) }
        verify(exactly = 0) { LaunchFunctions.startPushEventCoroutineWorker(ctx) }
    }

    /** - test_5: isRetry with success → deletes entity and returns false. */
    @Test
    fun isRetry_success_deletesAndReturnsFalse() = runTest {
        coEvery { dao.getAllPushEvents() } returns listOf(entity) andThen emptyList()
        coEvery { Request.pushEventRequest(ctx, entity) } returns DataClasses.Event("ok")

        val result = PushEvent.isRetry(ctx)

        assertFalse(result)
        coVerify { dao.deletePushEventByUid(UID_1) }
    }

    /** - test_6: isRetry with RetryError → increases retry count, no delete, returns true. */
    @Test
    fun isRetry_withRetry_increasesRetryAndReturnsTrue() = runTest {
        coEvery { dao.getAllPushEvents() } returnsMany listOf(listOf(entity), listOf(entity))
        coEvery { Request.pushEventRequest(ctx, entity) } returns DataClasses.RetryError("eventRequest")

        val result = PushEvent.isRetry(ctx)

        assertTrue(result)
        coVerify { dao.increasePushEventRetryCount(UID_1, any()) }
        coVerify(exactly = 0) { dao.deletePushEventByUid(any()) }
    }

    /** - test_7: sendAllPushEvents parallel success → deletes both entities. */
    @Test
    fun sendAllPushEvents_parallelSuccessDeletesBoth() = runTest {
        val e1 = PushEventEntity("u1", TYPE_OPEN)
        val e2 = PushEventEntity("u2", "delivery")

        coEvery { Request.pushEventRequest(ctx, e1) } returns DataClasses.Event("ok1")
        coEvery { Request.pushEventRequest(ctx, e2) } returns DataClasses.Event("ok2")

        PushEvent.sendAllPushEvents(ctx, db, listOf(e1, e2))

        coVerify { dao.deletePushEventByUid("u1") }
        coVerify { dao.deletePushEventByUid("u2") }
    }
}