@file:Suppress("SpellCheckingInspection")

package test.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.work.Data
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.extension.MapExtension
import com.altcraft.sdk.push.OpenPushStrategy
import com.altcraft.sdk.push.PushPresenter
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.workers.foreground.Request
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * OpenPushStrategyTest
 *
 * Positive scenarios:
 *  - test_1: openPushStrategy(): when app is foreground -> shows push directly (no worker)
 *  - test_2: openPushStrategy(): when background + non-FCM provider -> starts foreground worker
 *  - test_3: deliveryEventStrategy(): when app is foreground -> sends event directly (no worker)
 *  - test_4: deliveryEventStrategy(): when background + non-FCM provider -> starts foreground worker
 *
 * Edge/Fallback:
 *  - test_5: openPushStrategy(): background + non-FCM but workData == null -> falls back to direct show
 *
 * Notes:
 *  - Pure unit tests; android.util.Log is statically mocked to avoid "not mocked" errors.
 *  - We mock SubFunction/MapExtension/Request/PushPresenter/PushEvent objects.
 *  - To exercise worker path we use a non-FCM provider (_provider = "android-huawei").
 */
class OpenPushStrategyTest {

    private companion object {
        private const val TIMEOUT_MS = 1500L
        private val NON_FCM_MESSAGE = mapOf("_provider" to "android-huawei", UID_KEY to "U-123")
        private val ANY_MESSAGE = mapOf("k" to "v", UID_KEY to "U-1")
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Avoid android.util.Log crashes in JVM unit tests
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        // Mock SDK singletons referenced by OpenPushStrategy
        mockkObject(SubFunction)
        mockkObject(Request)
        mockkObject(PushPresenter)
        mockkObject(PushEvent)
        mockkObject(MapExtension)

        // Safe defaults
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { PushPresenter.showPush(any(), any()) } just Runs
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } just Runs
        every { Request.startPushForegroundWorker(any(), any()) } just Runs
        every { Request.startEventForegroundWorker(any(), any()) } just Runs

        // Default stubs for the extension: return some Data for known messages.
        every {
            MapExtension.run { NON_FCM_MESSAGE.toWorkDataOrNull() }
        } returns Data.Builder().putString("provider", "huawei").build()

        every {
            MapExtension.run { ANY_MESSAGE.toWorkDataOrNull() }
        } returns Data.Builder().putString("x", "y").build()
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Log::class)
    }

    /** test_1: openPushStrategy(): foreground -> show directly, no worker */
    @Test
    fun openPushStrategy_foreground_showsDirectly() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns true

        OpenPushStrategy.openPushStrategy(ctx, ANY_MESSAGE)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushPresenter.showPush(ctx, ANY_MESSAGE) }
        verify(timeout = TIMEOUT_MS, exactly = 0) { Request.startPushForegroundWorker(any(), any()) }
    }

    /** test_2: openPushStrategy(): background + non-FCM -> start worker, no direct show */
    @Test
    fun openPushStrategy_background_usesWorker() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns false
        // workData for NON_FCM_MESSAGE уже задан в setUp()

        OpenPushStrategy.openPushStrategy(ctx, NON_FCM_MESSAGE)

        verify(timeout = TIMEOUT_MS, exactly = 1) { Request.startPushForegroundWorker(ctx, any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushPresenter.showPush(any(), any()) }
    }

    /** test_3: deliveryEventStrategy(): foreground -> send directly, no worker */
    @Test
    fun deliveryEventStrategy_foreground_sendsDirectly() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns true

        OpenPushStrategy.deliveryEventStrategy(ctx, ANY_MESSAGE)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, DELIVERY, ANY_MESSAGE[UID_KEY])
        }
        verify(timeout = TIMEOUT_MS, exactly = 0) { Request.startEventForegroundWorker(any(), any()) }
    }

    /** test_4: deliveryEventStrategy(): background + non-FCM -> start worker, no direct send */
    @Test
    fun deliveryEventStrategy_background_usesWorker() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns false
        // workData для NON_FCM_MESSAGE задан в setUp()

        OpenPushStrategy.deliveryEventStrategy(ctx, NON_FCM_MESSAGE)

        verify(timeout = TIMEOUT_MS, exactly = 1) { Request.startEventForegroundWorker(ctx, any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.sendPushEvent(any(), any(), any()) }
    }

    /** test_5: openPushStrategy(): background + non-FCM but workData == null -> fallback to direct show */
    @Test
    fun openPushStrategy_background_noWorkData_fallsBackToDirect() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns false
        // Переопределяем конкретно для этого сообщения — вернуть null
        every {
            MapExtension.run { NON_FCM_MESSAGE.toWorkDataOrNull() }
        } returns null

        OpenPushStrategy.openPushStrategy(ctx, NON_FCM_MESSAGE)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushPresenter.showPush(ctx, NON_FCM_MESSAGE) }
        verify(timeout = TIMEOUT_MS, exactly = 0) { Request.startPushForegroundWorker(any(), any()) }
    }
}
