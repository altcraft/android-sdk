@file:Suppress("SpellCheckingInspection")

package test.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.altcraft.sdk.concurrency.ForegroundGate
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.push.Core
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.periodical.CommonFunctions
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CoreTest
 *
 * Positive scenarios:
 *  - test_1: pushModuleIsActive() returns true when manual token exists
 *  - test_2: performPushModuleCheck() starts all background tasks once on foreground=true
 *
 * Negative scenarios:
 *  - test_3: pushModuleIsActive() returns false when no manual token and no providers
 *  - test_4: performPushModuleCheck() does nothing on foreground=false
 *  - test_5: performPushModuleCheck() is idempotent (second call does not start tasks again)
 *
 * Notes:
 *  - We mock Looper.getMainLooper() and Handler(...) BEFORE touching ForegroundGate to avoid
 *    ExceptionInInitializerError in its static initializer.
 *  - ForegroundGate.foregroundCallback is mocked to synchronously invoke the callback.
 *  - android.util.Log is statically mocked to avoid "Method d in android.util.Log not mocked".
 *  - Core.pushControl is reset before each test to ensure independence.
 */
class CoreTest {

    private companion object {
        private const val TIMEOUT_MS = 600L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Reset idempotence guard.
        Core.pushControl.set(false)

        // --- CRUCIAL: mock Android main Looper and Handler constructor BEFORE touching ForegroundGate.
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        // Make Handler.post(...) run the Runnable immediately and return true.
        every { anyConstructed<Handler>().post(any()) } answers {
            val r = firstArg<Runnable>(); r.run(); true
        }

        // Log stubs to silence android.util.Log in unit env.
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        // SDK singletons/objects referenced by Core.
        mockkObject(ForegroundGate)
        mockkObject(CommonFunctions)
        mockkObject(PushSubscribe)
        mockkObject(PushEvent)
        mockkObject(TokenUpdate)
        mockkObject(Preferenses)
        mockkObject(TokenManager)

        // Background tasks — do nothing but succeed.
        coEvery { CommonFunctions.periodicalWorkerControl(any()) } just Runs
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { TokenUpdate.tokenUpdate(any()) } just Runs

        // Default: module inactive unless overridden.
        every { Preferenses.getManualToken(any()) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        // Default: no-op; specific tests override to invoke with true/false.
        every { ForegroundGate.foregroundCallback(any()) } answers { /* no-op */ }
    }

    @After
    fun tearDown() {
        unmockkAll()
        // Explicit unmock is safe even after unmockkAll()
        unmockkStatic(Looper::class)
        unmockkStatic(Log::class)
    }

    /** - test_1: pushModuleIsActive() returns true when manual token exists */
    @Test
    fun test_pushModuleIsActive_returnsTrue_whenManualTokenExists() {
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(
            provider = FCM_PROVIDER,
            token = "manual-token-123"
        )
        val active = Core.pushModuleIsActive(ctx)
        assertTrue(active)
    }

    /** - test_3: pushModuleIsActive() returns false when no manual token and no providers */
    @Test
    fun test_pushModuleIsActive_returnsFalse_whenNoTokenAndNoProviders() {
        every { Preferenses.getManualToken(ctx) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        val active = Core.pushModuleIsActive(ctx)
        assertFalse(active)
    }

    /** - test_2: performPushModuleCheck() starts all background tasks once on foreground=true */
    @Test
    fun test_performPushModuleCheck_startsAllTasks_onForegroundTrue() = runBlocking {
        val cb = slot<(Boolean) -> Unit>()
        every { ForegroundGate.foregroundCallback(capture(cb)) } answers { cb.captured.invoke(true) }

        Core.performPushModuleCheck(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.periodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /** - test_4: performPushModuleCheck() does nothing on foreground=false */
    @Test
    fun test_performPushModuleCheck_doesNothing_onForegroundFalse() = runBlocking {
        val cb = slot<(Boolean) -> Unit>()
        every { ForegroundGate.foregroundCallback(capture(cb)) } answers { cb.captured.invoke(false) }

        Core.performPushModuleCheck(ctx)

        coVerify(
            timeout = TIMEOUT_MS,
            exactly = 0
        ) { CommonFunctions.periodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** - test_5: performPushModuleCheck() is idempotent (second call does not start tasks again) */
    @Test
    fun test_performPushModuleCheck_isIdempotent_secondCallNoop() = runBlocking {
        val cb = slot<(Boolean) -> Unit>()
        every { ForegroundGate.foregroundCallback(capture(cb)) } answers { cb.captured.invoke(true) }

        // First call
        Core.performPushModuleCheck(ctx)
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { CommonFunctions.periodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { TokenUpdate.tokenUpdate(ctx) }

        // Clear recorded calls; keep stubs
        clearMocks(
            CommonFunctions,
            PushSubscribe,
            PushEvent,
            TokenUpdate,
            answers = false,
            recordedCalls = true
        )

        // Second call must be a no-op due to pushControl guard
        Core.performPushModuleCheck(ctx)

        coVerify(
            timeout = TIMEOUT_MS,
            exactly = 0
        ) { CommonFunctions.periodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }
}
