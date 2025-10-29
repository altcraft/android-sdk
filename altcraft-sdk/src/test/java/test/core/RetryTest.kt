@file:Suppress("SpellCheckingInspection")

package test.core

//  Created by Andrey Pogodin.
//
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
 * Covers performRetryOperations() and pushModuleIsActive() logic.
 *
 * Positive scenarios:
 *  - test_1: pushModuleIsActive() returns true when manual token exists.
 *  - test_2: performRetryOperations() starts ALL background tasks once when foreground=true and push active.
 *  - test_3: performRetryOperations() starts ONLY mobile tasks when push inactive.
 *
 * Negative scenarios:
 *  - test_4: pushModuleIsActive() returns false when no manual token and no providers.
 *  - test_5: performRetryOperations() does nothing when foreground=false.
 *  - test_6: performRetryOperations() is idempotent — second call does not start tasks again.
 */
class RetryTest {

    private companion object {
        private const val TIMEOUT_MS = 800L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        Retry.retryControl.set(false)

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        mockkObject(ForegroundGate)
        mockkObject(CommonFunctions)
        mockkObject(MobileEvent)
        mockkObject(PushSubscribe)
        mockkObject(PushEvent)
        mockkObject(TokenUpdate)
        mockkObject(Preferenses)
        mockkObject(TokenManager)
        mockkObject(Worker)

        coEvery { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) } just Runs
        coEvery { MobileEvent.isRetry(any()) } returns false
        coEvery { CommonFunctions.pushPeriodicalWorkerControl(any()) } just Runs
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { TokenUpdate.tokenUpdate(any()) } just Runs

        every { Preferenses.getManualToken(any()) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        every { ForegroundGate.foregroundCallback(any()) } answers { }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: pushModuleIsActive() returns true when manual token exists. */
    @Test
    fun `pushModuleIsActive returns true when manual token exists`() {
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(
            provider = FCM_PROVIDER,
            token = "manual-token-123"
        )
        assertTrue(Retry.pushModuleIsActive(ctx))
    }

    /** - test_2: performRetryOperations() starts ALL background tasks once when foreground=true and push active. */
    @Test
    fun `performRetryOperations starts mobile and push tasks when foreground true and push active`() = runBlocking {
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(FCM_PROVIDER, "tkn")

        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { MobileEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.pushPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /** - test_3: performRetryOperations() starts ONLY mobile tasks when push inactive. */
    @Test
    fun `performRetryOperations starts only mobile tasks when push inactive`() = runBlocking {
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        every { Preferenses.getManualToken(ctx) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null

        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { MobileEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** - test_4: pushModuleIsActive() returns false when no manual token and no providers. */
    @Test
    fun `pushModuleIsActive returns false when no token and no providers`() {
        every { Preferenses.getManualToken(ctx) } returns null
        every { TokenManager.fcmProvider } returns null
        every { TokenManager.hmsProvider } returns null
        every { TokenManager.rustoreProvider } returns null
        assertFalse(Retry.pushModuleIsActive(ctx))
    }

    /** - test_5: performRetryOperations() does nothing when foreground=false. */
    @Test
    fun `performRetryOperations does nothing when foreground false`() = runBlocking {
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(false)
        }

        clearMocks(
            CommonFunctions, MobileEvent, PushSubscribe, PushEvent, TokenUpdate,
            answers = false, recordedCalls = true
        )

        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { MobileEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** - test_6: performRetryOperations() is idempotent — second call does not start tasks again. */
    @Test
    fun `performRetryOperations is idempotent second call is no-op`() = runBlocking {
        every { ForegroundGate.foregroundCallback(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }
        every { Preferenses.getManualToken(ctx) } returns DataClasses.TokenData(FCM_PROVIDER, "tkn")

        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { CommonFunctions.mobileEventPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { MobileEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { CommonFunctions.pushPeriodicalWorkerControl(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { PushEvent.isRetry(ctx) }
        coVerify(timeout = TIMEOUT_MS, atLeast = 1) { TokenUpdate.tokenUpdate(ctx) }

        clearMocks(
            CommonFunctions, MobileEvent, PushSubscribe, PushEvent, TokenUpdate,
            answers = false, recordedCalls = true
        )

        Retry.performRetryOperations(ctx)

        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.mobileEventPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { MobileEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { CommonFunctions.pushPeriodicalWorkerControl(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(timeout = TIMEOUT_MS, exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }
}