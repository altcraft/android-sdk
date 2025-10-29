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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.CommonFunctions.mobileEventPeriodicalWorkerControl
import com.altcraft.sdk.workers.periodical.CommonFunctions.pushPeriodicalWorkerControl

/**
 * CommonFunctionsInstrumentedTest
 *
 * Covers:
 * - pushPeriodicalWorkerControl() behavior for SUB/UPDATE/PUSH_EVENT periodic works.
 * - mobileEventPeriodicalWorkerControl() behavior for MOBILE_EVENT periodic work.
 * - createWorker() unique periodic work enqueue and cancelPeriodicalWorkersTask() bulk cancellation.
 *
 * Positive scenarios:
 * - test_1: pushPeriodicalWorkerControl() — when nothing scheduled, it schedules 3 periodic works (SUB, UPDATE, PUSH_EVENT).
 * - test_3: mobileEventPeriodicalWorkerControl() — when none scheduled, it schedules MOBILE_EVENT.
 * - test_5: createWorker() — enqueues unique periodic work with UPDATE policy.
 *
 * Negative scenarios:
 * - test_2: pushPeriodicalWorkerControl() — when works are already enqueued, it doesn’t duplicate them.
 * - test_4: mobileEventPeriodicalWorkerControl() — when already enqueued, no duplication.
 * - test_6: cancelPeriodicalWorkersTask() — cancels all 4 unique works by name (SUB, UPDATE, PUSH_EVENT, MOBILE_EVENT).
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
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
    }

    private fun stateOf(name: String): WorkInfo.State? =
        wm.getWorkInfosForUniqueWork(name).get().firstOrNull()?.state

    private fun enqueueDummyFor(name: String) {
        val req: PeriodicWorkRequest = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)
        CommonFunctions.createWorker(context, name, req)
    }

    class DummyPeriodicWorker(
        appContext: Context,
        params: androidx.work.WorkerParameters
    ) : androidx.work.CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result = Result.success()
    }

    /** - test_1: pushPeriodicalWorkerControl() schedules SUB/UPDATE/PUSH_EVENT when none exist. */
    @Test
    fun pushPeriodicalWorkerControl_starts_three_when_none_exist() = runTest {
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(true))

        pushPeriodicalWorkerControl(context)

        assertThat(
            stateOf(UPDATE_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                    stateOf(UPDATE_P_WORK_NANE) == WorkInfo.State.RUNNING,
            `is`(true)
        )
        assertThat(
            stateOf(PUSH_EVENT_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                    stateOf(PUSH_EVENT_P_WORK_NANE) == WorkInfo.State.RUNNING,
            `is`(true)
        )
        assertThat(
            stateOf(SUB_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                    stateOf(SUB_P_WORK_NANE) == WorkInfo.State.RUNNING,
            `is`(true)
        )
    }

    /** - test_2: pushPeriodicalWorkerControl() does nothing if SUB/UPDATE/PUSH_EVENT already enqueued. */
    @Test
    fun pushPeriodicalWorkerControl_noop_when_already_enqueued() = runTest {
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(PUSH_EVENT_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)

        val beforeCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            PUSH_EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        pushPeriodicalWorkerControl(context)

        val afterCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            PUSH_EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        assertThat(afterCounts[UPDATE_P_WORK_NANE], `is`(beforeCounts[UPDATE_P_WORK_NANE]))
        assertThat(afterCounts[PUSH_EVENT_P_WORK_NANE], `is`(beforeCounts[PUSH_EVENT_P_WORK_NANE]))
        assertThat(afterCounts[SUB_P_WORK_NANE], `is`(beforeCounts[SUB_P_WORK_NANE]))
    }

    /** - test_3: mobileEventPeriodicalWorkerControl() schedules MOBILE_EVENT when none exist. */
    @Test
    fun mobileEventPeriodicalWorkerControl_starts_when_none_exist() = runTest {
        assertThat(wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))

        mobileEventPeriodicalWorkerControl(context)

        assertThat(
            stateOf(MOBILE_EVENT_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                    stateOf(MOBILE_EVENT_P_WORK_NANE) == WorkInfo.State.RUNNING,
            `is`(true)
        )
    }

    /** - test_4: mobileEventPeriodicalWorkerControl() does nothing if MOBILE_EVENT already enqueued. */
    @Test
    fun mobileEventPeriodicalWorkerControl_noop_when_already_enqueued() = runTest {
        enqueueDummyFor(MOBILE_EVENT_P_WORK_NANE)
        val before = wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().size

        mobileEventPeriodicalWorkerControl(context)

        val after = wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().size
        assertThat(after, `is`(before))
    }

    /** - test_5: createWorker() enqueues unique periodic work with UPDATE policy. */
    @Test
    fun createWorker_enqueues_unique_periodic_work() {
        val req = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)

        CommonFunctions.createWorker(context, "TEST_UNIQUE_NAME", req)

        val infos = wm.getWorkInfosForUniqueWork("TEST_UNIQUE_NAME").get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_6: cancelPeriodicalWorkersTask() cancels SUB/UPDATE/PUSH_EVENT/MOBILE_EVENT unique works. */
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
}