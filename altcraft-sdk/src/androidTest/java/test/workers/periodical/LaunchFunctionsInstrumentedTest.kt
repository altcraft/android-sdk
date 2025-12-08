@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

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
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.LaunchFunctions
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
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
 *  - test_1: startPeriodicalPushEventWorker() schedules periodic work and invokes PushEvent & cleanup.
 *  - test_2: startPeriodicalSubscribeWorker() schedules periodic work and invokes PushSubscribe.
 *  - test_3: startPeriodicalUpdateWorker() schedules periodic work and invokes TokenUpdate.
 *  - test_4: startPeriodicalMobileEventWorker() schedules periodic work and invokes MobileEvent & cleanup.
 */
@RunWith(AndroidJUnit4::class)
class PeriodicalLaunchFunctionsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private var testDriver: TestDriver? = null
    private lateinit var ctxSlot: CapturingSlot<Context>

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
            CommonFunctions,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent,
            RoomRequest
        )

        every { SubFunction.isAppInForegrounded() } returns false

        ctxSlot = slot()
        coEvery { CommonFunctions.awaitCancel(capture(ctxSlot), any()) } returns Unit

        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { RoomRequest.clearOldPushEventsFromRoom(any()) } returns Unit

        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { TokenUpdate.pushTokenUpdate(any()) } returns Unit

        coEvery { MobileEvent.isRetry(any()) } returns false
        coEvery { RoomRequest.clearOldMobileEventsFromRoom(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: startPeriodicalPushEventWorker() runs and invokes PushEvent + cleanup. */
    @Test
    fun test_1_startPeriodicalPushEventWorker_runs() {
        LaunchFunctions.startPeriodicalPushEventWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        coVerify(exactly = 1) { PushEvent.isRetry(any()) }
        coVerify(exactly = 1) { RoomRequest.clearOldPushEventsFromRoom(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
        assertEquals(context.packageName, ctxSlot.captured.packageName)
    }

    /** - test_2: startPeriodicalSubscribeWorker() runs and invokes PushSubscribe.isRetry(...). */
    @Test
    fun test_2_startPeriodicalSubscribeWorker_runs() {
        LaunchFunctions.startPeriodicalSubscribeWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(SUB_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        coVerify(exactly = 1) { PushSubscribe.isRetry(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
    }

    /** - test_3: startPeriodicalUpdateWorker() runs and invokes TokenUpdate.tokenUpdate(...). */
    @Test
    fun test_3_startPeriodicalUpdateWorker_runs() {
        LaunchFunctions.startPeriodicalUpdateWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        coVerify(exactly = 1) { TokenUpdate.pushTokenUpdate(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
    }

    /** - test_4: startPeriodicalMobileEventWorker() runs and invokes MobileEvent + cleanup. */
    @Test
    fun test_4_startPeriodicalMobileEventWorker_runs() {
        LaunchFunctions.startPeriodicalMobileEventWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        coVerify(exactly = 1) { MobileEvent.isRetry(any()) }
        coVerify(exactly = 1) { RoomRequest.clearOldMobileEventsFromRoom(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
        assertEquals(context.packageName, ctxSlot.captured.packageName)
    }
}