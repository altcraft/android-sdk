@file:Suppress("SpellCheckingInspection")

package test.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.extension.ExceptionExtension
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.uidIsNull
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import com.altcraft.sdk.workers.coroutine.Request as WorkerRequest
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
 *  - test_2: sendPushEvent offline + retry result → inserts entity and starts worker
 *  - test_3: sendPushEvent online + retry result → inserts entity and starts worker
 *  - test_4: sendPushEvent success → no insert and no worker
 *  - test_5: isRetry with success → deletes entity and returns false
 *  - test_6: isRetry with retry result → marks retry limit, no delete, returns true
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
    private lateinit var env: Environment
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
        entity = PushEventEntity(uid = UID_1, type = TYPE_OPEN)

        db = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        env = mockk(relaxed = true)

        every { db.request() } returns dao
        every { env.room } returns db

        mockkObject(Environment)
        every { Environment.create(ctx) } returns env

        mockkObject(RoomRequest)
        coEvery { RoomRequest.entityInsert(any(), any()) } coAnswers { Unit }
        coEvery { RoomRequest.entityDelete(any(), any()) } coAnswers { Unit }
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        mockkObject(Request)
        coEvery { Request.request(any(), any<PushEventEntity>()) } returns DataClasses.Event("ok")

        mockkObject(LaunchFunctions)
        coEvery { LaunchFunctions.startPushEventCoroutineWorker(any()) } coAnswers { Unit }

        mockkObject(WorkerRequest)
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        mockkObject(Events)
        every { Events.error(any(), any(), any()) } answers {
            DataClasses.Error(
                function = firstArg(),
                eventCode = null,
                eventMessage = secondArg<Any?>()?.toString(),
                eventValue = thirdArg()
            )
        }
        every { Events.error(any(), any()) } answers {
            DataClasses.Error(
                function = firstArg(),
                eventCode = null,
                eventMessage = secondArg<Any?>()?.toString(),
                eventValue = null
            )
        }
        every { Events.retry(any(), any(), any()) } answers {
            DataClasses.RetryError(
                function = firstArg(),
                eventCode = null,
                eventMessage = secondArg<Any?>()?.toString(),
                eventValue = thirdArg()
            )
        }
        every { Events.retry(any(), any()) } answers {
            DataClasses.RetryError(
                function = firstArg(),
                eventCode = null,
                eventMessage = secondArg<Any?>()?.toString(),
                eventValue = null
            )
        }

        mockkObject(SubFunction)

        mockkObject(ExceptionExtension)
        every { ExceptionExtension.exception(uidIsNull) } throws RuntimeException("uidIsNull")
    }

    @After
    fun tearDown() = unmockkAll()

    private fun mockOnline(isOnline: Boolean) {
        every { SubFunction.isOnline(ctx) } returns isOnline
    }

    /** - test_1: sendPushEvent with null UID → logs error, no DB insert, no worker. */
    @Test
    fun sendPushEvent_nullUid_triggersError_noInsert() = runTest {
        mockOnline(true)

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, null)

        verify(exactly = 1) { Events.error(FUNC_SEND, any<Exception>()) }
        coVerify(exactly = 0) { RoomRequest.entityInsert(any(), any()) }
        coVerify(exactly = 0) { LaunchFunctions.startPushEventCoroutineWorker(any()) }
        coVerify(exactly = 0) { Request.request(any(), any<PushEventEntity>()) }
    }

    /** - test_2: sendPushEvent offline + retry result → inserts entity and starts worker. */
    @Test
    fun sendPushEvent_offline_enqueuesAndStartsWorker() = runTest {
        mockOnline(false)

        coEvery { Request.request(ctx, any<PushEventEntity>()) } returns DataClasses.RetryError("eventRequest")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        verify(exactly = 1) { Events.error(FUNC_SEND, noInternetConnect) }
        coVerify(exactly = 1) {
            RoomRequest.entityInsert(
                ctx,
                match {
                    val e = it as? PushEventEntity ?: return@match false
                    e.uid == UID_1 && e.type == TYPE_OPEN && e.requestID.isNotBlank()
                }
            )
        }
        coVerify(exactly = 1) { LaunchFunctions.startPushEventCoroutineWorker(ctx) }
    }

    /** - test_3: sendPushEvent online + retry result → inserts entity and starts worker. */
    @Test
    fun sendPushEvent_retry_insertsAndStartsWorker() = runTest {
        mockOnline(true)

        coEvery { Request.request(ctx, any<PushEventEntity>()) } returns DataClasses.RetryError("eventRequest")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        coVerify(exactly = 1) {
            RoomRequest.entityInsert(
                ctx,
                match {
                    val e = it as? PushEventEntity ?: return@match false
                    e.uid == UID_1 && e.type == TYPE_OPEN && e.requestID.isNotBlank()
                }
            )
        }
        coVerify(exactly = 1) { LaunchFunctions.startPushEventCoroutineWorker(ctx) }
        verify(exactly = 0) { Events.error(FUNC_SEND, noInternetConnect) }
        verify(exactly = 0) { Events.error(FUNC_SEND, any<Exception>()) }
    }

    /** - test_4: sendPushEvent success → no DB insert and no worker start. */
    @Test
    fun sendPushEvent_success_doesNotInsertOrStartWorker() = runTest {
        mockOnline(true)

        coEvery { Request.request(ctx, any<PushEventEntity>()) } returns DataClasses.Event("ok")

        PushEvent.sendPushEvent(ctx, TYPE_OPEN, UID_1)

        coVerify(exactly = 0) { RoomRequest.entityInsert(any(), any()) }
        coVerify(exactly = 0) { LaunchFunctions.startPushEventCoroutineWorker(any()) }
        verify(exactly = 0) { Events.error(FUNC_SEND, noInternetConnect) }
        verify(exactly = 0) { Events.error(FUNC_SEND, any<Exception>()) }
    }

    /** - test_5: isRetry with success → deletes entity and returns false. */
    @Test
    fun isRetry_success_deletesAndReturnsFalse() = runTest {
        coEvery { dao.getAllPushEvents() } returns listOf(entity) andThen emptyList()
        coEvery { Request.request(ctx, entity) } returns DataClasses.Event("ok")

        val result = PushEvent.isRetry(ctx)

        assertFalse(result)
        coVerify(exactly = 1) { RoomRequest.entityDelete(db, entity) }
        coVerify(exactly = 0) { RoomRequest.isRetryLimit(db, entity) }
        coVerify(exactly = 0) { WorkerRequest.hasNewRequest(any(), any(), any()) }
    }

    /** - test_6: isRetry with retry result → marks retry limit, no delete, returns true. */
    @Test
    fun isRetry_withRetry_marksRetryLimitAndReturnsTrue() = runTest {
        coEvery { dao.getAllPushEvents() } returnsMany listOf(listOf(entity), listOf(entity))
        coEvery { Request.request(ctx, entity) } returns DataClasses.RetryError("eventRequest")
        coEvery { WorkerRequest.hasNewRequest(ctx, any(), any()) } returns false

        val result = PushEvent.isRetry(ctx)

        assertTrue(result)
        coVerify(exactly = 1) { RoomRequest.isRetryLimit(db, entity) }
        coVerify(exactly = 0) { RoomRequest.entityDelete(db, entity) }
    }

    /** - test_7: sendAllPushEvents parallel success → deletes both entities. */
    @Test
    fun sendAllPushEvents_parallelSuccessDeletesBoth() = runTest {
        val e1 = PushEventEntity(uid = "u1", type = TYPE_OPEN)
        val e2 = PushEventEntity(uid = "u2", type = "delivery")

        coEvery { Request.request(ctx, e1) } returns DataClasses.Event("ok1")
        coEvery { Request.request(ctx, e2) } returns DataClasses.Event("ok2")

        PushEvent.sendAllPushEvents(ctx, db, listOf(e1, e2))

        coVerify(exactly = 1) { RoomRequest.entityDelete(db, e1) }
        coVerify(exactly = 1) { RoomRequest.entityDelete(db, e2) }
        coVerify(exactly = 0) { RoomRequest.isRetryLimit(any(), any()) }
    }
}