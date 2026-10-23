@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.workers.CancelWork
import com.altcraft.sdk.workers.periodical.Worker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * WorkersInstrumentedTest
 *
 * Verifies periodic workers behavior in foreground/background:
 * - test_1: RetryAuthRequestWorker (background) calls cancelWorkerTask, AuthenticateProcess.isRetry → success.
 * - test_2: RetryAuthRequestWorker (foreground) skips body → success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkersInstrumentedTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(
            SubFunction,
            AuthenticateProcess,
            RoomRequest,
            CancelWork,
            Events
        )
        coEvery { CancelWork.cancelWorkerTask(any(), any<List<String>>()) } returns Unit
        every { Events.error(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: RetryAuthRequestWorker (background) calls cancelWorkerTask, AuthenticateProcess.isRetry, returns success. */
    @Test
    fun retryAuthRequestWorker_background_callsRetry_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { AuthenticateProcess.isRetry(ctx) } returns false

        val worker = TestListenableWorkerBuilder<Worker.PeriodicWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { CancelWork.cancelWorkerTask(ctx, any<List<String>>()) }
        coVerify(exactly = 1) { AuthenticateProcess.isRetry(ctx) }
    }

    /** - test_2: RetryAuthRequestWorker (foreground) skips body, returns success. */
    @Test
    fun retryAuthRequestWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Worker.PeriodicWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CancelWork.cancelWorkerTask(any(), any<List<String>>()) }
        coVerify(exactly = 0) { AuthenticateProcess.isRetry(any()) }
    }
}
