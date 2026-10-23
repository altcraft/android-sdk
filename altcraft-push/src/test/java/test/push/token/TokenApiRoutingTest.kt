@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.data.Preferenses
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.platform.token.TokenAPI
import com.altcraft.sdk.push.platform.token.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TokenApiRoutingTest
 *
 * Verifies that the public token API delegates to the underlying managers.
 */
class TokenApiRoutingTest {

    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** getToken delegates to TokenManager.getCurrentPushToken. */
    @Test
    fun getToken_delegatesToTokenManager() = runTest {
        mockkObject(TokenManager)
        coEvery { TokenManager.getCurrentPushToken(context) } returns TokenData("android-firebase", "tok")

        val token = TokenAPI.getToken(context)

        assertEquals("tok", token!!.token)
        coVerify(exactly = 1) { TokenManager.getCurrentPushToken(context) }
    }

    /** deleteToken delegates to TokenManager.deleteDeviceToken. */
    @Test
    fun deleteToken_delegatesToTokenManager() = runTest {
        mockkObject(TokenManager)
        coEvery { TokenManager.deleteDeviceToken(context, "android-huawei") } returns true

        val deleted = TokenAPI.deleteToken(context, "android-huawei")

        assertEquals(true, deleted)
        coVerify(exactly = 1) { TokenManager.deleteDeviceToken(context, "android-huawei") }
    }

    /** forcedTokenUpdate delegates to TokenManager.forcedTokenUpdate. */
    @Test
    fun forcedTokenUpdate_delegatesToTokenManager() = runTest {
        mockkObject(TokenManager)
        coEvery { TokenManager.forcedTokenUpdate(context) } just Runs

        TokenAPI.forcedTokenUpdate(context)

        coVerify(exactly = 1) { TokenManager.forcedTokenUpdate(context) }
    }

    /** changeProviderPriority delegates to TokenManager.changePriority. */
    @Test
    fun changeProviderPriority_delegatesToTokenManager() = runTest {
        mockkObject(TokenManager)
        coEvery { TokenManager.changePriority(context, listOf("android-rustore")) } just Runs

        TokenAPI.changeProviderPriority(context, listOf("android-rustore"))

        coVerify(exactly = 1) { TokenManager.changePriority(context, listOf("android-rustore")) }
    }

    /** setToken delegates to Preferenses.setPushToken. */
    @Test
    fun setToken_delegatesToPreferenses() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setPushToken(context, "android-firebase", "tok") } just Runs

        TokenAPI.setToken(context, "android-firebase", "tok")

        coVerify(exactly = 1) { Preferenses.setPushToken(context, "android-firebase", "tok") }
    }
}
