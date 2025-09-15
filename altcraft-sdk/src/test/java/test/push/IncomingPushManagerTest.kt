@file:Suppress("SpellCheckingInspection")

package test.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.push.IncomingPushManager
import com.altcraft.sdk.push.OpenPushStrategy
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * IncomingPushManagerTest
 *
 * - test_1: handlePush() with Altcraft message calls deliveryEventStrategy() and reaches fallback receiver
 * - test_2: Altcraft message with no custom receivers -> falls back to default AltcraftSDK.PushReceiver
 * - test_3: Non-Altcraft message -> no deliveryEventStrategy, no recipients/fallback
 *
 * Notes:
 * - Pure unit tests (no instrumentation).
 * - android.util.Log is statically mocked to avoid "not mocked" runtime errors.
 * - Private getAllRecipient(...) is stubbed via spy + `anyArguments` (no fragile matchers).
 */
class IncomingPushManagerTest {

    private companion object {
        private const val TIMEOUT_MS = 2000L // give IO coroutine time to run
        private val ALT_MESSAGE = mapOf("_ac_push" to "1", "_uid" to "U1")
        private val NON_ALT_MESSAGE = mapOf("some" to "value")
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Prevent android.util.Log crashes in local unit environment.
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        // SDK singletons.
        mockkObject(SubFunction)
        mockkObject(OpenPushStrategy)
        mockkObject(ConfigSetup)

        // Defaults for all tests.
        every { SubFunction.altcraftPush(any()) } answers {
            firstArg<Map<String, String>>().containsKey("_ac_push")
        }
        every { OpenPushStrategy.deliveryEventStrategy(any(), any()) } just Runs
        coEvery { ConfigSetup.getConfig(any()) } returns null // no custom receivers -> fallback by default
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Log::class)
    }

    /** - test_1: Altcraft message -> deliveryEventStrategy is called and fallback receiver is invoked. */
    @Test
    fun test_1_handlePush_altcraft_callsDeliveryAndFallback() = runBlocking {
        every { SubFunction.altcraftPush(ALT_MESSAGE) } returns true
        coEvery { ConfigSetup.getConfig(ctx) } returns null

        // Intercept default receiver construction and its pushHandler(...)
        mockkConstructor(AltcraftSDK.PushReceiver::class)
        every { anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(ctx, ALT_MESSAGE) } just Runs

        IncomingPushManager.handlePush(ctx, ALT_MESSAGE)

        verify(timeout = TIMEOUT_MS, exactly = 1) {
            OpenPushStrategy.deliveryEventStrategy(ctx, ALT_MESSAGE)
        }
        verify(timeout = TIMEOUT_MS, exactly = 1) {
            anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(ctx, ALT_MESSAGE)
        }
    }

    /** - test_2: Altcraft message, no custom receivers -> fallback to default AltcraftSDK.PushReceiver. */
    @Test
    fun test_2_handlePush_fallbackToDefaultReceiver_whenNoCustomReceivers() = runBlocking {
        every { SubFunction.altcraftPush(ALT_MESSAGE) } returns true
        coEvery { ConfigSetup.getConfig(ctx) } returns null

        mockkConstructor(AltcraftSDK.PushReceiver::class)
        every { anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(ctx, ALT_MESSAGE) } just Runs

        IncomingPushManager.handlePush(ctx, ALT_MESSAGE)

        verify(timeout = TIMEOUT_MS, exactly = 1) {
            OpenPushStrategy.deliveryEventStrategy(ctx, ALT_MESSAGE)
        }
        verify(timeout = TIMEOUT_MS, exactly = 1) {
            anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(ctx, ALT_MESSAGE)
        }
    }

    /** - test_3: Non-Altcraft message -> no deliveryEventStrategy, no recipients/fallback. */
    @Test
    fun test_4_handlePush_nonAltcraft_doesNothing() = runBlocking {
        every { SubFunction.altcraftPush(NON_ALT_MESSAGE) } returns false

        mockkConstructor(AltcraftSDK.PushReceiver::class)
        every { anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(any(), any()) } just Runs

        IncomingPushManager.handlePush(ctx, NON_ALT_MESSAGE)

        verify(timeout = TIMEOUT_MS, exactly = 0) {
            OpenPushStrategy.deliveryEventStrategy(any(), any())
        }
        verify(timeout = TIMEOUT_MS, exactly = 0) {
            anyConstructed<AltcraftSDK.PushReceiver>().pushHandler(any(), any())
        }
    }
}
