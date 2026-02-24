package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.sdk_events.Events
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TokenManagerTest
 *
 * Positive scenarios:
 *  - test_1: allProvidersValid() returns true for null/empty and for all-known providers.
 *  - test_2: getCurrentPushToken() returns manual token when ALL providers are null and logs event once.
 *  - test_3: getCurrentPushToken() respects provider priority list (first success wins).
 *  - test_4: getCurrentPushToken() uses default order (FCM → HMS → RuStore) when no priority set.
 *  - test_5: tokenEvent logs exactly once per session for the same token.
 *  - test_6: tokenEvent logs again after reset when token changes.
 *  - test_7: getCurrentPushToken() retries on empty/exception and falls back to next provider.
 *
 * Negative scenarios:
 *  - test_8: allProvidersValid() returns false if list contains unknown provider.
 *  - test_9: getCurrentPushToken() returns null when all providers fail (no event logged).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private lateinit var context: Context

    private lateinit var fcm: FCMInterface
    private lateinit var hms: HMSInterface
    private lateinit var rus: RustoreInterface

    private fun cfg(priority: List<String>? = null) = ConfigurationEntity(
        id = 0,
        icon = null,
        apiUrl = "https://api.example.com",
        rToken = "user-tag-123",
        appInfo = null,
        pushReceiverModules = null,
        providerPriorityList = priority,
        pushChannelName = null,
        pushChannelDescription = null
    )

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkObject(ConfigSetup)
        mockkObject(Preferenses)
        mockkObject(Events)

        fcm = mockk(relaxed = true)
        hms = mockk(relaxed = true)
        rus = mockk(relaxed = true)

        TokenManager.fcmProvider = fcm
        TokenManager.hmsProvider = hms
        TokenManager.rustoreProvider = rus

        coEvery { ConfigSetup.getConfig(any()) } returns cfg()
        coEvery { Preferenses.getManualToken(any()) } returns null

        coEvery { Events.error(any(), any()) } answers {
            DataClasses.Error(function = firstArg())
        }
        coEvery {
            Events.event(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Event(function = firstArg()) }

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

    /** - test_1: allProvidersValid() returns true for null/empty and for all-known providers. */
    @Test
    fun test_1_allProvidersValid_nullOrEmpty_returnsTrue() {
        assertThat(TokenManager.allProvidersValid(null), `is`(true))
        assertThat(TokenManager.allProvidersValid(emptyList()), `is`(true))

        val ok = listOf(Constants.FCM_PROVIDER, Constants.HMS_PROVIDER, Constants.RUS_PROVIDER)
        assertThat(TokenManager.allProvidersValid(ok), `is`(true))
    }

    /** - test_8: allProvidersValid() returns false if list contains unknown provider. */
    @Test
    fun test_8_allProvidersValid_containsUnknown_returnsFalse() {
        val bad = listOf(Constants.FCM_PROVIDER, "weird-provider")
        assertThat(TokenManager.allProvidersValid(bad), `is`(false))
    }

    /** - test_2: getCurrentPushToken() returns manual token when ALL providers are null and logs event once. */
    @Test
    fun test_2_getCurrentPushToken_returnsManualToken_whenNoProviders_andLogsEventOnce() = runTest {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null

        val manual = TokenData(Constants.FCM_PROVIDER, "MANUAL-123")
        coEvery { Preferenses.getManualToken(context) } returns manual

        val result = TokenManager.getCurrentPushToken(context)

        assertThat(result, `is`(manual))
        coVerify(exactly = 1) {
            Events.event(
                eq("tokenEvent"),
                any(),
                match { it["provider"] == Constants.FCM_PROVIDER && it["token"] == "MANUAL-123" }
            )
        }
    }

    /** - test_3: getCurrentPushToken() respects provider priority list (first success wins). */
    @Test
    fun test_3_getCurrentPushToken_respectsPriority_firstAvailableWins() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(
            listOf(Constants.HMS_PROVIDER, Constants.RUS_PROVIDER, Constants.FCM_PROVIDER)
        )

        coEvery { hms.getToken(context) } returns "HMS-OK"
        coEvery { rus.getToken() } answers {
            throw AssertionError("rus.getToken() must NOT be called")
        }
        coEvery { fcm.getToken() } answers {
            throw AssertionError("fcm.getToken() must NOT be called")
        }

        val result = TokenManager.getCurrentPushToken(context)

        assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-OK")))
        coVerify(exactly = 1) { hms.getToken(context) }
        coVerify(exactly = 0) { rus.getToken() }
        coVerify(exactly = 0) { fcm.getToken() }
    }

    /** - test_4: getCurrentPushToken() uses default order (FCM → HMS → RuStore) when no priority set. */
    @Test
    fun test_4_getCurrentPushToken_usesDefaultOrder_whenNoPriority() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

        coEvery { fcm.getToken() } returnsMany listOf("", "", null)
        coEvery { hms.getToken(context) } returns "HMS-XYZ"
        coEvery { rus.getToken() } returns null

        val result = TokenManager.getCurrentPushToken(context)

        assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-XYZ")))
        coVerify(atLeast = 1) { fcm.getToken() }
        coVerify(exactly = 1) { hms.getToken(context) }
    }

    /** - test_5: tokenEvent logs exactly once per session for the same token. */
    @Test
    fun test_5_getCurrentPushToken_logsEventOnlyOnce_perSession_forSameToken() = runTest {
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()

        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)
        coEvery { fcm.getToken() } returns "FCM-111"

        val r1 = TokenManager.getCurrentPushToken(context)
        val r2 = TokenManager.getCurrentPushToken(context)

        assertThat(r1, `is`(TokenData(Constants.FCM_PROVIDER, "FCM-111")))
        assertThat(r2, `is`(TokenData(Constants.FCM_PROVIDER, "FCM-111")))

        coVerify(exactly = 1) {
            Events.event(
                eq("tokenEvent"),
                any(),
                match { it["provider"] == Constants.FCM_PROVIDER && it["token"] == "FCM-111" }
            )
        }
    }

    /** - test_6: tokenEvent logs again after reset when token changes. */
    @Test
    fun test_6_getCurrentPushToken_logsAgain_afterReset_whenTokenChanges() = runTest {
        TokenManager.tokenLogShow = AtomicBoolean(false)
        TokenManager.tokens.clear()

        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)
        coEvery { fcm.getToken() } returns "A"

        val r1 = TokenManager.getCurrentPushToken(context)

        assertThat(r1, `is`(TokenData(Constants.FCM_PROVIDER, "A")))
        coVerify(exactly = 1) {
            Events.event(eq("tokenEvent"), any(), match { it["token"] == "A" })
        }

        TokenManager.tokenLogShow = AtomicBoolean(false)
        coEvery { fcm.getToken() } returns "B"

        val r2 = TokenManager.getCurrentPushToken(context)

        assertThat(r2, `is`(TokenData(Constants.FCM_PROVIDER, "B")))
        coVerify(exactly = 1) {
            Events.event(eq("tokenEvent"), any(), match { it["token"] == "B" })
        }
    }

    /** - test_7: getCurrentPushToken() retries on empty/exception and falls back to next provider. */
    @Test
    fun test_7_getCurrentPushToken_retriesOnEmptyOrException_thenFallsBackToNextProvider() =
        runTest {
            coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

            coEvery { fcm.getToken() } throws RuntimeException("boom") andThen "" andThen null
            coEvery { hms.getToken(context) } returns "HMS-OK"
            coEvery { rus.getToken() } returns null

            val result = TokenManager.getCurrentPushToken(context)

            assertThat(result, `is`(TokenData(Constants.HMS_PROVIDER, "HMS-OK")))
            coVerify(atLeast = 1) { Events.error(eq("getNonEmptyToken"), any()) }
        }

    /** - test_9: getCurrentPushToken() returns null when all providers fail (no event logged). */
    @Test
    fun test_9_getCurrentPushToken_returnsNull_whenAllProvidersFail_noEventLogged() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns cfg(priority = null)

        coEvery { fcm.getToken() } returnsMany listOf("", "", null)
        coEvery { hms.getToken(context) } returnsMany listOf("", "", null)
        coEvery { rus.getToken() } returnsMany listOf("", "", null)

        val result = TokenManager.getCurrentPushToken(context)

        assertThat(result, `is`(null as TokenData?))
        coVerify(exactly = 0) { Events.event(eq("tokenEvent"), any(), any()) }
    }
}