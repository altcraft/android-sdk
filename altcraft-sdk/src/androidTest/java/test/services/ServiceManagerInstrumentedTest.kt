package test.services

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// SDK imports
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.PushPresenter
import com.altcraft.sdk.services.SubscribeService
import com.altcraft.sdk.services.UpdateService
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import com.altcraft.sdk.workers.coroutine.Worker

/**
 * ServiceManagerInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: startSubscribeWorker — when preconditions pass, starts foreground SubscribeService.
 *  - test_2: startSubscribeWorker — when preconditions fail, enqueues coroutine worker.
 *  - test_3: startUpdateWorker — when preconditions pass, starts foreground UpdateService.
 *  - test_4: startUpdateWorker — when preconditions fail, enqueues coroutine worker.
 *  - test_5: stopService — when service is running, sends STOP_SERVICE_ACTION to target service.
 *  - test_6: closedServiceHandler — with STOP_SERVICE_ACTION, stops service (stopSelf called).
 *  - test_7: createServiceNotification — returns Notification when channel exists.
 *  - test_8: checkStartForeground — online + valid notification → returns true and calls startForeground.
 *  - test_9: closeService(PUSH_SUBSCRIBE_SERVICE) — resets retry and calls stopService.
 *  - test_10: closeService(TOKEN_UPDATE_SERVICE) — resets retry and calls stopService.
 *  - test_11: checkServiceClosed — when app in background, eventually calls closeService.
 *
 * Negative scenarios:
 *  - test_12: stopService — when service is not running, does nothing (no startService call).
 *  - test_13: createServiceNotification — channel not created → returns null (error path).
 *  - test_14: checkStartForeground — offline → returns false and does not startForeground.
 *
 */
