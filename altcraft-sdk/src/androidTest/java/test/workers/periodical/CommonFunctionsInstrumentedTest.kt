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

// SDK
import com.altcraft.sdk.data.Constants.CHECK_P_WORK_NANE
import com.altcraft.sdk.data.Constants.EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.workers.periodical.CommonFunctions

/**
 * CommonFunctionsInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: periodicalWorkerControl() — when nothing scheduled, it schedules all 4 periodic works.
 *  - test_2: periodicalWorkerControl() — when works are already enqueued, it doesn’t duplicate them.
 *  - test_3: createWorker() — enqueues unique periodic work with UPDATE policy.
 *  - test_4: cancelPeriodicalWorkersTask() — cancels all 4 unique works by name.
 *
 * Notes:
 *  - No static mocking of WorkManager; we use the real test WorkManager via WorkManagerTestInitHelper.
 *  - We assert by inspecting real WorkManager state (WorkInfo) for each unique work name.
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

        // Clean slate before each test
        wm.cancelAllWork().result.get()
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
    }

    // region helpers

    private fun stateOf(name: String): WorkInfo.State? =
        wm.getWorkInfosForUniqueWork(name).get().firstOrNull()?.state

    private fun enqueueDummyFor(name: String) {
        // Use a generic periodic request; we don't need a specific Worker class here
        val req: PeriodicWorkRequest = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)
        CommonFunctions.createWorker(context, name, req)
    }

    // Minimal no-op CoroutineWorker for periodic requests
    class DummyPeriodicWorker(
        appContext: Context,
        params: androidx.work.WorkerParameters
    ) : androidx.work.CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result = Result.success()
    }

    // endregion

    /** Schedules all 4 periodic works when nothing is enqueued yet. */
    @Test
    fun periodicalWorkerControl_starts_all_when_none_exist() = runTest {
        // Pre-condition: nothing scheduled
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(CHECK_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(true))

        // Act
        CommonFunctions.periodicalWorkerControl(context)

        // Assert: each unique work name has at least one ENQUEUED request
        assertThat(stateOf(UPDATE_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                stateOf(UPDATE_P_WORK_NANE) == WorkInfo.State.RUNNING, `is`(true))
        assertThat(stateOf(EVENT_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                stateOf(EVENT_P_WORK_NANE) == WorkInfo.State.RUNNING, `is`(true))
        assertThat(stateOf(CHECK_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                stateOf(CHECK_P_WORK_NANE) == WorkInfo.State.RUNNING, `is`(true))
        assertThat(stateOf(SUB_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                stateOf(SUB_P_WORK_NANE) == WorkInfo.State.RUNNING, `is`(true))
    }

    /** Does not duplicate scheduling if items are already ENQUEUED. */
    @Test
    fun periodicalWorkerControl_noop_when_already_enqueued() = runTest {
        // Arrange: pre-enqueue all 4 names
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(EVENT_P_WORK_NANE)
        enqueueDummyFor(CHECK_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)

        val beforeCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(EVENT_P_WORK_NANE).get().size,
            CHECK_P_WORK_NANE to wm.getWorkInfosForUniqueWork(CHECK_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        // Act
        CommonFunctions.periodicalWorkerControl(context)

        // Assert: counts stay the same due to unique work + UPDATE policy
        val afterCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(EVENT_P_WORK_NANE).get().size,
            CHECK_P_WORK_NANE to wm.getWorkInfosForUniqueWork(CHECK_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        assertThat(afterCounts[UPDATE_P_WORK_NANE], `is`(beforeCounts[UPDATE_P_WORK_NANE]))
        assertThat(afterCounts[EVENT_P_WORK_NANE], `is`(beforeCounts[EVENT_P_WORK_NANE]))
        assertThat(afterCounts[CHECK_P_WORK_NANE], `is`(beforeCounts[CHECK_P_WORK_NANE]))
        assertThat(afterCounts[SUB_P_WORK_NANE], `is`(beforeCounts[SUB_P_WORK_NANE]))
    }

    /** Enqueues unique periodic work with UPDATE policy. */
    @Test
    fun createWorker_enqueues_unique_periodic_work() {
        val req = CommonFunctions.createRequest(DummyPeriodicWorker::class.java)

        // Act
        CommonFunctions.createWorker(context, "TEST_UNIQUE_NAME", req)

        // Assert
        val infos = wm.getWorkInfosForUniqueWork("TEST_UNIQUE_NAME").get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** Cancels all 4 unique works by name. */
    @Test
    fun cancelPeriodicalWorkersTask_cancels_all_unique_works() {
        // Arrange: enqueue dummies under expected names
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(EVENT_P_WORK_NANE)
        enqueueDummyFor(CHECK_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)

        // Sanity
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(EVENT_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(CHECK_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(false))

        // Act
        CommonFunctions.cancelPeriodicalWorkersTask(context)

        // Assert
        // WorkManager marks them CANCELLED or removes them; both are acceptable here.
        val upd = wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get()
        val evt = wm.getWorkInfosForUniqueWork(EVENT_P_WORK_NANE).get()
        val chk = wm.getWorkInfosForUniqueWork(CHECK_P_WORK_NANE).get()
        val sub = wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get()

        val updCancelled = upd.isEmpty() || upd.all { it.state == WorkInfo.State.CANCELLED }
        val evtCancelled = evt.isEmpty() || evt.all { it.state == WorkInfo.State.CANCELLED }
        val chkCancelled = chk.isEmpty() || chk.all { it.state == WorkInfo.State.CANCELLED }
        val subCancelled = sub.isEmpty() || sub.all { it.state == WorkInfo.State.CANCELLED }

        assertThat(updCancelled, `is`(true))
        assertThat(evtCancelled, `is`(true))
        assertThat(chkCancelled, `is`(true))
        assertThat(subCancelled, `is`(true))
    }
}
