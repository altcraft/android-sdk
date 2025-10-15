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
 * PeriodicalLaunchFunctionsE2EInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: startPeriodicalPushEventWorker() — schedules periodic work; run once and verify:
 *             PushEvent.isRetry(...) and RoomRequest.clearOldPushEventsFromRoom(...) are called.
 *  - test_2: startPeriodicalSubscribeWorker() — schedules periodic work; run once and verify:
 *             PushSubscribe.isRetry(...) is called.
 *  - test_3: startPeriodicalUpdateWorker() — schedules periodic work; run once and verify:
 *             TokenUpdate.tokenUpdate(...) is called.
 *  - test_4: startPeriodicalMobileEventWorker() — schedules periodic work; run once and verify:
 *             MobileEvent.isRetry(...) and RoomRequest.clearOldMobileEventsFromRoom(...) are called.
 */
@RunWith(AndroidJUnit4::class)
class PeriodicalLaunchFunctionsE2EInstrumentedTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private var testDriver: TestDriver? = null

    private lateinit var ctxSlot: CapturingSlot<Context>

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Real WorkManager in test mode with synchronous executor
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver = WorkManagerTestInitHelper.getTestDriver(context)

        // Mock collaborators used inside workers
        mockkObject(
            SubFunction,
            CommonFunctions,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent,
            RoomRequest
        )

        // Workers execute their body only when app is NOT foregrounded.
        every { SubFunction.isAppInForegrounded() } returns false

        // Do not actually cancel anything; just let awaitCancel return immediately.
        ctxSlot = slot()
        coEvery { CommonFunctions.awaitCancel(capture(ctxSlot), any()) } returns Unit

        // Business operations inside workers — make them no-op/success
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { RoomRequest.clearOldPushEventsFromRoom(any()) } returns Unit

        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { TokenUpdate.tokenUpdate(any()) } returns Unit

        coEvery { MobileEvent.isRetry(any()) } returns false
        coEvery { RoomRequest.clearOldMobileEventsFromRoom(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: EVENT periodic worker runs and invokes PushEvent + cleanup */
    @Test
    fun test_1_startPeriodicalPushEventWorker_runs() {
        // Enqueue periodic worker via LaunchFunctions API
        LaunchFunctions.startPeriodicalPushEventWorker(context)

        // Ensure unique work exists
        val infos = workManager.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        // Trigger constraints + period to run the worker once
        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        // Verify business effects happened inside the worker
        coVerify(exactly = 1) { PushEvent.isRetry(any()) }
        coVerify(exactly = 1) { RoomRequest.clearOldPushEventsFromRoom(any()) }
        // awaitCancel must be used in this worker path
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
        // Context sanity: same package as test app
        assertEquals(context.packageName, ctxSlot.captured.packageName)
    }

    /** test_2: SUBSCRIBE periodic worker runs and invokes PushSubscribe.isRetry(...) */
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

    /** test_3: UPDATE periodic worker runs and invokes TokenUpdate.tokenUpdate(...) */
    @Test
    fun test_3_startPeriodicalUpdateWorker_runs() {
        LaunchFunctions.startPeriodicalUpdateWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(UPDATE_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        coVerify(exactly = 1) { TokenUpdate.tokenUpdate(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
    }

    /** test_4: MOBILE EVENT periodic worker runs and invokes MobileEvent + cleanup */
    @Test
    fun test_4_startPeriodicalMobileEventWorker_runs() {
        // Enqueue periodic worker via LaunchFunctions API
        LaunchFunctions.startPeriodicalMobileEventWorker(context)

        // Ensure unique work exists
        val infos = workManager.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NANE).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        // Trigger constraints + period to run the worker once
        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)

        // Verify business effects happened inside the worker
        coVerify(exactly = 1) { MobileEvent.isRetry(any()) }
        coVerify(exactly = 1) { RoomRequest.clearOldMobileEventsFromRoom(any()) }
        coVerify(atLeast = 1) { CommonFunctions.awaitCancel(any(), any()) }
        assertEquals(context.packageName, ctxSlot.captured.packageName)
    }
}
