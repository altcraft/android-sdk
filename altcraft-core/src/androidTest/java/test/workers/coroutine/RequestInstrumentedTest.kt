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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateWorker
import java.util.UUID

/**
 * RequestInstrumentedTest
 *
 * test_1: authRequest enqueues a worker with the correct tag and runs -> SUCCEEDED.
 * test_2: hasNewRequest() returns false when there is a single active work.
 * test_3: hasNewRequest() returns true for older work and false for the newest one.
 */
@RunWith(AndroidJUnit4::class)
class RequestInstrumentedTest {

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

    private fun awaitWorkSucceeded(id: UUID, timeoutMs: Long = 2000L): WorkInfo.State? {
        val start = System.currentTimeMillis()
        var state: WorkInfo.State? = null
        while (System.currentTimeMillis() - start < timeoutMs) {
            state = workManager.getWorkInfoById(id).get()?.state
            if (state != null && state.isFinished) break
            Thread.sleep(50)
        }
        return state
    }

    /** test_1: authRequest enqueues with correct tag and runs to SUCCEEDED. */
    @Test
    fun test_1_authRequest_buildsAndRunsSucceeded() {
        Request.startWorker<AuthenticateWorker>(context, AUTH_REQUEST_O_WORK_TAG)

        val infos = workManager.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        assertEquals(1, infos.size)
        val reqId = infos.first().id
        assertTrue(infos.first().tags.contains(AUTH_REQUEST_O_WORK_TAG))

        testDriver!!.setAllConstraintsMet(reqId)

        val state = awaitWorkSucceeded(reqId)
        assertEquals(WorkInfo.State.SUCCEEDED, state)

        io.mockk.coVerify(exactly = 1) { AuthenticateProcess.isRetry(any(), reqId) }
    }

    /** test_2: hasNewRequest returns false when there is a single active work for the tag. */
    @Test
    fun test_2_hasNewRequest_singleActive_returnsFalse() = runBlocking {
        Request.startWorker<AuthenticateWorker>(context, AUTH_REQUEST_O_WORK_TAG)

        val infos = workManager.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        val reqId = infos.first().id

        val hasNew = Request.hasNewRequest(context, AUTH_REQUEST_O_WORK_TAG, reqId)
        assertFalse(hasNew)
    }

    /** test_3: hasNewRequest returns true for older work and false for the newest one. */
    @Test
    fun test_3_hasNewRequest_detectsNewerRequest() = runBlocking {
        Request.startWorker<AuthenticateWorker>(context, AUTH_REQUEST_O_WORK_TAG)
        val infos1 = workManager.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        val firstId = infos1.first().id

        Thread.sleep(5)

        Request.startWorker<AuthenticateWorker>(context, AUTH_REQUEST_O_WORK_TAG)
        val infos2 = workManager.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        val secondId = infos2.first { it.id != firstId }.id

        val hasNewForFirst = Request.hasNewRequest(context, AUTH_REQUEST_O_WORK_TAG, firstId)
        val hasNewForSecond = Request.hasNewRequest(context, AUTH_REQUEST_O_WORK_TAG, secondId)

        assertTrue(hasNewForFirst)
        assertFalse(hasNewForSecond)
    }
}
