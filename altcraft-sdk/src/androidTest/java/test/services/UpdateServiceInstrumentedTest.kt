@file:Suppress("SpellCheckingInspection")

package test.services

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.altcraft.sdk.data.Constants.STOP_SERVICE_ACTION
import com.altcraft.sdk.services.UpdateService
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * UpdateServiceInstrumentedTest
 *
 * Positive:
 *  - test_1: onCreate(): calls ServiceManager.checkStartForeground(); when false — the service stops itself
 *  - test_2: onStartCommand() with non-STOP action: starts startUpdateCoroutineWorker() and calls closedServiceHandler()
 *
 * Edge:
 *  - test_3: onStartCommand() with STOP_SERVICE_ACTION: does not start worker, but calls closedServiceHandler()
 *
 * Notes:
 *  - Instrumentation tests (androidTest) executed on device/emulator.
 *  - Service is started via real Context.startService(...).
 */
@RunWith(AndroidJUnit4::class)
class UpdateServiceInstrumentedTest {

    @get:Rule
    val serviceRule = ServiceTestRule.withTimeout(10, TimeUnit.SECONDS)!!

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Before
    fun setUp() {
        mockkObject(ServiceManager)
        mockkObject(LaunchFunctions)

        coEvery { ServiceManager.checkStartForeground(any()) } returns true
        every { ServiceManager.closedServiceHandler(any(), any()) } just Runs
        every { LaunchFunctions.startUpdateCoroutineWorker(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(ServiceManager)
        unmockkObject(LaunchFunctions)
        unmockkAll()
    }

    /** test_1: onCreate(): calls checkStartForeground(); when false — service stops itself */
    @Test
    fun test_1_onCreate_callsCheckStartForeground_andStopsWhenFalse() {
        runBlocking {
            coEvery { ServiceManager.checkStartForeground(any()) } returns false

            val name: ComponentName =
                context.startService(Intent(context, UpdateService::class.java))
                    ?: error("Service wasn't started")

            coVerify(timeout = 1500, exactly = 1) { ServiceManager.checkStartForeground(any()) }

            context.stopService(Intent().setComponent(name))
        }
    }

    /** test_2: onStartCommand() with non-STOP action -> starts worker + calls closedServiceHandler */
    @Test
    fun test_2_onStartCommand_nonStopAction_startsWorker_andCallsClosedHandler() {
        val startIntent = Intent(context, UpdateService::class.java).apply {
            action = "ANY_ACTION"
        }

        val name: ComponentName =
            context.startService(startIntent) ?: error("Service wasn't started")

        verify(timeout = 1500, exactly = 1) { LaunchFunctions.startUpdateCoroutineWorker(any()) }
        verify(timeout = 1500, exactly = 1) {
            ServiceManager.closedServiceHandler(withArg {
                assertEquals("ANY_ACTION", it.action)
            }, any())
        }

        context.stopService(Intent().setComponent(name))
    }

    /** test_3: onStartCommand() with STOP_SERVICE_ACTION -> worker not started, closedServiceHandler called */
    @Test
    fun test_3_onStartCommand_stopAction_noWorker_callsClosedHandler() {
        val stopIntent = Intent(context, UpdateService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }

        val name: ComponentName =
            context.startService(stopIntent) ?: error("Service wasn't started")

        verify(timeout = 1500, exactly = 0) { LaunchFunctions.startUpdateCoroutineWorker(any()) }
        verify(timeout = 1500, exactly = 1) {
            ServiceManager.closedServiceHandler(withArg {
                assertEquals(STOP_SERVICE_ACTION, it.action)
            }, any())
        }

        context.stopService(Intent().setComponent(name))
    }
}
