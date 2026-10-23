@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.data.Constants.CORE_P_WORK_TAG
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.Worker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PeriodicalLaunchFunctionsInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: startPeriodicalAuthRequestWorker() schedules periodic work.
 */
@RunWith(AndroidJUnit4::class)
class PeriodicalLaunchFunctionsInstrumentedTest {

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

        mockkObject(
            SubFunction,
            AuthenticateProcess
        )

        every { SubFunction.isAppInForegrounded() } returns false

        coEvery { AuthenticateProcess.isRetry(any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: startPeriodicalAuthRequestWorker() schedules periodic work. */
    @Test
    fun test_1_startPeriodicalAuthRequestWorker_runs() {
        CommonFunctions.startPeriodicalWorker<Worker.PeriodicWorker>(context, CORE_P_WORK_TAG)

        val infos = workManager.getWorkInfosForUniqueWork(CORE_P_WORK_TAG).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }
}
