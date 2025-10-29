@file:Suppress("SpellCheckingInspection")

package test.workers.foreground

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.push.PushPresenter
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.foreground.Request
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ForegroundRequestInstrumentedTest
 *
 * test_1: startEventForegroundWorker() enqueues a real worker; worker requests foreground info
 * and calls sendPushEvent(...)
 * test_2: startPushForegroundWorker() enqueues a real worker; worker requests foreground info
 * and calls showPush(...)
 */
@RunWith(AndroidJUnit4::class)
class ForegroundRequestInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        mockkObject(ServiceManager)
        mockkObject(PushEvent)
        mockkObject(PushPresenter)

        val fakeNotification: Notification =
            NotificationCompat.Builder(context, "test-channel")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentText("fg")
                .build()

        coEvery { ServiceManager.createServiceNotification(any()) } returns fakeNotification
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } returns Unit
        coEvery { PushPresenter.showPush(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(ServiceManager)
        unmockkObject(PushEvent)
        unmockkObject(PushPresenter)
        unmockkAll()
    }

    /** test_1: verifies EventForegroundWorker gets foreground info and sends delivery event with given UID */
    @Test
    fun test_1_startEventForegroundWorker_enqueuesAndSendsDelivery() {
        val uid = "uid-123"
        val data = Data.Builder().putString(UID_KEY, uid).build()

        Request.startEventForegroundWorker(context, data)

        val ctxSlot = slot<Context>()
        coVerify(exactly = 1) { ServiceManager.createServiceNotification(capture(ctxSlot)) }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)

        coVerify(exactly = 1) { PushEvent.sendPushEvent(ctxSlot.captured, DELIVERY, uid) }
    }

    /** test_2: verifies PushForegroundWorker gets foreground info and shows push with provided data */
    @Test
    fun test_2_startPushForegroundWorker_enqueuesAndShowsPush() {
        val payload = mapOf("title" to "Hello", "body" to "World", UID_KEY to "u-42")
        val data = Data.Builder().apply { payload.forEach { (k, v) -> putString(k, v) } }.build()

        Request.startPushForegroundWorker(context, data)

        val ctxSlot = slot<Context>()
        coVerify(exactly = 1) { ServiceManager.createServiceNotification(capture(ctxSlot)) }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)

        coVerify(exactly = 1) {
            PushPresenter.showPush(
                ctxSlot.captured,
                withArg { passed ->
                    assertEquals(payload["title"], passed["title"])
                    assertEquals(payload["body"], passed["body"])
                    assertEquals(payload[UID_KEY], passed[UID_KEY])
                }
            )
        }
    }
}