@RunWith(AndroidJUnit4::class)
class ServiceManagerInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var ctx: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        ctx = mockk(relaxed = true)

        MockKAnnotations.init(this, relaxUnitFun = true)

        // Stub common singletons
        mockkObject(SubFunction)
        mockkObject(ConfigSetup)
        mockkObject(PushChannel)
        mockkObject(PushPresenter)
        mockkObject(LaunchFunctions)

        // Defaults (overridden per-test as needed)
        every { SubFunction.isAppInForegrounded() } returns true
        every { SubFunction.isServiceRunning(any(), any()) } returns false
        every { SubFunction.checkingNotificationPermission(any()) } returns true
        every { SubFunction.isOnline(any()) } returns true

        // PushChannel defaults (we won't create real channels)
        every { PushChannel.selectAndCreateChannel(any(), any()) } just Runs
        every { PushChannel.isChannelCreated(any(), any()) } returns true

        // Config default for createServiceNotification path
        coEvery { ConfigSetup.getConfig(any()) } returns ConfigurationEntity(
            id = 0,
            icon = 777, // arbitrary icon id to avoid resource dependency
            apiUrl = "https://api.example.com",
            rToken = "rt",
            appInfo = null,
            usingService = true,
            serviceMessage = "fg service running",
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = "AC",
            pushChannelDescription = "Altcraft channel"
        )

        // getChannelInfo real behavior (call original) unless overridden
        every { PushChannel.getChannelInfo(any(), any()) } answers { callOriginal() }

        // Notification factory used by ServiceManager.createServiceNotification
        val dummyNotification = mockk<Notification>(relaxed = true)
        every {
            PushPresenter.createNotification(any(), any(), any(), any())
        } returns dummyNotification
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --------------------------------------------------------------------------------------------
    // startSubscribeWorker
    // --------------------------------------------------------------------------------------------

    /** Preconditions true ⇒ explicit startService(SubscribeService) is called. */
    @Test
    fun startSubscribeWorker_starts_foreground_service_when_preconditions_pass() {
        val cfg = requireNotNull(runBlocking { ConfigSetup.getConfig(appContext) }).copy(usingService = true)

        val intentSlot = slot<Intent>()
        every { ctx.startService(capture(intentSlot)) } returns ComponentName("pkg", "name")

        ServiceManager.startSubscribeWorker(ctx, cfg)

        val started = intentSlot.captured
        // Explicit intent to SubscribeService
        assertThat(started.component?.className, `is`(SubscribeService::class.java.name))
    }

    /** Preconditions fail (usingService=false) ⇒ enqueues coroutine worker instead of service. */
    @Test
    fun startSubscribeWorker_enqueues_worker_when_preconditions_fail() {
        val cfg = requireNotNull(runBlocking { ConfigSetup.getConfig(appContext) }).copy(usingService = false)

        every { LaunchFunctions.startSubscribeCoroutineWorker(any()) } just Runs

        ServiceManager.startSubscribeWorker(ctx, cfg)

        verify(exactly = 1) { LaunchFunctions.startSubscribeCoroutineWorker(ctx) }
        verify(exactly = 0) { ctx.startService(any()) }
    }

    // --------------------------------------------------------------------------------------------
    // startUpdateWorker
    // --------------------------------------------------------------------------------------------

    /** Preconditions true ⇒ explicit startService(UpdateService) is called. */
    @Test
    fun startUpdateWorker_starts_foreground_service_when_preconditions_pass() {
        val cfg = requireNotNull(runBlocking { ConfigSetup.getConfig(appContext) }).copy(usingService = true)

        val intentSlot = slot<Intent>()
        every { ctx.startService(capture(intentSlot)) } returns ComponentName("pkg", "name")

        ServiceManager.startUpdateWorker(ctx, cfg)

        val started = intentSlot.captured
        assertThat(started.component?.className, `is`(UpdateService::class.java.name))
    }

    /** Preconditions fail ⇒ enqueues coroutine worker instead of service. */
    @Test
    fun startUpdateWorker_enqueues_worker_when_preconditions_fail() {
        val cfg = requireNotNull(runBlocking { ConfigSetup.getConfig(appContext) }).copy(usingService = false)

        every { LaunchFunctions.startUpdateCoroutineWorker(any()) } just Runs

        ServiceManager.startUpdateWorker(ctx, cfg)

        verify(exactly = 1) { LaunchFunctions.startUpdateCoroutineWorker(ctx) }
        verify(exactly = 0) { ctx.startService(any()) }
    }

    // --------------------------------------------------------------------------------------------
    // stopService
    // --------------------------------------------------------------------------------------------

    /** When target service is running ⇒ sends STOP_SERVICE_ACTION intent to that service. */
    @Test
    fun stopService_when_running_sends_stop_action_intent() {
        every { SubFunction.isServiceRunning(ctx, SubscribeService::class.java) } returns true

        val intentSlot = slot<Intent>()
        every { ctx.startService(capture(intentSlot)) } returns ComponentName("pkg", "name")

        ServiceManager.stopService(ctx, SubscribeService::class.java)

        val sent = intentSlot.captured
        assertThat(sent.action, `is`(Constants.STOP_SERVICE_ACTION))
        assertThat(sent.component?.className, `is`(SubscribeService::class.java.name))
    }

    /** When target service is NOT running ⇒ no startService call. */
    @Test
    fun stopService_when_not_running_does_nothing() {
        every { SubFunction.isServiceRunning(ctx, UpdateService::class.java) } returns false

        ServiceManager.stopService(ctx, UpdateService::class.java)

        verify(exactly = 0) { ctx.startService(any()) }
    }

    // --------------------------------------------------------------------------------------------
    // closedServiceHandler
    // --------------------------------------------------------------------------------------------

    /** With STOP_SERVICE_ACTION ⇒ service.stopSelf() is called (foreground stopped internally). */
    @Test
    fun closedServiceHandler_with_stop_action_stops_service() {
        val svc = mockk<Service>(relaxed = true)
        val intent = Intent(Constants.STOP_SERVICE_ACTION)

        ServiceManager.closedServiceHandler(intent, svc)

        verify(exactly = 1) { svc.stopSelf() }
    }

    // --------------------------------------------------------------------------------------------
    // createServiceNotification
    // --------------------------------------------------------------------------------------------

    /** Returns a Notification when channel exists and factory returns non-null. */
    @Test
    fun createServiceNotification_returns_notification_on_success() = runBlocking {
        // Channel exists by default stub; createNotification already stubbed to return dummy notification
        val n = ServiceManager.createServiceNotification(appContext)
        assertThat(n is Notification, `is`(true))
        verify(atLeast = 1) { PushPresenter.createNotification(any(), any(), any(), any()) }
    }

    /** Returns null when channel is reported as NOT created. */
    @Test
    fun createServiceNotification_returns_null_when_channel_missing() = runBlocking {
        every { PushChannel.isChannelCreated(any(), any()) } returns false

        val n = ServiceManager.createServiceNotification(appContext)
        assertThat(n == null, `is`(true))
    }

    // --------------------------------------------------------------------------------------------
    // checkStartForeground
    // --------------------------------------------------------------------------------------------

    /**
     * Online + valid notification: startForeground is invoked (signature depends on API level),
     * and function returns true.
     */
    @Test
    fun checkStartForeground_success_calls_startForeground_and_returns_true() = runBlocking {
        every { SubFunction.isOnline(any()) } returns true
        // Ensure createServiceNotification path is healthy
        every { PushChannel.isChannelCreated(any(), any()) } returns true

        val svc = mockk<Service>(relaxed = true)
        // Stub both overloads to be safe
        every { svc.startForeground(any(), any() as Notification) } just Runs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            every { svc.startForeground(any(), any() as Notification, any()) } just Runs
        }

        val ok = ServiceManager.checkStartForeground(svc)

        assertThat(ok, `is`(true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            verify(exactly = 1) { svc.startForeground(any(), any() as Notification, any()) }
        } else {
            verify(exactly = 1) { svc.startForeground(any(), any() as Notification) }
        }
    }

    /** Offline ⇒ returns false and does not call startForeground. */
    @Test
    fun checkStartForeground_offline_returns_false() = runBlocking {
        every { SubFunction.isOnline(any()) } returns false

        val svc = mockk<Service>(relaxed = true)
        val ok = ServiceManager.checkStartForeground(svc)

        assertThat(ok, `is`(false))
        // Ensure no startForeground calls
        verify(exactly = 0) { svc.startForeground(any(), any() as Notification) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            verify(exactly = 0) { svc.startForeground(any(), any() as Notification, any()) }
        }
    }

    // --------------------------------------------------------------------------------------------
    // closeService
    // --------------------------------------------------------------------------------------------

    /** closeService(PUSH_SUBSCRIBE_SERVICE, delay=false) resets retrySubscribe and calls stopService(SubscribeService). */
    @Test
    fun closeService_subscribe_resets_retry_and_calls_stop() {
        mockkObject(ServiceManager)
        // Call real closeService, stub stopService to capture
        every { ServiceManager.stopService(any(), any()) } just Runs
        every { ServiceManager.closeService(any(), any(), any()) } answers { callOriginal() }

        Worker.retrySubscribe = 5

        val latch = CountDownLatch(1)
        every { ServiceManager.stopService(ctx, any()) } answers {
            latch.countDown()
        }

        ServiceManager.closeService(ctx, PUSH_SUBSCRIBE_SERVICE, delay = false)

        // Wait the launched coroutine
        latch.await(1, TimeUnit.SECONDS)
        assertThat(Worker.retrySubscribe, `is`(0))
    }

    /** closeService(TOKEN_UPDATE_SERVICE, delay=false) resets retryUpdate and calls stopService(UpdateService). */
    @Test
    fun closeService_update_resets_retry_and_calls_stop() {
        mockkObject(ServiceManager)
        every { ServiceManager.stopService(any(), any()) } just Runs
        every { ServiceManager.closeService(any(), any(), any()) } answers { callOriginal() }

        Worker.retryUpdate = 7

        val latch = CountDownLatch(1)
        every { ServiceManager.stopService(ctx, any()) } answers {
            latch.countDown()
        }

        ServiceManager.closeService(ctx, TOKEN_UPDATE_SERVICE, delay = false)

        latch.await(1, TimeUnit.SECONDS)
        assertThat(Worker.retryUpdate, `is`(0))
    }

    // --------------------------------------------------------------------------------------------
    // checkServiceClosed
    // --------------------------------------------------------------------------------------------

    /**
     * When app is in background, checkServiceClosed should schedule a close after ~500ms
     * regardless of count. We await the latch to ensure it happened.
     */
    @Test
    fun checkServiceClosed_in_background_schedules_close() {
        mockkObject(ServiceManager)
        every { ServiceManager.stopService(any(), any()) } just Runs
        every { ServiceManager.closeService(any(), any(), any()) } answers { callOriginal() }

        // Force background
        every { SubFunction.isAppInForegrounded() } returns false

        val latch = CountDownLatch(1)
        every { ServiceManager.stopService(ctx, any()) } answers { latch.countDown() }

        // Count value doesn't matter in background branch
        ServiceManager.checkServiceClosed(ctx, PUSH_SUBSCRIBE_SERVICE, count = 1)

        // Await the delayed close (~500ms). Give it up to 2 seconds.
        val closed = latch.await(2, TimeUnit.SECONDS)
        assertThat(closed, `is`(true))
    }
}
