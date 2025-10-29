@file:Suppress("SpellCheckingInspection")

package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import com.altcraft.sdk.workers.coroutine.Worker
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LaunchFunctionsInstrumentedTest
 *
 * test_1: startPushEventCoroutineWorker(): enqueues and runs worker -> SUCCEEDED
 * test_2: startSubscribeCoroutineWorker(): enqueues and runs worker -> SUCCEEDED; calls closeService(...)
 * test_3: startUpdateCoroutineWorker(): enqueues and runs worker -> SUCCEEDED; calls closeService(...)
 * test_4: startMobileEventCoroutineWorker(): enqueues and runs worker -> SUCCEEDED
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

        // Mocks
        mockkObject(SubFunction)
        mockkObject(ServiceManager)
        mockkObject(PushEvent)
        mockkObject(PushSubscribe)
        mockkObject(TokenUpdate)
        mockkObject(MobileEvent)

        every { SubFunction.isAppInForegrounded() } returns true
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        coEvery { MobileEvent.isRetry(any()) } returns false

        every { ServiceManager.checkServiceClosed(any(), any(), any()) } just Runs
        every { ServiceManager.closeService(any(), any(), any()) } just Runs

        Worker.retrySubscribe = 0
        Worker.retryUpdate = 0
    }

    @After
    fun tearDown() {
        unmockkObject(
            SubFunction,
            ServiceManager,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent
        )
        unmockkAll()
    }

    /**
     * test_1:
     * Enqueues the PushEvent worker via LaunchFunctions, satisfies constraints,
     * asserts the work finishes with SUCCEEDED, and verifies PushEvent.isRetry() is called once.
     */
    @Test
    fun test_1_startPushEventCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startPushEventCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(PUSH_EVENT_C_WORK_TAG).get()
        assertEquals("Exactly one PushEvent work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, finished?.state)

        coVerify(exactly = 1) { PushEvent.isRetry(any()) }
    }

    /**
     * test_2:
     * Enqueues the Subscribe worker, satisfies constraints, asserts SUCCEEDED,
     * verifies ServiceManager.closeService() is invoked for PUSH_SUBSCRIBE_SERVICE,
     * and verifies PushSubscribe.isRetry() is called once.
     */
    @Test
    fun test_2_startSubscribeCoroutineWorker_runsAndClosesService() {
        LaunchFunctions.startSubscribeCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(SUBSCRIBE_C_WORK_TAG).get()
        assertEquals("Exactly one Subscribe work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, finished?.state)

        val ctxSlot = slot<Context>()
        verify(atLeast = 1) {
            ServiceManager.closeService(capture(ctxSlot), PUSH_SUBSCRIBE_SERVICE, true)
        }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)

        coVerify(exactly = 1) { PushSubscribe.isRetry(any()) }
    }

    /**
     * test_3:
     * Enqueues the Update worker, satisfies constraints, asserts SUCCEEDED,
     * verifies ServiceManager.closeService() is invoked for TOKEN_UPDATE_SERVICE,
     * and verifies TokenUpdate.isRetry() is called once with any uid.
     */
    @Test
    fun test_3_startUpdateCoroutineWorker_runsAndClosesService() {
        LaunchFunctions.startUpdateCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(UPDATE_C_WORK_TAG).get()
        assertEquals("Exactly one Update work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, finished?.state)

        val ctxSlot = slot<Context>()
        verify(atLeast = 1) {
            ServiceManager.closeService(capture(ctxSlot), TOKEN_UPDATE_SERVICE, true)
        }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)

        coVerify(exactly = 1) { TokenUpdate.isRetry(any(), any()) }
    }

    /**
     * test_4:
     * Enqueues the MobileEvent worker via LaunchFunctions, satisfies constraints,
     * asserts SUCCEEDED, and verifies MobileEvent.isRetry() is called once.
     */
    @Test
    fun test_4_startMobileEventCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startMobileEventCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(MOBILE_EVENT_C_WORK_TAG).get()
        assertEquals("Exactly one MobileEvent work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, finished?.state)

        coVerify(exactly = 1) { MobileEvent.isRetry(any()) }
    }
}