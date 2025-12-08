@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.PID
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.Worker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WorkersInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: PushEventCoroutineWorker → pid same + PushEvent.isRetry = true ⇒ Result.retry.
 *  - test_2: PushEventCoroutineWorker → pid same + PushEvent.isRetry = false ⇒ Result.success.
 *  - test_3: SubscribeCoroutineWorker → pid same + isRetry = true ⇒ Result.retry, no stopService.
 *  - test_4: SubscribeCoroutineWorker → pid same + isRetry = false ⇒ Result.success, stopService(SUBSCRIBE_SERVICE).
 *  - test_5: UpdateCoroutineWorker → pid same + isRetry = true ⇒ Result.retry, no stopService.
 *  - test_6: UpdateCoroutineWorker → pid same + isRetry = false ⇒ Result.success, stopService(UPDATE_SERVICE).
 *  - test_7: MobileEventCoroutineWorker → pid same + MobileEvent.isRetry = true ⇒ Result.retry.
 *  - test_8: MobileEventCoroutineWorker → pid same + MobileEvent.isRetry = false ⇒ Result.success.
 *  - test_9: pidChanged branch (push) ⇒ Result.success, isRetry not called.
 *  - test_10: pidChanged branch (subscribe) ⇒ Result.success, stopService(SUBSCRIBE_SERVICE), isRetry not called.
 *  - test_11: pidChanged branch (update) ⇒ Result.success, stopService(UPDATE_SERVICE), isRetry not called.
 *  - test_12: pidChanged branch (mobile) ⇒ Result.success, isRetry not called.
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
    }

    @After
    fun tearDown() {
        unmockkObject(
            SubFunction,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent,
            ServiceManager
        )
        unmockkAll()
    }

    private fun pushEventWorker(samePid: Boolean = true): Worker.PushEventCoroutineWorker {
        val data = Data.Builder()
            .putInt(PID, if (samePid) Process.myPid() else -1)
            .build()
        return TestListenableWorkerBuilder<Worker.PushEventCoroutineWorker>(context)
            .setInputData(data)
            .build()
    }

    private fun mobileEventWorker(samePid: Boolean = true): Worker.MobileEventCoroutineWorker {
        val data = Data.Builder()
            .putInt(PID, if (samePid) Process.myPid() else -1)
            .build()
        return TestListenableWorkerBuilder<Worker.MobileEventCoroutineWorker>(context)
            .setInputData(data)
            .build()
    }

    private fun subscribeWorker(samePid: Boolean = true): Worker.SubscribeCoroutineWorker {
        val data = Data.Builder()
            .putInt(PID, if (samePid) Process.myPid() else -1)
            .build()
        return TestListenableWorkerBuilder<Worker.SubscribeCoroutineWorker>(context)
            .setInputData(data)
            .build()
    }

    private fun updateWorker(samePid: Boolean = true): Worker.UpdateCoroutineWorker {
        val data = Data.Builder()
            .putInt(PID, if (samePid) Process.myPid() else -1)
            .build()
        return TestListenableWorkerBuilder<Worker.UpdateCoroutineWorker>(context)
            .setInputData(data)
            .build()
    }

    /** - test_1: PushEventCoroutineWorker → same pid + PushEvent.isRetry = true ⇒ Result.retry. */
    @Test
    fun pushEventWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { PushEvent.isRetry(any(), any()) } returns true

        val worker = pushEventWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { PushEvent.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_2: PushEventCoroutineWorker → same pid + PushEvent.isRetry = false ⇒ Result.success. */
    @Test
    fun pushEventWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { PushEvent.isRetry(any(), any()) } returns false

        val worker = pushEventWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { PushEvent.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_3: SubscribeCoroutineWorker → same pid + isRetry = true ⇒ Result.retry, no stopService. */
    @Test
    fun subscribeWorker_retry_path_no_stopService_when_isRetry_true_and_pid_same() = runTest {
        coEvery { PushSubscribe.isRetry(any(), any()) } returns true
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = subscribeWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { PushSubscribe.isRetry(worker.applicationContext, worker.id) }
        verify(exactly = 0) { ServiceManager.stopService(any(), any()) }
    }

    /** - test_4: SubscribeCoroutineWorker → same pid + isRetry = false ⇒ Result.success, stopService(SUBSCRIBE_SERVICE). */
    @Test
    fun subscribeWorker_success_path_calls_stopService_when_isRetry_false_and_pid_same() = runTest {
        coEvery { PushSubscribe.isRetry(any(), any()) } returns false
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = subscribeWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { PushSubscribe.isRetry(worker.applicationContext, worker.id) }
        verify(exactly = 1) { ServiceManager.stopService(worker.applicationContext, SUBSCRIBE_SERVICE) }
    }

    /** - test_5: UpdateCoroutineWorker → same pid + isRetry = true ⇒ Result.retry, no stopService. */
    @Test
    fun updateWorker_retry_path_no_stopService_when_isRetry_true_and_pid_same() = runTest {
        coEvery { TokenUpdate.isRetry(any(), any()) } returns true
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = updateWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { TokenUpdate.isRetry(worker.applicationContext, worker.id) }
        verify(exactly = 0) { ServiceManager.stopService(any(), any()) }
    }

    /** - test_6: UpdateCoroutineWorker → same pid + isRetry = false ⇒ Result.success, stopService(UPDATE_SERVICE). */
    @Test
    fun updateWorker_success_path_calls_stopService_when_isRetry_false_and_pid_same() = runTest {
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = updateWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { TokenUpdate.isRetry(worker.applicationContext, worker.id) }
        verify(exactly = 1) { ServiceManager.stopService(worker.applicationContext, UPDATE_SERVICE) }
    }

    /** - test_7: MobileEventCoroutineWorker → same pid + MobileEvent.isRetry = true ⇒ Result.retry. */
    @Test
    fun mobileEventWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { MobileEvent.isRetry(any(), any()) } returns true

        val worker = mobileEventWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { MobileEvent.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_8: MobileEventCoroutineWorker → same pid + MobileEvent.isRetry = false ⇒ Result.success. */
    @Test
    fun mobileEventWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { MobileEvent.isRetry(any(), any()) } returns false

        val worker = mobileEventWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { MobileEvent.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_9: PushEventCoroutineWorker → pidChanged ⇒ Result.success, isRetry not called. */
    @Test
    fun pushEventWorker_pidChanged_skips_isRetry_and_returns_success() = runTest {
        coEvery { PushEvent.isRetry(any(), any()) } returns true

        val worker = pushEventWorker(samePid = false)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { PushEvent.isRetry(any(), any()) }
    }

    /** - test_10: SubscribeCoroutineWorker → pidChanged ⇒ Result.success, stopService(SUBSCRIBE_SERVICE), isRetry not called. */
    @Test
    fun subscribeWorker_pidChanged_calls_stopService_and_skips_isRetry() = runTest {
        coEvery { PushSubscribe.isRetry(any(), any()) } returns true
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = subscribeWorker(samePid = false)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { PushSubscribe.isRetry(any(), any()) }
        verify(exactly = 1) { ServiceManager.stopService(worker.applicationContext, SUBSCRIBE_SERVICE) }
    }

    /** - test_11: UpdateCoroutineWorker → pidChanged ⇒ Result.success, stopService(UPDATE_SERVICE), isRetry not called. */
    @Test
    fun updateWorker_pidChanged_calls_stopService_and_skips_isRetry() = runTest {
        coEvery { TokenUpdate.isRetry(any(), any()) } returns true
        every { ServiceManager.stopService(any(), any()) } returns Unit

        val worker = updateWorker(samePid = false)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { TokenUpdate.isRetry(any(), any()) }
        verify(exactly = 1) { ServiceManager.stopService(worker.applicationContext, UPDATE_SERVICE) }
    }

    /** - test_12: MobileEventCoroutineWorker → pidChanged ⇒ Result.success, isRetry not called. */
    @Test
    fun mobileEventWorker_pidChanged_skips_isRetry_and_returns_success() = runTest {
        coEvery { MobileEvent.isRetry(any(), any()) } returns true

        val worker = mobileEventWorker(samePid = false)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { MobileEvent.isRetry(any(), any()) }
    }
}