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

// SDK
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
 * Positive scenarios:
 *  - test_1: pushPeriodicalWorkerControl() — when nothing scheduled, it schedules 3 periodic works
 *            (SUB, UPDATE, PUSH_EVENT).
 *  - test_2: pushPeriodicalWorkerControl() — when works are already enqueued, it doesn’t duplicate them.
 *  - test_3: mobileEventPeriodicalWorkerControl() — when none scheduled, it schedules MOBILE_EVENT.
 *  - test_4: mobileEventPeriodicalWorkerControl() — when already enqueued, no duplication.
 *  - test_5: createWorker() — enqueues unique periodic work with UPDATE policy.
 *  - test_6: cancelPeriodicalWorkersTask() — cancels all 4 unique works by name (SUB, UPDATE, PUSH_EVENT, MOBILE_EVENT).
 *
 * Notes:
 *  - Uses real test WorkManager via WorkManagerTestInitHelper.
 *  - Asserts by inspecting real WorkManager state (WorkInfo) for each unique work name.
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

    /** Schedules SUB/UPDATE/PUSH_EVENT when none exist yet. */
    @Test
    fun pushPeriodicalWorkerControl_starts_three_when_none_exist() = runTest {
        // Pre-condition: nothing scheduled for these work names
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(true))

        // Act
        pushPeriodicalWorkerControl(context)

        // Assert: each unique work name is ENQUEUED (or RUNNING)
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

    /** Does not duplicate SUB/UPDATE/PUSH_EVENT scheduling if already ENQUEUED. */
    @Test
    fun pushPeriodicalWorkerControl_noop_when_already_enqueued() = runTest {
        // Arrange: pre-enqueue all three names
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(PUSH_EVENT_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)

        val beforeCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            PUSH_EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        // Act
        pushPeriodicalWorkerControl(context)

        // Assert: counts stay the same
        val afterCounts = mapOf(
            UPDATE_P_WORK_NANE to wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().size,
            PUSH_EVENT_P_WORK_NANE to wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().size,
            SUB_P_WORK_NANE to wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().size,
        )

        assertThat(afterCounts[UPDATE_P_WORK_NANE], `is`(beforeCounts[UPDATE_P_WORK_NANE]))
        assertThat(afterCounts[PUSH_EVENT_P_WORK_NANE], `is`(beforeCounts[PUSH_EVENT_P_WORK_NANE]))
        assertThat(afterCounts[SUB_P_WORK_NANE], `is`(beforeCounts[SUB_P_WORK_NANE]))
    }

    /** Schedules MOBILE_EVENT periodic work when none exists yet. */
    @Test
    fun mobileEventPeriodicalWorkerControl_starts_when_none_exist() = runTest {
        // Pre-condition: nothing scheduled for mobile event
        assertThat(wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().isEmpty(), `is`(true))

        // Act
        mobileEventPeriodicalWorkerControl(context)

        // Assert
        assertThat(
            stateOf(MOBILE_EVENT_P_WORK_NANE) == WorkInfo.State.ENQUEUED ||
                    stateOf(MOBILE_EVENT_P_WORK_NANE) == WorkInfo.State.RUNNING,
            `is`(true)
        )
    }

    /** Does not duplicate MOBILE_EVENT scheduling if already ENQUEUED. */
    @Test
    fun mobileEventPeriodicalWorkerControl_noop_when_already_enqueued() = runTest {
        // Arrange
        enqueueDummyFor(MOBILE_EVENT_P_WORK_NANE)
        val before = wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().size

        // Act
        mobileEventPeriodicalWorkerControl(context)

        // Assert
        val after = wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().size
        assertThat(after, `is`(before))
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

    /** Cancels all 4 unique works by name (SUB, UPDATE, PUSH_EVENT, MOBILE_EVENT). */
    @Test
    fun cancelPeriodicalWorkersTask_cancels_all_unique_works() {
        // Arrange: enqueue dummies under expected names
        enqueueDummyFor(UPDATE_P_WORK_NANE)
        enqueueDummyFor(PUSH_EVENT_P_WORK_NANE)
        enqueueDummyFor(SUB_P_WORK_NANE)
        enqueueDummyFor(MOBILE_EVENT_P_WORK_NANE)

        // Sanity
        assertThat(wm.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get().isEmpty(), `is`(false))
        assertThat(wm.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get().isEmpty(), `is`(false))

        // Act
        CommonFunctions.cancelPeriodicalWorkersTask(context)

        // Assert: cancelled or removed
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
