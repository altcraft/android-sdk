@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.data.Constants.MOB_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PID
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.RETRY_TIME_C_WORK
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.coroutine.Request
import com.altcraft.sdk.workers.coroutine.Worker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * RequestInstrumentedTest
 *
 * test_1: pushEventRequest() builds request with correct shape (tag, constraints, backoff, worker, pid)
 *         and runs -> SUCCEEDED.
 * test_2: subscribeRequest() builds correctly and runs -> SUCCEEDED.
 * test_3: updateRequest() builds correctly and runs -> SUCCEEDED.
 * test_4: mobileEventRequest() builds correctly and runs -> SUCCEEDED.
 * test_5: hasNewRequest() returns false when there is a single active work.
 * test_6: hasNewRequest() returns true for older work and false for the newest one.
 */
@RunWith(AndroidJUnit4::class)
class RequestInstrumentedTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private var testDriver: TestDriver? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver = WorkManagerTestInitHelper.getTestDriver(context)

        mockkObject(PushEvent, PushSubscribe, TokenUpdate, MobileEvent)

        coEvery { PushEvent.isRetry(any(), any()) } returns false
        coEvery { PushSubscribe.isRetry(any(), any()) } returns false
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        coEvery { MobileEvent.isRetry(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkObject(PushEvent, PushSubscribe, TokenUpdate, MobileEvent)
        unmockkAll()
    }

    private fun awaitWorkSucceeded(id: UUID, timeoutMs: Long = 2000L): WorkInfo.State? {
        val start = System.currentTimeMillis()
        var state: WorkInfo.State? = null
        while (System.currentTimeMillis() - start < timeoutMs) {
            state = workManager.getWorkInfoById(id).get()?.state
            if (state != null && state.isFinished) break
            Thread.sleep(50)
        }
        return state
    }

    /** test_1: pushEventRequest builds with expected constraints/backoff/class/pid and runs to SUCCEEDED. */
    @Test
    fun test_1_pushEventRequest_buildsAndRunsSucceeded() {
        val req = Request.pushEventRequest()

        assertTrue(req.tags.contains(PUSH_EVENT_C_WORK_TAG))
        assertEquals(NetworkType.CONNECTED, req.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, req.workSpec.backoffPolicy)

        val expectedBackoffMs = maxOf(
            TimeUnit.SECONDS.toMillis(RETRY_TIME_C_WORK),
            WorkRequest.MIN_BACKOFF_MILLIS
        )
        assertEquals(expectedBackoffMs, req.workSpec.backoffDelayDuration)

        assertEquals(
            Worker.PushEventCoroutineWorker::class.java.name,
            req.workSpec.workerClassName
        )

        val storedPid = req.workSpec.input.getInt(PID, -1)
        assertEquals(Process.myPid(), storedPid)

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val state = awaitWorkSucceeded(req.id)
        assertEquals(WorkInfo.State.SUCCEEDED, state)

        coVerify(exactly = 1) { PushEvent.isRetry(any(), req.id) }
    }

    /** test_2: subscribeRequest builds correctly and runs to SUCCEEDED. */
    @Test
    fun test_2_pushSubscribeRequest_buildsAndRunsSucceeded() {
        val req = Request.subscribeRequest()

        assertTrue(req.tags.contains(SUBSCRIBE_C_WORK_TAG))
        assertEquals(NetworkType.CONNECTED, req.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, req.workSpec.backoffPolicy)

        val expectedBackoffMs = maxOf(
            TimeUnit.SECONDS.toMillis(RETRY_TIME_C_WORK),
            WorkRequest.MIN_BACKOFF_MILLIS
        )
        assertEquals(expectedBackoffMs, req.workSpec.backoffDelayDuration)

        assertEquals(
            Worker.SubscribeCoroutineWorker::class.java.name,
            req.workSpec.workerClassName
        )

        val storedPid = req.workSpec.input.getInt(PID, -1)
        assertEquals(Process.myPid(), storedPid)

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val state = awaitWorkSucceeded(req.id)
        assertEquals(WorkInfo.State.SUCCEEDED, state)

        coVerify(exactly = 1) { PushSubscribe.isRetry(any(), req.id) }
    }

    /** test_3: updateRequest builds correctly and runs to SUCCEEDED. */
    @Test
    fun test_3_tokenUpdateRequest_buildsAndRunsSucceeded() {
        val req = Request.updateRequest()

        assertTrue(req.tags.contains(UPDATE_C_WORK_TAG))
        assertEquals(NetworkType.CONNECTED, req.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, req.workSpec.backoffPolicy)

        val expectedBackoffMs = maxOf(
            TimeUnit.SECONDS.toMillis(RETRY_TIME_C_WORK),
            WorkRequest.MIN_BACKOFF_MILLIS
        )
        assertEquals(expectedBackoffMs, req.workSpec.backoffDelayDuration)

        assertEquals(
            Worker.UpdateCoroutineWorker::class.java.name,
            req.workSpec.workerClassName
        )

        val storedPid = req.workSpec.input.getInt(PID, -1)
        assertEquals(Process.myPid(), storedPid)

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val state = awaitWorkSucceeded(req.id)
        assertEquals(WorkInfo.State.SUCCEEDED, state)

        coVerify(exactly = 1) { TokenUpdate.isRetry(any(), req.id) }
    }

    /** test_4: mobileEventRequest builds with expected constraints/backoff/class/pid and runs to SUCCEEDED. */
    @Test
    fun test_4_mobileEventRequest_buildsAndRunsSucceeded() {
        val req = Request.mobileEventRequest()

        assertTrue(req.tags.contains(MOB_EVENT_C_WORK_TAG))
        assertEquals(NetworkType.CONNECTED, req.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, req.workSpec.backoffPolicy)

        val expectedBackoffMs = maxOf(
            TimeUnit.SECONDS.toMillis(RETRY_TIME_C_WORK),
            WorkRequest.MIN_BACKOFF_MILLIS
        )
        assertEquals(expectedBackoffMs, req.workSpec.backoffDelayDuration)

        assertEquals(
            Worker.MobileEventCoroutineWorker::class.java.name,
            req.workSpec.workerClassName
        )

        val storedPid = req.workSpec.input.getInt(PID, -1)
        assertEquals(Process.myPid(), storedPid)

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val state = awaitWorkSucceeded(req.id)
        assertEquals(WorkInfo.State.SUCCEEDED, state)

        coVerify(exactly = 1) { MobileEvent.isRetry(any(), req.id) }
    }

    /** test_5: hasNewRequest returns false when there is a single active work for the tag. */
    @Test
    fun test_5_hasNewRequest_singleActive_returnsFalse() = runBlocking {
        val req = Request.pushEventRequest()
        workManager.enqueue(req).result.get()

        val hasNew = Request.hasNewRequest(context, PUSH_EVENT_C_WORK_TAG, req.id)
        assertFalse(hasNew)
    }

    /** test_6: hasNewRequest returns true for older work and false for the newest one. */
    @Test
    fun test_6_hasNewRequest_detectsNewerRequest() = runBlocking {
        val first = Request.pushEventRequest()
        workManager.enqueue(first).result.get()

        Thread.sleep(5)

        val second = Request.pushEventRequest()
        workManager.enqueue(second).result.get()

        val hasNewForFirst = Request.hasNewRequest(context, PUSH_EVENT_C_WORK_TAG, first.id)
        val hasNewForSecond = Request.hasNewRequest(context, PUSH_EVENT_C_WORK_TAG, second.id)

        assertTrue(hasNewForFirst)
        assertFalse(hasNewForSecond)
    }
}