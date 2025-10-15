@file:Suppress("SpellCheckingInspection")

package test.core

//  Created by Andrey Pogodin.
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.altcraft.sdk.concurrency.ForegroundGate
import com.altcraft.sdk.core.Retry
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.coroutine.Worker
import com.altcraft.sdk.workers.periodical.CommonFunctions
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RetryTest
 *
 * Verifies Retry.performRetryOperations() flow including mobile and push branches.
 *
 * Positive scenarios:
 *  - test_1: pushModuleIsActive() returns true when manual token exists.
 *  - test_2: performRetryOperations() starts ALL background tasks once on foreground=true
 *            with push module active:
 *              * mobileEventPeriodicalWorkerControl + MobileEvent.isRetry (always)
 *              * pushPeriodicalWorkerControl + PushSubscribe.isRetry + PushEvent.isRetry + TokenUpdate.tokenUpdate
 *  - test_3: performRetryOperations() starts ONLY mobile background tasks if push module inactive.
 *
 * Negative scenarios:
 *  - test_4: pushModuleIsActive() returns false when no manual token and no providers.
 *  - test_5: performRetryOperations() does nothing on foreground=false.
 *  - test_6: performRetryOperations() is idempotent — second call does not start tasks again.
 *
 * Notes:
 *  - We mock Looper.getMainLooper() and Handler constructor BEFORE touching ForegroundGate
 *    to avoid ExceptionInInitializerError from static init.
 *  - ForegroundGate.foregroundCallback has signature: (callback: (Boolean) -> Unit) -> Unit.
 *    In tests, we invoke the captured callback with true/false explicitly.
 *  - android.util.Log is statically mocked for JVM tests.
 *  - Retry.retryControl is reset before each test to ensure idempotence across tests.
 *  - Worker.retryUpdate / Worker.retrySubscribe are accessed and reset implicitly by the code path;
 *    we don't assert their values here, only side-effects/calls.
 */
class RetryTest {

    private companion object {
        private const val TIMEOUT_MS = 800L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Reset idempotence guard for each test.
        Retry.retryControl.set(false)

        // IMPORTANT: mock main Looper and Handler BEFORE ForegroundGate usage.
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        // Make Handler.post(...) run the Runnable immediately.
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        // Silence android.util.Log in unit env.
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        // Mock SDK singletons/objects.
        mockkObject(ForegroundGate)
        mockkObject(CommonFunctions)
        mockkObject(MobileEvent)
        mockkObject(PushSubscribe)
        mockkObject(PushEvent)
        mockkObject(TokenUpdate)
        mockkObject(Preferenses)
        mockkObject(TokenManager)
        mockkObject(Worker)

        // Default stubs: mobile & push tasks succeed.
        coEvery { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) } just Runs
        coEvery { MobileEvent.isRetry(any()) } returns false

        coEvery { CommonFunctions.pushPeriodicalWorkerControl(any()) } just Runs
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { TokenUpdate.tokenUpdate(any()) } just Runs

        // Module inactive by default unless a test overrides.
        every { Preferenses.getManualToken(any()) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        // Default ForegroundGate: no-op; tests will override to fire true/false.
        every { ForegroundGate.foregroundCallback(any()) } answers { /* no-op */ }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ---------------------------
    // pushModuleIsActive()
    // ---------------------------

    /** Returns true when manual token exists. */
    @Test
    fun `pushModuleIsActive returns true when manual token exists`() {
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(
            provider = FCM_PROVIDER,
            token = "manual-token-123"
        )
        assertTrue(Retry.pushModuleIsActive(ctx))
    }

    /** Returns false when no manual token and no providers. */
    @Test
    fun `pushModuleIsActive returns false when no token and no providers`() {
        every { Preferenses.getManualToken(ctx) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null
        assertFalse(Retry.pushModuleIsActive(ctx))
    }

    // ---------------------------
    // performRetryOperations()
    // ---------------------------

    /**
     * Starts ALL background tasks once when foreground=true and push module is active.
     * Verifies mobile + push branches are triggered exactly once.
     */
    @Test
    fun `performRetryOperations starts mobile and push tasks when foreground true and push active`() = runBlocking {
        // Foreground = true
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        // Make push module active
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(FCM_PROVIDER, "tkn")

        Retry.performRetryOperations(ctx)

        // Mobile branch: always
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { MobileEvent.isRetry(ctx) }

        // Push branch: only when active
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.pushPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /**
     * Starts ONLY mobile background tasks when foreground=true and push module is inactive.
     * Verifies that push tasks are NOT called.
     */
    @Test
    fun `performRetryOperations starts only mobile tasks when push inactive`() = runBlocking {
        // Foreground = true
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        // Ensure push module inactive
        every { Preferenses.getManualToken(ctx) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        Retry.performRetryOperations(ctx)

        // Mobile branch should run
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { MobileEvent.isRetry(ctx) }

        // Push branch should NOT run
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** Does nothing when foreground=false (neither mobile nor push). */
    @Test
    fun `performRetryOperations does nothing when foreground false`() = runBlocking {
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(false)
        }

        // Clear previous interactions
        clearMocks(
            CommonFunctions, MobileEvent, PushSubscribe, PushEvent, TokenUpdate,
            answers = false, recordedCalls = true
        )

        Retry.performRetryOperations(ctx)

        // Mobile
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { MobileEvent.isRetry(any()) }

        // Push
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /**
     * Idempotence: the second call must be no-op due to retryControl guard.
     * We first trigger foreground=true and (optionally) active push,
     * then ensure no additional calls happen on the second invocation.
     */
    @Test
    fun `performRetryOperations is idempotent second call is no-op`() = runBlocking {
        // First call: foreground = true, push active
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(FCM_PROVIDER, "tkn")

        Retry.performRetryOperations(ctx)

        // Verify at least once on first call
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { MobileEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { CommonFunctions.pushPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { TokenUpdate.tokenUpdate(ctx) }

        // Clear recorded calls; keep stubs
        clearMocks(
            CommonFunctions, MobileEvent, PushSubscribe, PushEvent, TokenUpdate,
            answers = false, recordedCalls = true
        )

        // Second call — must be NO-OP
        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { MobileEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }
}
