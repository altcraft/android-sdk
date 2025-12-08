@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.core.Retry
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.LaunchFunctions
import io.mockk.MockKAnnotations
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
import java.util.concurrent.TimeUnit

/**
 * CommonFunctionsInstrumentedTest
 *
 * Positive scenarios:
 * - test_1: periodicalWorkerControl() — when nothing scheduled and push active, it triggers
 *   periodic workers for MOBILE, PUSH_EVENT, UPDATE, SUBSCRIBE.
 * - test_2: periodicalWorkerControl() — when nothing scheduled and push disabled, only MOBILE
 *   worker is triggered.
 * - test_3: createRequest() — builds PeriodicWorkRequest with expected interval and constraints.
 * - test_4: createWorker() — enqueues unique periodic work with UPDATE policy.
 * - test_5: cancelPeriodicalWorkersTask() — cancels all 4 unique works by name.
 * - test_6: periodicalWorkerControl() — when all works already enqueued, it does not reschedule.
 * - test_7: awaitCancel() — suspends until the provided task calls its completion callback.
 */
@RunWith(AndroidJUnit4::class)
class CommonFunctionsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var wm: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        wm = WorkManager.getInstance(context)
        wm.cancelAllWork().result.get()

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(LaunchFunctions)
        mockkObject(Retry)
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
        unmockkObject(LaunchFunctions)
        unmockkObject(Retry)
        unmockkAll()
    }

    private fun stateOf(name: String): WorkInfo.State? =
        wm.getWorkInfosForUniqueWork(name).get().firstOrNull()?.state

    private fun enqueueDummyFor(name: String) {
        val req: PeriodicWorkRequest =
            CommonFunctions.createRequest(DummyPeriodicWorker::class.java)
        CommonFunctions.createWorker(context, name, req)
    }

    class DummyPeriodicWorker(
        appContext: Context,
        params: WorkerParameters
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result = Result.success()
    }

    /** - test_1: periodicalWorkerControl() starts all periodic workers when none exist and push is active. */
    @Test
    fun periodicalWorkerControl_starts_all_when_none_exist_and_push_active() = runTest {
        every { Retry.pushModuleIsActive(any()) } returns true
        every { LaunchFunctions.startPeriodicalMobileEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalPushEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalUpdateWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalSubscribeWorker(any()) } returns Unit

        assertThat(wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(true))

        CommonFunctions.periodicalWorkerControl(context)

        verify(exactly = 1) { LaunchFunctions.startPeriodicalMobileEventWorker(context) }
        verify(exactly = 1) { LaunchFunctions.startPeriodicalPushEventWorker(context) }
        verify(exactly = 1) { LaunchFunctions.startPeriodicalUpdateWorker(context) }
        verify(exactly = 1) { LaunchFunctions.startPeriodicalSubscribeWorker(context) }
    }

    /** - test_2: periodicalWorkerControl() starts only mobile worker when pushModuleIsActive is false. */
    @Test
    fun periodicalWorkerControl_starts_only_mobile_when_push_inactive() = runTest {
        every { Retry.pushModuleIsActive(any()) } returns false
        every { LaunchFunctions.startPeriodicalMobileEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalPushEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalUpdateWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalSubscribeWorker(any()) } returns Unit

        CommonFunctions.periodicalWorkerControl(context)

        verify(exactly = 1) { LaunchFunctions.startPeriodicalMobileEventWorker(context) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalPushEventWorker(any()) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalUpdateWorker(any()) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalSubscribeWorker(any()) }
    }

    /** - test_3: createRequest() builds PeriodicWorkRequest with expected interval and constraints. */
    @Test
    fun createRequest_builds_periodic_work_with_expected_properties() {
        val req = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)

        val intervalHours = TimeUnit.MILLISECONDS.toHours(req.workSpec.intervalDuration)
        assertThat(
            intervalHours,
            `is`(com.altcraft.sdk.data.Constants.RETRY_TIME_P_WORK)
        )

        val networkType = req.workSpec.constraints.requiredNetworkType
        assertThat(networkType, `is`(androidx.work.NetworkType.CONNECTED))
    }

    /** - test_4: createWorker() enqueues unique periodic work with UPDATE policy. */
    @Test
    fun createWorker_enqueues_unique_periodic_work() {
        val req = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)

        CommonFunctions.createWorker(context, "TEST_UNIQUE_NAME", req)

        val infos = wm.getWorkInfosForUniqueWork("TEST_UNIQUE_NAME").get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_5: cancelPeriodicalWorkersTask() cancels SUB/UPDATE/PUSH_EVENT/MOBILE_EVENT unique works. */
    @Test
    fun cancelPeriodicalWorkersTask_cancels_all_unique_works() {
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(PUSH_EVENT_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)
        enqueueDummyFor(MOBILE_EVENT_P_WORK_NANE)

        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().isEmpty(), `is`(false))

        CommonFunctions.cancelPeriodicalWorkersTask(context)

        fun cancelled(name: String): Boolean {
            val infos = wm.getWorkInfosForUniqueWork(name).get()
            return infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED }
        }
        assertThat(cancelled(UPDATE_P_WORK_NANE), `is`(true))
        assertThat(cancelled(PUSH_EVENT_P_WORK_NANE), `is`(true))
        assertThat(cancelled(SUB_P_WORK_NANE), `is`(true))
        assertThat(cancelled(MOBILE_EVENT_P_WORK_NANE), `is`(true))
    }

    /** - test_6: periodicalWorkerControl() does not reschedule when all periodic works already exist. */
    @Test
    fun periodicalWorkerControl_does_not_reschedule_when_all_exist() = runTest {
        every { Retry.pushModuleIsActive(any()) } returns true
        every { LaunchFunctions.startPeriodicalMobileEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalPushEventWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalUpdateWorker(any()) } returns Unit
        every { LaunchFunctions.startPeriodicalSubscribeWorker(any()) } returns Unit

        enqueueDummyFor(MOBILE_EVENT_P_WORK_NANE)
        enqueueDummyFor(PUSH_EVENT_P_WORK_NANE)
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)

        assertThat(stateOf(MOBILE_EVENT_P_WORK_NANE), `is`(WorkInfo.State.ENQUEUED))
        assertThat(stateOf(PUSH_EVENT_P_WORK_NANE), `is`(WorkInfo.State.ENQUEUED))
        assertThat(stateOf(UPDATE_P_WORK_NANE), `is`(WorkInfo.State.ENQUEUED))
        assertThat(stateOf(SUB_P_WORK_NANE), `is`(WorkInfo.State.ENQUEUED))

        CommonFunctions.periodicalWorkerControl(context)

        verify(exactly = 0) { LaunchFunctions.startPeriodicalMobileEventWorker(any()) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalPushEventWorker(any()) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalUpdateWorker(any()) }
        verify(exactly = 0) { LaunchFunctions.startPeriodicalSubscribeWorker(any()) }
    }

    /** - test_7: awaitCancel() suspends until the task calls its completion callback. */
    @Test
    fun awaitCancel_suspends_until_task_calls_completion() = runTest {
        val trace = mutableListOf<String>()

        val task: (Context, () -> Unit) -> Unit = { _, done ->
            trace.add("task-start")
            done()
            trace.add("task-after-done")
        }

        trace.add("before")
        CommonFunctions.awaitCancel(context, task)
        trace.add("after")

        assertThat(
            trace,
            `is`(listOf("before", "task-start", "task-after-done", "after"))
        )
    }
}