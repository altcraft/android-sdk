@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.Worker

/**
 * WorkersInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: PushEventCoroutineWorker → PushEvent.isRetry = true ⇒ Result.retry.
 *  - test_2: PushEventCoroutineWorker → PushEvent.isRetry = false ⇒ Result.success.
 *  - test_3: SubscribeCoroutineWorker → isRetry = true ⇒
 *  ServiceManager.checkServiceClosed(..., PUSH_SUBSCRIBE_SERVICE, ++retrySubscribe); Result.retry.
 *  - test_4: SubscribeCoroutineWorker → isRetry = false ⇒
 *  ServiceManager.closeService(..., PUSH_SUBSCRIBE_SERVICE, true); Result.success.
 *  - test_5: UpdateCoroutineWorker → isRetry = true ⇒
 *  ServiceManager.checkServiceClosed(..., TOKEN_UPDATE_SERVICE, ++retrySubscribe); Result.retry.
 *  - test_6: UpdateCoroutineWorker → isRetry = false ⇒
 *  ServiceManager.closeService(..., TOKEN_UPDATE_SERVICE, true); Result.success.
 *  - test_7: MobileEventCoroutineWorker → MobileEvent.isRetry = true ⇒ Result.retry.
 *  - test_8: MobileEventCoroutineWorker → MobileEvent.isRetry = false ⇒ Result.success.
 *
 * Notes:
 *  - isAppInForegrounded() is mocked to true to skip delay in awaitInBackground().
 *  - Worker.retrySubscribe / Worker.retryUpdate are reset between tests.
 */
@RunWith(AndroidJUnit4::class)
class WorkersInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(SubFunction)
        mockkObject(PushEvent)
        mockkObject(PushSubscribe)
        mockkObject(TokenUpdate)
        mockkObject(MobileEvent)
        mockkObject(ServiceManager)

        every { SubFunction.isAppInForegrounded() } returns true

        Worker.retrySubscribe = 0
        Worker.retryUpdate = 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun buildPushEventWorker(): Worker.PushEventCoroutineWorker =
        TestListenableWorkerBuilder<Worker.PushEventCoroutineWorker>(context).build()

    private fun buildMobileEventWorker(): Worker.MobileEventCoroutineWorker =
        TestListenableWorkerBuilder<Worker.MobileEventCoroutineWorker>(context).build()

    private fun buildSubscribeWorker(): Worker.SubscribeCoroutineWorker =
        TestListenableWorkerBuilder<Worker.SubscribeCoroutineWorker>(context).build()

    private fun buildUpdateWorker(): Worker.UpdateCoroutineWorker =
        TestListenableWorkerBuilder<Worker.UpdateCoroutineWorker>(context).build()

    /** - test_1: PushEventCoroutineWorker → PushEvent.isRetry = true ⇒ Result.retry. */
    @Test
    fun pushEventWorker_returns_retry_when_PushEvent_isRetry_true() = runTest {
        coEvery { PushEvent.isRetry(any()) } returns true

        val worker = buildPushEventWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { PushEvent.isRetry(context) }
    }

    /** - test_2: PushEventCoroutineWorker → PushEvent.isRetry = false ⇒ Result.success. */
    @Test
    fun pushEventWorker_returns_success_when_PushEvent_isRetry_false() = runTest {
        coEvery { PushEvent.isRetry(any()) } returns false

        val worker = buildPushEventWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { PushEvent.isRetry(context) }
    }

    /** - test_3: SubscribeCoroutineWorker → isRetry = true ⇒
     *  checkServiceClosed(..., PUSH_SUBSCRIBE_SERVICE, 1) and Result.retry.
     *  */
    @Test
    fun subscribeWorker_retry_path_calls_checkServiceClosed_and_returns_retry() = runTest {
        Worker.retrySubscribe = 0
        coEvery { PushSubscribe.isRetry(any()) } returns true
        every { ServiceManager.checkServiceClosed(any(), any(), any()) } returns Unit

        val worker = buildSubscribeWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        verify(exactly = 1) { ServiceManager.checkServiceClosed(context, PUSH_SUBSCRIBE_SERVICE, 1) }
        verify(exactly = 0) { ServiceManager.closeService(any(), any(), any()) }
        coVerify(exactly = 1) { PushSubscribe.isRetry(context) }
    }

    /** - test_4: SubscribeCoroutineWorker → isRetry = false ⇒
     * closeService(..., PUSH_SUBSCRIBE_SERVICE, true) and Result.success.
     * */
    @Test
    fun subscribeWorker_success_path_calls_closeService_and_returns_success() = runTest {
        coEvery { PushSubscribe.isRetry(any()) } returns false
        every { ServiceManager.closeService(any(), any(), any()) } returns Unit

        val worker = buildSubscribeWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        verify(exactly = 1) { ServiceManager.closeService(context, PUSH_SUBSCRIBE_SERVICE, true) }
        verify(exactly = 0) { ServiceManager.checkServiceClosed(any(), any(), any()) }
        coVerify(exactly = 1) { PushSubscribe.isRetry(context) }
    }

    /** - test_5: UpdateCoroutineWorker → isRetry = true ⇒
     * checkServiceClosed(..., TOKEN_UPDATE_SERVICE, 1) and Result.retry.
     * */
    @Test
    fun updateWorker_retry_path_calls_checkServiceClosed_and_returns_retry() = runTest {
        Worker.retrySubscribe = 0
        coEvery { TokenUpdate.isRetry(any(), any()) } returns true
        every { ServiceManager.checkServiceClosed(any(), any(), any()) } returns Unit

        val worker = buildUpdateWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        verify(exactly = 1) { ServiceManager.checkServiceClosed(context, TOKEN_UPDATE_SERVICE, 1) }
        verify(exactly = 0) { ServiceManager.closeService(any(), any(), any()) }
        coVerify(exactly = 1) { TokenUpdate.isRetry(context, any()) }
    }

    /** - test_6: UpdateCoroutineWorker → isRetry = false ⇒
     * closeService(..., TOKEN_UPDATE_SERVICE, true) and Result.success.
     * */
    @Test
    fun updateWorker_success_path_calls_closeService_and_returns_success() = runTest {
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        every { ServiceManager.closeService(any(), any(), any()) } returns Unit

        val worker = buildUpdateWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        verify(exactly = 1) { ServiceManager.closeService(context, TOKEN_UPDATE_SERVICE, true) }
        verify(exactly = 0) { ServiceManager.checkServiceClosed(any(), any(), any()) }
        coVerify(exactly = 1) { TokenUpdate.isRetry(context, any()) }
    }

    /** - test_7: MobileEventCoroutineWorker → MobileEvent.isRetry = true ⇒ Result.retry. */
    @Test
    fun mobileEventWorker_returns_retry_when_MobileEvent_isRetry_true() = runTest {
        coEvery { MobileEvent.isRetry(any()) } returns true

        val worker = buildMobileEventWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { MobileEvent.isRetry(context) }
    }

    /** - test_8: MobileEventCoroutineWorker → MobileEvent.isRetry = false ⇒ Result.success. */
    @Test
    fun mobileEventWorker_returns_success_when_MobileEvent_isRetry_false() = runTest {
        coEvery { MobileEvent.isRetry(any()) } returns false

        val worker = buildMobileEventWorker()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { MobileEvent.isRetry(context) }
    }
}