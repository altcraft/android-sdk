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
import com.altcraft.sdk.data.Constants.MOB_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LaunchFunctionsInstrumentedTest
 *
 * test_1: startPushEventCoroutineWorker(): enqueues worker and it moves to RUNNING.
 * test_2: startSubscribeCoroutineWorker(): enqueues worker and it moves to RUNNING.
 * test_3: startUpdateCoroutineWorker(): enqueues worker and it moves to RUNNING.
 * test_4: startMobileEventCoroutineWorker(): enqueues worker and it moves to RUNNING.
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

        mockkObject(SubFunction)
        mockkObject(PushEvent)
        mockkObject(PushSubscribe)
        mockkObject(TokenUpdate)
        mockkObject(MobileEvent)

        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { PushSubscribe.isRetry(any(), any()) } returns false
        coEvery { TokenUpdate.isRetry(any(), any()) } returns false
        coEvery { MobileEvent.isRetry(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkObject(
            SubFunction,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent
        )
        unmockkAll()
    }

    /** test_1: startPushEventCoroutineWorker() enqueues worker and it moves to RUNNING. */
    @Test
    fun test_1_startPushEventCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startPushEventCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(PUSH_EVENT_C_WORK_TAG).get()
        assertEquals("Exactly one PushEvent work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.RUNNING, finished?.state)
    }

    /** test_2: startSubscribeCoroutineWorker() enqueues worker and it moves to RUNNING. */
    @Test
    fun test_2_startSubscribeCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startSubscribeCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(SUBSCRIBE_C_WORK_TAG).get()
        assertEquals("Exactly one Subscribe work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.RUNNING, finished?.state)
    }

    /** test_3: startUpdateCoroutineWorker() enqueues worker and it moves to RUNNING. */
    @Test
    fun test_3_startUpdateCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startUpdateCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(UPDATE_C_WORK_TAG).get()
        assertEquals("Exactly one Update work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.RUNNING, finished?.state)
    }

    /** test_4: startMobileEventCoroutineWorker() enqueues worker and it moves to RUNNING. */
    @Test
    fun test_4_startMobileEventCoroutineWorker_runsAndSucceeds() {
        LaunchFunctions.startMobileEventCoroutineWorker(context)

        val infos = workManager.getWorkInfosByTag(MOB_EVENT_C_WORK_TAG).get()
        assertEquals("Exactly one MobileEvent work expected", 1, infos.size)
        val info = infos.first()
        testDriver!!.setAllConstraintsMet(info.id)

        val finished = workManager.getWorkInfoById(info.id).get()
        assertEquals(WorkInfo.State.RUNNING, finished?.state)
    }
}