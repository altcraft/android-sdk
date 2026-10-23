@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.platform.token.TokenManager
import com.altcraft.sdk.push.platform.token.provider.FCMInterface
import com.altcraft.sdk.push.platform.token.provider.HMSInterface
import com.altcraft.sdk.push.platform.token.provider.RustoreInterface
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TokenManagerTest
 *
 * Verifies provider validation and token selection priorities.
 *
 * Positive scenarios:
 *  - Only supported provider identifiers are accepted.
 *  - The default provider order is FCM, HMS, RuStore.
 *  - The configured priority list selects the matching provider.
 *  - A manual token is returned when no provider is registered.
 *
 * Negative scenarios:
 *  - Unknown provider identifiers are rejected.
 */
class TokenManagerTest {

    private class FakeFcm(
        private val token: String
    ) : FCMInterface {
        override suspend fun getToken(): String? = token
        override fun deleteToken(completion: (Boolean) -> Unit) = completion(true)
    }

    private class FakeHms(
        private val token: String
    ) : HMSInterface {
        override suspend fun getToken(context: Context): String? = token
        override fun deleteToken(context: Context, complete: (Boolean) -> Unit) = complete(true)
    }

    private class FakeRus(
        private val token: String
    ) : RustoreInterface {
        override suspend fun getToken(): String? = token
        override fun deleteToken(complete: (Boolean) -> Unit) = complete(true)
    }

    private val context: Context = mockk(relaxed = true)
    private val env: Environment = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(Environment.Companion)
        coEvery { Environment.create(context) } returns env

        mockkObject(Events)
        every { Events.event(any(), any(), any(), any()) } answers {
            SDKEventModel.Event(function = firstArg())
        }
        every { Events.error(any(), any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
    }

    @After
    fun tearDown() {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
        unmockkAll()
    }

    private fun registerAllProviders() {
        TokenManager.fcmProvider = FakeFcm("fcm-token")
        TokenManager.hmsProvider = FakeHms("hms-token")
        TokenManager.rustoreProvider = FakeRus("rus-token")
    }

    private fun withProviderPriority(priority: List<String>) {
        coEvery { env.pushConfig() } returns PushConfigEntity(
            providerPriority = priority
        )
    }

    /** All supported provider identifiers are accepted; unknown ones are not. */
    @Test
    fun providersValid_acceptsKnownRejectsUnknown() {
        assertTrue(TokenManager.providersValid(listOf("android-firebase", "android-huawei", "android-rustore")))
        assertTrue(TokenManager.providersValid(emptyList()))
        assertFalse(TokenManager.providersValid(listOf("android-firebase", "google")))
        assertFalse(TokenManager.providersValid(listOf("adb")))
    }

    /** Without a priority list the default order FCM, HMS, RuStore applies. */
    @Test
    fun getCurrentPushToken_defaultOrder_prefersFcm() = runTest {
        registerAllProviders()
        withProviderPriority(emptyList())

        val token = TokenManager.getCurrentPushToken(context)

        assertEquals("android-firebase", token!!.provider)
        assertEquals("fcm-token", token.token)
    }

    /** The configured priority list selects the matching provider. */
    @Test
    fun getCurrentPushToken_priorityList_honorsOrder() = runTest {
        registerAllProviders()

        withProviderPriority(listOf("android-huawei", "android-firebase"))
        val hms = TokenManager.getCurrentPushToken(context)
        assertEquals("android-huawei", hms!!.provider)

        withProviderPriority(listOf("android-rustore"))
        val rus = TokenManager.getCurrentPushToken(context)
        assertEquals("android-rustore", rus!!.provider)
    }

    /** Unknown entries in the priority list are skipped. */
    @Test
    fun getCurrentPushToken_priorityList_skipsUnknownEntries() = runTest {
        registerAllProviders()
        withProviderPriority(listOf("bogus", "android-firebase"))

        val token = TokenManager.getCurrentPushToken(context)

        assertEquals("android-firebase", token!!.provider)
    }

    /** Without registered providers the manual token is returned. */
    @Test
    fun getCurrentPushToken_noProviders_returnsManualToken() = runTest {
        coEvery { env.manualToken() } returns TokenData("manual", "manual-token")

        val token = TokenManager.getCurrentPushToken(context)

        assertEquals("manual", token!!.provider)
        assertEquals("manual-token", token.token)
    }

    /** No token is available when every provider and the manual token are empty. */
    @Test
    fun getCurrentPushToken_nothingAvailable_returnsNull() = runTest {
        coEvery { env.manualToken() } returns null
        coEvery { env.pushConfig() } returns PushConfigEntity()

        val token = TokenManager.getCurrentPushToken(context)

        assertNull(token)
    }
}
