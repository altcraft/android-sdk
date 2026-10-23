@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.platform.token.TokenAPI
import com.altcraft.sdk.push.platform.token.TokenManager
import com.altcraft.sdk.push.platform.token.provider.FCMInterface
import com.altcraft.sdk.push.platform.token.provider.HMSInterface
import com.altcraft.sdk.push.platform.token.provider.RustoreInterface
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * TokenApiTest
 *
 * Verifies provider registration through the public token API.
 */
class TokenApiTest {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
    }

    @After
    fun tearDown() {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
        unmockkAll()
    }

    private class FakeFcm : FCMInterface {
        override suspend fun getToken(): String? = null
        override fun deleteToken(completion: (Boolean) -> Unit) = completion(false)
    }

    private class FakeHms : HMSInterface {
        override suspend fun getToken(context: Context): String? = null
        override fun deleteToken(context: Context, complete: (Boolean) -> Unit) = complete(false)
    }

    private class FakeRus : RustoreInterface {
        override suspend fun getToken(): String? = null
        override fun deleteToken(complete: (Boolean) -> Unit) = complete(false)
    }

    /** The facade registers and clears the FCM provider. */
    @Test
    fun setFCMTokenProvider_registersAndClearsProvider() {
        val provider = FakeFcm()

        TokenAPI.setFCMTokenProvider(provider)
        assertSame(provider, TokenManager.fcmProvider)

        TokenAPI.setFCMTokenProvider(null)
        assertNull(TokenManager.fcmProvider)
    }

    /** The facade registers and clears the HMS provider. */
    @Test
    fun setHMSTokenProvider_registersAndClearsProvider() {
        val provider = FakeHms()

        TokenAPI.setHMSTokenProvider(provider)
        assertSame(provider, TokenManager.hmsProvider)

        TokenAPI.setHMSTokenProvider(null)
        assertNull(TokenManager.hmsProvider)
    }

    /** The facade registers and clears the RuStore provider. */
    @Test
    fun setRuStoreTokenProvider_registersAndClearsProvider() {
        val provider = FakeRus()

        TokenAPI.setRuStoreTokenProvider(provider)
        assertSame(provider, TokenManager.rustoreProvider)

        TokenAPI.setRuStoreTokenProvider(null)
        assertNull(TokenManager.rustoreProvider)
    }
}
