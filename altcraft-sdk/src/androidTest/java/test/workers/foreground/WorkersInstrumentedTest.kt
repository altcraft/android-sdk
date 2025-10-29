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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.push.PushPresenter
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.foreground.Worker
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WorkersInstrumentedTest
 *
 * Positive scenarios:
 * - test_1: EventForegroundWorker returns Success, requests foreground notification,
 * and calls sendPushEvent(context, DELIVERY, uid).
 * - test_2: PushForegroundWorker returns Success, requests foreground notification,
 * and calls showPush(context, inputMap).
 */
@RunWith(AndroidJUnit4::class)
class WorkersInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockkObject(ServiceManager)
        mockkObject(PushEvent)
        mockkObject(PushPresenter)

        val fakeNotification: Notification =
            NotificationCompat.Builder(context, "test-channel")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("test")
                .setContentText("fg")
                .build()

        coEvery { ServiceManager.createServiceNotification(any()) } returns fakeNotification
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } just Runs
        coEvery { PushPresenter.showPush(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(ServiceManager)
        unmockkObject(PushEvent)
        unmockkObject(PushPresenter)
        unmockkAll()
    }

    /** - test_1: EventForegroundWorker returns Success, sets foreground, and sends DELIVERY with uid. */
    @Test
    fun test_1_EventForegroundWorker_success_and_sendsDelivery() = runBlocking {
        val uid = "uid-123"
        val input = Data.Builder()
            .putString(UID_KEY, uid)
            .build()

        val worker = TestListenableWorkerBuilder<Worker.EventForegroundWorker>(context)
            .setInputData(input)
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)

        val ctxSlot = slot<Context>()
        coVerify(exactly = 1) { ServiceManager.createServiceNotification(capture(ctxSlot)) }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)
        coVerify(exactly = 1) { PushEvent.sendPushEvent(ctxSlot.captured, DELIVERY, uid) }
    }

    /** - test_2: PushForegroundWorker returns Success, sets foreground, and shows push with input payload. */
    @Test
    fun test_2_PushForegroundWorker_success_and_showsPush() = runBlocking {
        val payload = mapOf(
            "title" to "Hello",
            "body" to "World",
            UID_KEY to "u-42"
        )
        val input = Data.Builder().apply { payload.forEach { (k, v) -> putString(k, v) } }.build()

        val worker = TestListenableWorkerBuilder<Worker.PushForegroundWorker>(context)
            .setInputData(input)
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)

        val ctxSlot = slot<Context>()
        coVerify(exactly = 1) { ServiceManager.createServiceNotification(capture(ctxSlot)) }
        assertEquals("Package must match", context.packageName, ctxSlot.captured.packageName)
        coVerify(exactly = 1) {
            PushPresenter.showPush(ctxSlot.captured, withArg { map ->
                assertEquals(payload["title"], map["title"])
                assertEquals(payload["body"], map["body"])
                assertEquals(payload[UID_KEY], map[UID_KEY])
            })
        }
    }
}