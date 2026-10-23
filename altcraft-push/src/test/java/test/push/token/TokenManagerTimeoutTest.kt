@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.push.platform.token.TokenManager
import com.altcraft.sdk.push.platform.token.provider.FCMInterface
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TokenManagerTimeoutTest
 *
 * Verifies that deleteDeviceToken never suspends forever when a provider
 * never invokes its delete callback.
 *
 * Positive scenarios:
 *  - test_2: provider callback fires -> returns true.
 *
 * Negative scenarios:
 *  - test_1: provider callback never fires -> returns false after timeout.
 */
class TokenManagerTimeoutTest {

    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
        unmockkAll()
    }

    /** test_1: callback never fires -> returns false after timeout. */
    @Test
    fun deleteDeviceToken_timesOut_whenCallbackNeverFires() = runTest {
        TokenManager.fcmProvider = object : FCMInterface {
            override suspend fun getToken(): String? = null

            override fun deleteToken(completion: (Boolean) -> Unit) = Unit
        }

        val deleted = TokenManager.deleteDeviceToken(context, FCM_PROVIDER)

        assertFalse(deleted)
    }

    /** test_2: provider callback fires -> returns true. */
    @Test
    fun deleteDeviceToken_returnsTrue_whenCallbackFires() = runTest {
        TokenManager.fcmProvider = object : FCMInterface {
            override suspend fun getToken(): String? = null

            override fun deleteToken(completion: (Boolean) -> Unit) {
                completion(true)
            }
        }

        val deleted = TokenManager.deleteDeviceToken(context, FCM_PROVIDER)

        assertTrue(deleted)
    }
}
