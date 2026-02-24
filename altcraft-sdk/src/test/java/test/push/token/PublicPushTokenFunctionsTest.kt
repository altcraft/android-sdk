@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import com.altcraft.sdk.push.token.PublicPushTokenFunctions
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PublicPushTokenFunctionsTest
 *
 * Positive scenarios:
 *  - test_1: deleteDeviceToken() routes to FCM and invokes completion callback.
 *  - test_2: deleteDeviceToken() routes to HMS and invokes completion callback.
 *  - test_3: deleteDeviceToken() routes to RuStore and invokes completion callback.
 *  - test_4: forcedTokenUpdate() deletes current token, triggers TokenUpdate.pushTokenUpdate(),
 *  then calls completion.
 *  - test_5: changePushProviderPriorityList() config exists & providers valid -> updates list,
 *  refreshes cache, triggers tokenUpdate.
 *
 * Negative scenarios:
 *  - test_6: forcedTokenUpdate() when Environment.create/token fails -> no completion.
 *  - test_7: changePushProviderPriorityList() invalid providers -> no update, no tokenUpdate.
 *  - test_8: changePushProviderPriorityList() no config in DB -> no update, no tokenUpdate.
 */
class PublicPushTokenFunctionsTest {

    private companion object {
        private const val TIMEOUT_MS = 2000L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        mockkObject(TokenManager)
        mockkObject(TokenUpdate)
        mockkObject(SDKdb)
        mockkObject(ConfigSetup)
        mockkObject(Environment)

        TokenManager.fcmProvider = null
        TokenManager.hmsProvider = null
        TokenManager.rustoreProvider = null
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    /** - test_1: deleteDeviceToken() routes to FCM and invokes completion callback. */
    @Test
    fun deleteDeviceToken_routesToFCM_andCompletes() = runBlocking {
        val provider = mockk<FCMInterface>(relaxed = true)
        TokenManager.fcmProvider = provider

        every { provider.deleteToken(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)
        PublicPushTokenFunctions.deleteDeviceToken(ctx, FCM_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { provider.deleteToken(any()) }
    }

    /** - test_2: deleteDeviceToken() routes to HMS and invokes completion callback. */
    @Test
    fun deleteDeviceToken_routesToHMS_andCompletes() = runBlocking {
        val provider = mockk<HMSInterface>(relaxed = true)
        TokenManager.hmsProvider = provider

        every { provider.deleteToken(eq(ctx), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)
        PublicPushTokenFunctions.deleteDeviceToken(ctx, HMS_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { provider.deleteToken(eq(ctx), any()) }
    }

    /** - test_3: deleteDeviceToken() routes to RuStore and invokes completion callback. */
    @Test
    fun deleteDeviceToken_routesToRuStore_andCompletes() = runBlocking {
        val provider = mockk<RustoreInterface>(relaxed = true)
        TokenManager.rustoreProvider = provider

        every { provider.deleteToken(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)
        PublicPushTokenFunctions.deleteDeviceToken(ctx, RUS_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { provider.deleteToken(any()) }
    }

    /** - test_4: forcedTokenUpdate() deletes current token, triggers TokenUpdate.pushTokenUpdate(), then calls completion. */
    @Test
    fun forcedTokenUpdate_deletes_then_updates_then_completes() = runBlocking {
        val provider = mockk<FCMInterface>(relaxed = true)
        TokenManager.fcmProvider = provider

        val env = mockk<Environment>(relaxed = true)
        val tokenData = mockk<DataClasses.TokenData>(relaxed = true)

        mockkObject(Environment.Companion)
        every { Environment.create(ctx) } returns env
        coEvery { env.token() } returns tokenData
        every { tokenData.provider } returns FCM_PROVIDER

        every { provider.deleteToken(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }

        coEvery { TokenUpdate.pushTokenUpdate(ctx) } returns true

        val latch = CountDownLatch(1)
        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { provider.deleteToken(any()) }
        coVerify(exactly = 1) { TokenUpdate.pushTokenUpdate(ctx) }

        unmockkObject(Environment.Companion)
    }


    /** - test_6: forcedTokenUpdate() when Environment.create/token fails -> no completion. */
    @Test
    fun forcedTokenUpdate_envThrows_noops() = runBlocking {
        every { Environment.create(ctx) } throws RuntimeException("boom")

        val latch = CountDownLatch(1)
        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        assertFalse(latch.await(500, TimeUnit.MILLISECONDS))
        coVerify(exactly = 0) { TokenUpdate.pushTokenUpdate(any()) }
    }

    /** - test_5: changePushProviderPriorityList() config exists & providers valid -> updates list, refreshes cache, triggers tokenUpdate. */
    @Test
    fun changePushProviderPriorityList_happyPath_updates_and_triggers() = runBlocking {
        val room = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        every { SDKdb.getDb(ctx) } returns room
        every { room.request() } returns dao

        coEvery { dao.getConfig() } returns ConfigurationEntity(
            id = 1,
            apiUrl = "https://api",
            rToken = null,
            appInfo = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        every { TokenManager.allProvidersValid(any()) } returns true

        coJustRun { dao.updateProviderPriorityList(any()) }
        coJustRun { ConfigSetup.updateConfigCache(ctx) }

        coEvery { TokenUpdate.pushTokenUpdate(ctx) } returns true

        val newPriority = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)
        PublicPushTokenFunctions.changePushProviderPriorityList(ctx, newPriority)

        coVerify(exactly = 1) { dao.updateProviderPriorityList(newPriority) }
        coVerify(exactly = 1) { ConfigSetup.updateConfigCache(ctx) }
        coVerify(exactly = 1) { TokenUpdate.pushTokenUpdate(ctx) }
    }

    /** - test_7: changePushProviderPriorityList() invalid providers -> no update, no tokenUpdate. */
    @Test
    fun changePushProviderPriorityList_invalidProviders_noops() = runBlocking {
        val room = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        every { SDKdb.getDb(ctx) } returns room
        every { room.request() } returns dao

        coEvery { dao.getConfig() } returns ConfigurationEntity(
            id = 1,
            apiUrl = "https://api",
            rToken = null,
            appInfo = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        every { TokenManager.allProvidersValid(any()) } returns false

        PublicPushTokenFunctions.changePushProviderPriorityList(ctx, listOf("bad-provider"))

        coVerify(exactly = 0) { dao.updateProviderPriorityList(any()) }
        coVerify(exactly = 0) { ConfigSetup.updateConfigCache(any()) }
        coVerify(exactly = 0) { TokenUpdate.pushTokenUpdate(any()) }
    }

    /** - test_8: changePushProviderPriorityList() no config in DB -> no update, no tokenUpdate. */
    @Test
    fun changePushProviderPriorityList_noConfig_noops() = runBlocking {
        val room = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        every { SDKdb.getDb(ctx) } returns room
        every { room.request() } returns dao

        coEvery { dao.getConfig() } returns null
        every { TokenManager.allProvidersValid(any()) } returns true

        PublicPushTokenFunctions.changePushProviderPriorityList(ctx, listOf(FCM_PROVIDER))

        coVerify(exactly = 0) { dao.updateProviderPriorityList(any()) }
        coVerify(exactly = 0) { ConfigSetup.updateConfigCache(any()) }
        coVerify(exactly = 0) { TokenUpdate.pushTokenUpdate(any()) }
    }
}