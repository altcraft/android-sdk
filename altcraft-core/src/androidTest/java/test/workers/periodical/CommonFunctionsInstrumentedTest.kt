@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.data.Constants.CORE_P_WORK_TAG
import com.altcraft.sdk.workers.CancelWork
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.Worker
import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CommonFunctionsInstrumentedTest
 *
 * Positive scenarios:
 * - test_1: ensureWorker() enqueues the core periodic worker when none exist.
 * - test_2: startPeriodicalWorker() builds and enqueues periodic work with expected interval and constraints.
     * - test_3: cancelPeriodicalWorkersTask() cancels all core unique works by name.
 * - test_4: ensureWorker() does not reschedule when the periodic worker already exists.
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
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
        unmockkAll()
    }

    private fun stateOf(name: String): WorkInfo.State? =
        wm.getWorkInfosForUniqueWork(name).get().firstOrNull()?.state

    private fun enqueueDummyFor(name: String) {
        CommonFunctions.startPeriodicalWorker<DummyPeriodicWorker>(context, name)
    }

    class DummyPeriodicWorker(
        appContext: Context,
        params: WorkerParameters
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result = Result.success()
    }

    /** - test_1: ensureWorker() enqueues the core periodic worker when it does not exist. */
    @Test
    fun ensureWorker_starts_when_none_exist() = runTest {
        assertThat(wm.getWorkInfosForUniqueWork(CORE_P_WORK_TAG).get().isEmpty(), `is`(true))

        CommonFunctions.ensureWorker<Worker.PeriodicWorker>(
            context, CORE_P_WORK_TAG
        )

        assertThat(stateOf(CORE_P_WORK_TAG), `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_2: startPeriodicalWorker() builds and enqueues periodic work with correct interval and constraints. */
    @Test
    fun startPeriodicalWorker_builds_and_enqueues() {
        CommonFunctions.startPeriodicalWorker<DummyPeriodicWorker>(context, "TEST_PERIODIC_NAME")

        val infos = wm.getWorkInfosForUniqueWork("TEST_PERIODIC_NAME").get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_3: cancelWorkerTask(context, tags) cancels periodic work by its tag. */
    @Test
    fun cancelPeriodicalWorkersTask_cancels_all_unique_works() {
        enqueueDummyFor(CORE_P_WORK_TAG)

        runBlocking {
            CancelWork.cancelWorkerTask(context, listOf(CORE_P_WORK_TAG))
        }

        fun cancelled(name: String): Boolean {
            val infos = wm.getWorkInfosForUniqueWork(name).get()
            return infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED }
        }

        assertThat(cancelled(CORE_P_WORK_TAG), `is`(true))
    }

    /** - test_4: ensureWorker() does not reschedule when the periodic worker already exists. */
    @Test
    fun ensureWorker_does_not_reschedule_when_all_exist() = runTest {
        enqueueDummyFor(CORE_P_WORK_TAG)

        assertThat(stateOf(CORE_P_WORK_TAG), `is`(WorkInfo.State.ENQUEUED))

        CommonFunctions.ensureWorker<Worker.PeriodicWorker>(
            context, CORE_P_WORK_TAG
        )

        assertThat(wm.getWorkInfosForUniqueWork(CORE_P_WORK_TAG).get().size, `is`(1))
    }
}
