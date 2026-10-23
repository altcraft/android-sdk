@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

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
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateWorker
import com.altcraft.sdk.data.Constants.PID
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
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
 *  - test_1: AuthRequestWorker → pid same + AuthenticateProcess.isRetry = true ⇒ Result.retry.
 *  - test_2: AuthRequestWorker → pid same + AuthenticateProcess.isRetry = false ⇒ Result.success.
 *  - test_3: pidChanged branch (auth) ⇒ Result.success, isRetry not called.
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
        mockkObject(AuthenticateProcess)

        every { SubFunction.isAppInForegrounded() } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(
            SubFunction,
            AuthenticateProcess
        )
        unmockkAll()
    }

    private fun authWorker(samePid: Boolean = true): AuthenticateWorker {
        val data = Data.Builder()
            .putInt(PID, if (samePid) Process.myPid() else -1)
            .build()
        return TestListenableWorkerBuilder<AuthenticateWorker>(context)
            .setInputData(data)
            .build()
    }

    /** - test_1: AuthRequestWorker → same pid + AuthenticateProcess.isRetry = true ⇒ Result.retry. */
    @Test
    fun authWorker_returns_retry_when_isRetry_true_and_pid_same() = runTest {
        coEvery { AuthenticateProcess.isRetry(any(), any()) } returns true

        val worker = authWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Retry, `is`(true))
        coVerify(exactly = 1) { AuthenticateProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_2: AuthRequestWorker → same pid + AuthenticateProcess.isRetry = false ⇒ Result.success. */
    @Test
    fun authWorker_returns_success_when_isRetry_false_and_pid_same() = runTest {
        coEvery { AuthenticateProcess.isRetry(any(), any()) } returns false

        val worker = authWorker(samePid = true)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 1) { AuthenticateProcess.isRetry(worker.applicationContext, worker.id) }
    }

    /** - test_3: AuthRequestWorker → pidChanged ⇒ Result.success, isRetry not called. */
    @Test
    fun authWorker_pidChanged_skips_isRetry_and_returns_success() = runTest {
        coEvery { AuthenticateProcess.isRetry(any(), any()) } returns true

        val worker = authWorker(samePid = false)
        val result = worker.doWork()

        assertThat(result is ListenableWorker.Result.Success, `is`(true))
        coVerify(exactly = 0) { AuthenticateProcess.isRetry(any(), any()) }
    }
}
