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
import com.altcraft.sdk.services.SubscribeService
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * SubscribeServiceInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: onCreate() invokes ServiceManager.checkStartForeground(); when it
 *    returns false — the service requests stop.
 *  - test_2: onStartCommand() with a non-STOP action: starts the coroutine
 *    worker and calls closedServiceHandler.
 *
 * Edge:
 *  - test_3: onStartCommand() with STOP_SERVICE_ACTION: does NOT start the
 *    worker; still calls closedServiceHandler.
 */
@RunWith(AndroidJUnit4::class)
class SubscribeServiceInstrumentedTest {

    @get:Rule
    val serviceRule: ServiceTestRule = ServiceTestRule.withTimeout(10, TimeUnit.SECONDS)

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(ServiceManager)
        mockkObject(LaunchFunctions)

        coEvery { ServiceManager.checkStartForeground(any()) } returns true
        every { ServiceManager.closedServiceHandler(any(), any()) } answers { }
        every { LaunchFunctions.startSubscribeCoroutineWorker(any()) } answers { }
    }

    @After
    fun tearDown() {
        unmockkObject(ServiceManager)
        unmockkObject(LaunchFunctions)
        unmockkAll()
    }

    /** test_1: onCreate(): calls checkStartForeground(); when false — service stops itself. */
    @Test
    fun test_1_onCreate_callsCheckStartForeground_andStopsWhenFalse() {
        coEvery { ServiceManager.checkStartForeground(any()) } returns false

        val name: ComponentName =
            context.startService(Intent(context, SubscribeService::class.java))
                ?: error("Service wasn't started")

        coVerify(timeout = 1500, exactly = 1) {
            ServiceManager.checkStartForeground(any())
        }

        context.stopService(Intent().setComponent(name))
    }

    /** test_2: onStartCommand() with non-STOP action -> starts worker + calls closedServiceHandler. */
    @Test
    fun test_2_onStartCommand_nonStopAction_startsWorker_andCallsClosedHandler() {
        val startIntent = Intent(context, SubscribeService::class.java).apply {
            action = "ANY_ACTION"
        }

        val name: ComponentName =
            context.startService(startIntent) ?: error("Service wasn't started")

        verify(timeout = 1500, exactly = 1) {
            LaunchFunctions.startSubscribeCoroutineWorker(any())
        }
        verify(timeout = 1500, exactly = 1) {
            ServiceManager.closedServiceHandler(
                withArg {
                    assertEquals("ANY_ACTION", it.action)
                },
                any()
            )
        }

        context.stopService(Intent().setComponent(name))
    }

    /** test_3: onStartCommand() with STOP_SERVICE_ACTION -> worker not started, closedServiceHandler called. */
    @Test
    fun test_3_onStartCommand_stopAction_noWorker_callsClosedHandler() {
        val stopIntent = Intent(context, SubscribeService::class.java).apply {
            action = STOP_SERVICE_ACTION
        }

        val name: ComponentName =
            context.startService(stopIntent) ?: error("Service wasn't started")

        verify(timeout = 1500, exactly = 0) {
            LaunchFunctions.startSubscribeCoroutineWorker(any())
        }
        verify(timeout = 1500, exactly = 1) {
            ServiceManager.closedServiceHandler(
                withArg {
                    assertEquals(STOP_SERVICE_ACTION, it.action)
                },
                any()
            )
        }

        context.stopService(Intent().setComponent(name))
    }
}