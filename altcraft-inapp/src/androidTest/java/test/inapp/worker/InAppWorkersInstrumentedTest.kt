@file:Suppress("SpellCheckingInspection")

package test.inapp.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

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
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import com.altcraft.sdk.inapp.platform.static.StaticProcess
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.inapp.platform.event.EventProcess
import com.altcraft.sdk.inapp.worker.PeriodicWorker
import com.altcraft.sdk.inapp.platform.event.EventWorker
import com.altcraft.sdk.inapp.platform.placements.PlacementsWorker
import com.altcraft.sdk.inapp.platform.static.StaticWorker
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionWorker
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * InAppWorkersInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: Subscribe → pid same + isRetry = true ⇒ Result.retry.
 *  - test_2: Subscribe → pid same + isRetry = false ⇒ Result.success.
 *  - test_3: Event → pid same + isRetry = true ⇒ Result.retry.
 *  - test_4: Event → pid same + isRetry = false ⇒ Result.success.
 *  - test_5: Static → pid same + isRetry = true ⇒ Result.retry.
 *  - test_6: Static → pid same + isRetry = false ⇒ Result.success.
 *  - test_7: Placements → pid same + isRetry = true ⇒ Result.retry.
 *  - test_8: Placements → pid same + isRetry = false ⇒ Result.success.
 *
 * Negative scenarios:
 *  - test_9: Subscribe → pidChanged ⇒ Result.success, isRetry not called.
 *  - test_10: PeriodicWorker (background) calls isRetry → success.
 *  - test_11: PeriodicWorker (foreground) skips body → success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class InAppWorkersInstrumentedTest {

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
        mockkObject(SubscriptionProcess)
        mockkObject(EventProcess)
        mockkObject(StaticProcess)
        mockkObject(PlacementsProcess)

        every { SubFunction.isAppInForegrounded() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun pidData(samePid: Boolean = true): Data {
        return Data.Builder()
            .putInt("pid", if (samePid) Process.myPid() else -1)
            .build()
    }

    /** - test_1: SubscriptionWorker → same pid + isRetry = true ⇒ Result.retry. */
    @Test
    fun subscribeWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { SubscriptionProcess.isRetry(any(), any()) } returns true

        val worker = TestListenableWorkerBuilder<SubscriptionWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { SubscriptionProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_2: SubscriptionWorker → same pid + isRetry = false ⇒ Result.success. */
    @Test
    fun subscribeWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { SubscriptionProcess.isRetry(any(), any()) } returns false

        val worker = TestListenableWorkerBuilder<SubscriptionWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { SubscriptionProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_3: EventWorker → same pid + isRetry = true ⇒ Result.retry. */
    @Test
    fun eventWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { EventProcess.isRetry(any(), any()) } returns true

        val worker = TestListenableWorkerBuilder<EventWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { EventProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_4: EventWorker → same pid + isRetry = false ⇒ Result.success. */
    @Test
    fun eventWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { EventProcess.isRetry(any(), any()) } returns false

        val worker = TestListenableWorkerBuilder<EventWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { EventProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_5: StaticWorker → same pid + isRetry = true ⇒ Result.retry. */
    @Test
    fun staticWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { StaticProcess.isRetry(any(), any()) } returns true

        val worker = TestListenableWorkerBuilder<StaticWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { StaticProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_6: StaticWorker → same pid + isRetry = false ⇒ Result.success. */
    @Test
    fun staticWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { StaticProcess.isRetry(any(), any()) } returns false

        val worker = TestListenableWorkerBuilder<StaticWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { StaticProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_7: PlacementsWorker → same pid + isRetry = true ⇒ Result.retry. */
    @Test
    fun placementsWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { PlacementsProcess.isRetry(any(), any()) } returns true

        val worker = TestListenableWorkerBuilder<PlacementsWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { PlacementsProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_8: PlacementsWorker → same pid + isRetry = false ⇒ Result.success. */
    @Test
    fun placementsWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { PlacementsProcess.isRetry(any(), any()) } returns false

        val worker = TestListenableWorkerBuilder<PlacementsWorker>(context)
            .setInputData(pidData(true))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { PlacementsProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_9: Subscribe → pidChanged ⇒ Result.success, isRetry not called. */
    @Test
    fun subscribeWorker_pidChanged_skips_isRetry_and_returns_success() = runTest {
        coEvery { SubscriptionProcess.isRetry(any(), any()) } returns true

        val worker = TestListenableWorkerBuilder<SubscriptionWorker>(context)
            .setInputData(pidData(samePid = false))
            .build()
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { SubscriptionProcess.isRetry(any(), any()) }
    }

    /** - test_10: PeriodicWorker (background) calls isRetry → success. */
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun retryWorker_background_callsRetry_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { SubscriptionProcess.isRetry(any()) } returns true
        coEvery { EventProcess.isRetry(any()) } returns false
        every { PlacementsProcess.requestPlacements(any(), any(), any()) } just Runs

        val worker = TestListenableWorkerBuilder<PeriodicWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { SubscriptionProcess.isRetry(any()) }
        coVerify(exactly = 1) { EventProcess.isRetry(any()) }
        verify(exactly = 1) { PlacementsProcess.requestPlacements(any(), any(), any()) }
    }

    /** - test_11: PeriodicWorker (foreground) skips body → success. */
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun retryWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<PeriodicWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { SubscriptionProcess.isRetry(any()) }
        coVerify(exactly = 0) { EventProcess.isRetry(any()) }
        verify(exactly = 0) { PlacementsProcess.requestPlacements(any(), any(), any()) }
    }
}
