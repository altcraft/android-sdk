@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
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
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.RETRY_TIME_C_WORK
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.Request
import com.altcraft.sdk.workers.coroutine.Worker
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * RequestInstrumentedTest
 *
 * test_1: pushEventRequest() builds request with correct shape and runs -> SUCCEEDED
 * test_2: subscribeRequest() builds correctly and on run ->
 * SUCCEEDED; closeService(..., PUSH_SUBSCRIBE_SERVICE, true)
 * test_3: updateRequest() builds correctly and on run ->
 * SUCCEEDED; closeService(..., TOKEN_UPDATE_SERVICE, true)
 * test_4: mobileEventRequest() builds request with correct shape and runs -> SUCCEEDED
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

        mockkObject(SubFunction, ServiceManager, PushEvent, PushSubscribe, TokenUpdate, MobileEvent)

        every { SubFunction.isAppInForegrounded() } returns true
        every { ServiceManager.checkServiceClosed(any(), any(), any()) } just Runs
        every { ServiceManager.closeService(any(), any(), any()) } just Runs

        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        coEvery { MobileEvent.isRetry(any()) } returns false

        Worker.retrySubscribe = 0
        Worker.retryUpdate = 0
    }

    @After
    fun tearDown() {
        unmockkObject(SubFunction, ServiceManager, PushEvent, PushSubscribe, TokenUpdate, MobileEvent)
        unmockkAll()
    }

    /** test_1: pushEventRequest builds with expected constraints/backoff/class and runs to SUCCEEDED */
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

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val info = workManager.getWorkInfoById(req.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)

        coVerify(exactly = 1) { PushEvent.isRetry(any()) }
    }

    /** test_2: subscribeRequest builds and, when run, closes PUSH_SUBSCRIBE_SERVICE and succeeds */
    @Test
    fun test_2_pushSubscribeRequest_buildsAndClosesService() {
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

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val info = workManager.getWorkInfoById(req.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)

        val ctx = slot<Context>()
        verify(atLeast = 1) { ServiceManager.closeService(capture(ctx), PUSH_SUBSCRIBE_SERVICE, true) }
        assertEquals("Package must match", context.packageName, ctx.captured.packageName)

        coVerify(exactly = 1) { PushSubscribe.isRetry(any()) }
    }

    /** test_3: updateRequest builds and, when run, closes TOKEN_UPDATE_SERVICE and succeeds */
    @Test
    fun test_3_tokenUpdateRequest_buildsAndClosesService() {
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

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val info = workManager.getWorkInfoById(req.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)

        val ctx = slot<Context>()
        verify(atLeast = 1) { ServiceManager.closeService(capture(ctx), TOKEN_UPDATE_SERVICE, true) }
        assertEquals("Package must match", context.packageName, ctx.captured.packageName)

        coVerify(exactly = 1) { TokenUpdate.isRetry(any(), any()) }
    }

    /** test_4: mobileEventRequest builds with expected constraints/backoff/class and runs to SUCCEEDED */
    @Test
    fun test_4_mobileEventRequest_buildsAndRunsSucceeded() {
        val req = Request.mobileEventRequest()

        assertTrue(req.tags.contains(MOBILE_EVENT_C_WORK_TAG))
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

        workManager.enqueue(req).result.get()
        testDriver!!.setAllConstraintsMet(req.id)

        val info = workManager.getWorkInfoById(req.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)

        coVerify(exactly = 1) { MobileEvent.isRetry(any()) }
    }
}