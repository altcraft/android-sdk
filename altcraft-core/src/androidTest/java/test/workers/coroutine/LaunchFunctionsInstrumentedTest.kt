@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

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
import com.altcraft.sdk.data.Constants.AUTH_REQUEST_O_WORK_TAG
import com.altcraft.sdk.workers.onetime.Request
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateWorker

/**
 * LaunchFunctionsInstrumentedTest
 *
 * test_1: startAuthRequestWorker(): enqueues worker and it moves to RUNNING.
 */
@RunWith(AndroidJUnit4::class)
class LaunchFunctionsInstrumentedTest {

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

        mockkObject(AuthenticateProcess)

        coEvery { AuthenticateProcess.isRetry(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkObject(AuthenticateProcess)
        unmockkAll()
    }

    /** test_1: startAuthRequestWorker() enqueues worker and it moves to RUNNING. */
    @Test
    fun test_1_startAuthRequestWorker_runsAndSucceeds() {
        Request.startWorker<AuthenticateWorker>(context, AUTH_REQUEST_O_WORK_TAG)

        val infos = workManager.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        assertEquals("Exactly one AuthRequest work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.RUNNING, finished?.state)
    }
}
