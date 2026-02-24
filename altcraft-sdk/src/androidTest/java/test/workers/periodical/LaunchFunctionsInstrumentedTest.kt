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
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NAME
import com.altcraft.sdk.data.Constants.PROFILE_UPDATE_P_WORK_NAME
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NAME
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NAME
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_P_WORK_NAME
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.profile.ProfileUpdate
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.LaunchFunctions
import io.mockk.CapturingSlot
import io.mockk.coEvery
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
 *  - test_1: startPeriodicalPushEventWorker() schedules periodic work.
 *  - test_2: startPeriodicalSubscribeWorker() schedules periodic work.
 *  - test_3: startPeriodicalTokenUpdateWorker() schedules periodic work.
 *  - test_4: startPeriodicalMobileEventWorker() schedules periodic work.
 *  - test_5: startPeriodicalProfileUpdateWorker() schedules periodic work.
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
            ProfileUpdate,
            RoomRequest
        )

        every { SubFunction.isAppInForegrounded() } returns false

        ctxSlot = slot()
        coEvery { CommonFunctions.awaitCancel(capture(ctxSlot), any()) } returns Unit

        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { TokenUpdate.pushTokenUpdate(any()) } returns true
        coEvery { MobileEvent.isRetry(any()) } returns false
        coEvery { ProfileUpdate.isRetry(any(), any()) } returns false

        coEvery { RoomRequest.clearOldPushEventsFromRoom(any()) } returns Unit
        coEvery { RoomRequest.clearOldMobileEventsFromRoom(any()) } returns Unit
        coEvery { RoomRequest.clearOldProfileUpdatesFromRoom(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: startPeriodicalPushEventWorker() schedules periodic work. */
    @Test
    fun test_1_startPeriodicalPushEventWorker_runs() {
        LaunchFunctions.startPeriodicalPushEventWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(PUSH_EVENT_P_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }

    /** - test_2: startPeriodicalSubscribeWorker() schedules periodic work. */
    @Test
    fun test_2_startPeriodicalSubscribeWorker_runs() {
        LaunchFunctions.startPeriodicalSubscribeWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(SUB_P_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }

    /** - test_3: startPeriodicalTokenUpdateWorker() schedules periodic work. */
    @Test
    fun test_3_startPeriodicalTokenUpdateWorker_runs() {
        LaunchFunctions.startPeriodicalTokenUpdateWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(TOKEN_UPDATE_P_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }

    /** - test_4: startPeriodicalMobileEventWorker() schedules periodic work. */
    @Test
    fun test_4_startPeriodicalMobileEventWorker_runs() {
        LaunchFunctions.startPeriodicalMobileEventWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(MOBILE_EVENT_P_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }

    /** - test_5: startPeriodicalProfileUpdateWorker() schedules periodic work. */
    @Test
    fun test_5_startPeriodicalProfileUpdateWorker_runs() {
        LaunchFunctions.startPeriodicalProfileUpdateWorker(context)

        val infos = workManager.getWorkInfosForUniqueWork(PROFILE_UPDATE_P_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertTrue(infos.first().state == WorkInfo.State.ENQUEUED)

        val id = infos.first().id
        testDriver!!.setAllConstraintsMet(id)
        testDriver!!.setPeriodDelayMet(id)
    }
}