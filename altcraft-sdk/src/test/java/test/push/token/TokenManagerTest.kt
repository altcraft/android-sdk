/// Created by Andrey Pogodin. Copyright © 2024 Altcraft.

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

// SDK imports
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import com.altcraft.sdk.data.room.ConfigurationEntity

/**
 * TokenManagerUnitTest
 *
 * Positive scenarios:
 *  - test_1: allProvidersValid() returns true for null/empty and for all-known providers.
 *  - test_2: getCurrentToken() returns manual token without calling providers or logging event.
 *  - test_3: getCurrentToken() respects provider priority list (first success wins).
 *  - test_4: getCurrentToken() uses default order (FCM → HMS → RuStore) when no priority set.
 *  - test_5: tokenEvent logs exactly once per session for the same token.
 *  - test_6: tokenEvent logs again after reset when token changes.
 *  - test_7: getCurrentToken() retries on empty/exception and falls back to next provider.
 *
 * Negative scenarios:
 *  - test_8: allProvidersValid() returns false if list contains unknown provider.
 *  - test_9: getCurrentToken() returns null when all providers fail (no event logged).
 *
 * Notes:
 *  - Pure JVM unit tests; Context is a relaxed mock.
 *  - Avoid recursive stubbing: ConfigSetup.getConfig() always returns a prebuilt ConfigurationEntity.
 *  - Events.* are stubbed to return DataClasses objects (not Unit) to avoid ClassCastException.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private lateinit var context: Context

    // Providers
    private lateinit var fcm: FCMInterface
    private lateinit var hms: HMSInterface
    private lateinit var rus: RustoreInterface

    // Helper: build a base config without calling the real DB
    private fun cfg(priority: List<String>? = null) = ConfigurationEntity(
        id = 0,
        icon = null,
        apiUrl = "https://api.example.com",
        rToken = "user-tag-123",
        appInfo = null,
        usingService = false,
        serviceMessage = null,
        pushReceiverModules = null,
        providerPriorityList = priority,
        pushChannelName = null,
        pushChannelDescription = null
    )

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock singletons used inside TokenManager
        mockkObject(ConfigSetup)
        mockkObject(Preferenses)
        mockkObject(Events)

        // Provider mocks
        fcm = mockk(relaxed = true)
        hms = mockk(relaxed = true)
        rus = mockk(relaxed = true)

        // Inject providers into TokenManager
        TokenManager.fcmProvider = fcm
        TokenManager.hmsProvider = hms
        TokenManager.rustoreProvider = rus

        // Default config (no priority list) — IMPORTANT: return a prebuilt instance
        coEvery { ConfigSetup.getConfig(any()) } returns cfg()

        // Default: no manual token
        every { Preferenses.getManualToken(any()) } returns null

        // Events.* return DataClasses objects (not Unit)
        coEvery { Events.error(any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        coEvery {
            Events.event(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Event(function = firstArg()) }

        // Reset token logging state for each test
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()
    }

    @After
    fun tearDown() {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()
        unmockkAll()
    }

    // region allProvidersValid

    /** Null/empty lists are considered valid. */
    @Test
    fun allProvidersValid_nullOrEmpty_returns_true() {
        assertThat(TokenManager.allProvidersValid(null), `is`(true))
        assertThat(TokenManager.allProvidersValid(emptyList()), `is`(true))
    }

    /** All known providers → true. */
    @Test
    fun allProvidersValid_allKnown_returns_true() {
        val ok = listOf(Constants.FCM_PROVIDER, Constants.HMS_PROVIDER, Constants.RUS_PROVIDER)
        assertThat(TokenManager.allProvidersValid(ok), `is`(true))
    }

    /** Contains an unknown provider → false. */
    @Test
    fun allProvidersValid_containsUnknown_returns_false() {
        val bad = listOf(Constants.FCM_PROVIDER, "weird-provider")
        assertThat(TokenManager.allProvidersValid(bad), `is`(false))
    }

    // endregion

    // region getCurrentToken: manual token

    /** Manual token short-circuits: no provider calls, no event. */
    @Test
    fun getCurrentToken_returns_manual_token_and_does_not_log_event() = runTest {
        val manual = TokenData(Constants.FCM_PROVIDER, "MANUAL-123")
        every { Preferenses.getManualToken(context) } returns manual

        val result = TokenManager.getCurrentToken(context)

        assertThat(result, `is`(manual))
        coVerify(exactly = 0) { Events.event(eq("tokenEvent"), any(), any()) }
        coVerify(exactly = 0) { fcm.getToken() }
        coVerify(exactly = 0) { hms.getToken(any()) }
        coVerify(exactly = 0) { rus.getToken() }
    }

    // endregion

    // region getCurrentToken: priority list

    /** Respects priority: first provider that returns a non-empty token wins. */
    @Test
    fun getCurrentToken_respects_priority_first_available() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(
            listOf(Constants.HMS_PROVIDER, Constants.RUS_PROVIDER, Constants.FCM_PROVIDER)
        )

        coEvery { hms.getToken(context) } returns "HMS-OK"
        coEvery { rus.getToken() } answers { throw AssertionError("rus.getToken() must NOT be called") }
        coEvery { fcm.getToken() } answers { throw AssertionError("fcm.getToken() must NOT be called") }

        val result = TokenManager.getCurrentToken(context)

        assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-OK")))
        coVerify(exactly = 1) { hms.getToken(context) }
        coVerify(exactly = 0) { rus.getToken() }
        coVerify(exactly = 0) { fcm.getToken() }
    }

    // endregion

    // region getCurrentToken: default order (FCM → HMS → RuStore)

    /** Without priority list, tries FCM then HMS then RuStore. */
    @Test
    fun getCurrentToken_uses_default_order_when_no_priority() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

        // FCM returns "", "", null across internal retries (3 attempts)
        coEvery { fcm.getToken() } returnsMany listOf("", "", null)
        // HMS succeeds
        coEvery { hms.getToken(context) } returns "HMS-XYZ"

        val result = TokenManager.getCurrentToken(context)

        assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-XYZ")))
        coVerify(atLeast = 1) { fcm.getToken() }
        coVerify(exactly = 1) { hms.getToken(context) }
        coVerify(exactly = 0) { rus.getToken() }
    }

    // endregion

    // region tokenEvent logging behavior

    /** Logs the set-token event only once per session for the same token. */
    @Test
    fun getCurrentToken_logs_event_only_once_per_session_for_same_token() = runTest {
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)
        coEvery { fcm.getToken() } returns "FCM-111"

        val r1 = TokenManager.getCurrentToken(context)
        val r2 = TokenManager.getCurrentToken(context)

        assertThat(r1, `is`(TokenData(Constants.FCM_PROVIDER, "FCM-111")))
        assertThat(r2, `is`(TokenData(Constants.FCM_PROVIDER, "FCM-111")))
        // Logged only once
        coVerify(exactly = 1) {
            Events.event(
                eq("tokenEvent"),
                any(),
                match { it["provider"] == Constants.FCM_PROVIDER && it["token"] == "FCM-111" }
            )
        }
    }

    /** After resetting the session flag and changing token, logs again. */
    @Test
    fun getCurrentToken_logs_again_after_session_reset_and_token_change() = runTest {
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

        // First token = "A"
        coEvery { fcm.getToken() } returns "A"
        val r1 = TokenManager.getCurrentToken(context)
        assertThat(r1, `is`(TokenData(Constants.FCM_PROVIDER, "A")))
        coVerify(exactly = 1) {
            Events.event(
                eq("tokenEvent"),
                any(),
                match { it["token"] == "A" })
        }

        // Reset "session" flag and change token
        TokenManager.tokenLogShow = AtomicBoolean(false)
        coEvery { fcm.getToken() } returns "B"

        val r2 = TokenManager.getCurrentToken(context)
        assertThat(r2, `is`(TokenData(Constants.FCM_PROVIDER, "B")))
        coVerify(exactly = 1) {
            Events.event(
                eq("tokenEvent"),
                any(),
                match { it["token"] == "B" })
        }
    }

    // endregion

    // region retries & fallbacks

    /** When current provider throws/returns empty, falls back to next; errors are logged. */
    @Test
    fun getCurrentToken_retries_on_empty_and_handles_exceptions_then_uses_next_provider() =
        runTest {
            coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

            // FCM: throw once, then "", then null (3 attempts inside getNonEmptyToken)
            coEvery { fcm.getToken() } throws RuntimeException("boom") andThen "" andThen null
            // HMS succeeds
            coEvery { hms.getToken(context) } returns "HMS-OK"

            val result = TokenManager.getCurrentToken(context)

            assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-OK")))
            coVerify(atLeast = 1) { Events.error(eq("getNonEmptyToken"), any()) }
        }

    // endregion

    // region all providers fail → null

    /** All providers return empty/null → result is null and no event logged. */
    @Test
    fun getCurrentToken_returns_null_when_all_providers_fail() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

        coEvery { fcm.getToken() } returnsMany listOf("", "", null)
        coEvery { hms.getToken(context) } returnsMany listOf("", "", null)
        coEvery { rus.getToken() } returnsMany listOf("", "", null)

        val result = TokenManager.getCurrentToken(context)

        assertThat(result, `is`(null as TokenData?))
        coVerify(exactly = 0) { Events.event(eq("tokenEvent"), any(), any()) }
    }

    // endregion
}
