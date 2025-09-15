@file:Suppress("SpellCheckingInspection")

package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.push.token.PublicPushTokenFunctions
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * PublicPushTokenFunctionsTest
 *
 * Positive scenarios:
 *  - test_1: deleteDeviceToken() routes to proper TokenManager method (FCM / HMS / RuStore) and invokes completion
 *  - test_2: forcedTokenUpdate(): deletes current token, then calls TokenUpdate.tokenUpdate(), then calls complete()
 *  - test_3: changePushProviderPriorityList(): when config exists and providers valid -> updates list, refreshes cache, triggers tokenUpdate
 *
 * Negative scenarios:
 *  - test_4: forcedTokenUpdate(): when getPushToken() returns null -> no delete, no tokenUpdate, no completion
 *  - test_5: changePushProviderPriorityList(): invalid providers OR no config -> no update, no tokenUpdate
 *
 * Notes:
 *  - Pure unit tests; android.util.Log is mocked to avoid "not mocked" errors.
 *  - For coroutines launched inside forcedTokenUpdate(), we use CountDownLatch and verify with timeouts.
 */
class PublicPushTokenFunctionsTest {

    private companion object {
        private const val TIMEOUT_MS = 2000L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Avoid android.util.Log crashes in JVM unit tests
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
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Log::class)
    }

    // ---------------- deleteDeviceToken ----------------

    /** test_1: deleteDeviceToken(): dispatches to TokenManager.deleteFCMToken and invokes completion */
    @Test
    fun deleteDeviceToken_routesToFCM_andCompletes() = runBlocking {
        coEvery { TokenManager.deleteFCMToken(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.deleteDeviceToken(ctx, FCM_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { TokenManager.deleteFCMToken(any()) }
        coVerify(exactly = 0) { TokenManager.deleteHMSToken(any(), any()) }
        coVerify(exactly = 0) { TokenManager.deleteRuStoreToken(any()) }
    }

    /** test_1: deleteDeviceToken(): dispatches to TokenManager.deleteHMSToken and invokes completion */
    @Test
    fun deleteDeviceToken_routesToHMS_andCompletes() = runBlocking {
        coEvery { TokenManager.deleteHMSToken(any(), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.deleteDeviceToken(ctx, HMS_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { TokenManager.deleteHMSToken(ctx, any()) }
        coVerify(exactly = 0) { TokenManager.deleteFCMToken(any()) }
        coVerify(exactly = 0) { TokenManager.deleteRuStoreToken(any()) }
    }

    /** test_1: deleteDeviceToken(): dispatches to TokenManager.deleteRuStoreToken and invokes completion */
    @Test
    fun deleteDeviceToken_routesToRuStore_andCompletes() = runBlocking {
        coEvery { TokenManager.deleteRuStoreToken(any()) } answers {
            firstArg<(Boolean) -> Unit>().invoke(true)
        }

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.deleteDeviceToken(ctx, RUS_PROVIDER) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { TokenManager.deleteRuStoreToken(any()) }
        coVerify(exactly = 0) { TokenManager.deleteFCMToken(any()) }
        coVerify(exactly = 0) { TokenManager.deleteHMSToken(any(), any()) }
    }

    // ---------------- forcedTokenUpdate ----------------

    /** test_2: forcedTokenUpdate(): deletes current token by provider, then triggers TokenUpdate.tokenUpdate(), then calls complete() */
    @Test
    fun forcedTokenUpdate_deletes_then_updates_then_completes() = runBlocking {
        // Mock the object itself so self-calls are intercepted
        mockkObject(PublicPushTokenFunctions)

        // Call real forcedTokenUpdate
        every {
            PublicPushTokenFunctions.forcedTokenUpdate(
                any(),
                any()
            )
        } answers { callOriginal() }

        // Return a current token with FCM provider
        coEvery { PublicPushTokenFunctions.getPushToken(ctx) } returns DataClasses.TokenData(
            FCM_PROVIDER,
            "tok-123"
        )

        // When deleteDeviceToken(...) is called, immediately invoke its completion
        coEvery {
            PublicPushTokenFunctions.deleteDeviceToken(
                eq(ctx),
                eq(FCM_PROVIDER),
                any()
            )
        } answers {
            thirdArg<() -> Unit>().invoke()
        }

        coEvery { TokenUpdate.tokenUpdate(ctx) } just Runs

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { PublicPushTokenFunctions.getPushToken(ctx) }
        coVerify(exactly = 1) {
            PublicPushTokenFunctions.deleteDeviceToken(
                eq(ctx),
                eq(FCM_PROVIDER),
                any()
            )
        }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /** test_4: forcedTokenUpdate(): when getPushToken() returns null -> no delete, no tokenUpdate, no completion */
    @Test
    fun forcedTokenUpdate_tokenNull_noops() = runBlocking {
        mockkObject(PublicPushTokenFunctions)
        every {
            PublicPushTokenFunctions.forcedTokenUpdate(
                any(),
                any()
            )
        } answers { callOriginal() }

        coEvery { PublicPushTokenFunctions.getPushToken(ctx) } returns null
        coEvery { TokenUpdate.tokenUpdate(ctx) } just Runs

        // A latch that should NOT be counted down
        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        // Wait a bit and assert completion wasn't called
        val completed = latch.await(500, TimeUnit.MILLISECONDS)
        assertTrue(!completed)

        coVerify(exactly = 1) { PublicPushTokenFunctions.getPushToken(ctx) }
        coVerify(exactly = 0) { PublicPushTokenFunctions.deleteDeviceToken(any(), any(), any()) }
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    // ---------------- changePushProviderPriorityList ----------------

    /** test_3: changePushProviderPriorityList(): config exists & providers valid -> update list, refresh cache, trigger tokenUpdate */
    @Test
    fun changePushProviderPriorityList_happyPath_updates_and_triggers() = runBlocking {
        val room = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        every { SDKdb.getDb(ctx) } returns room
        every { room.request() } returns dao

        coEvery {
            dao.getConfig()
        } returns ConfigurationEntity(
            id = 1,
            apiUrl = "https://api",
            rToken = null,
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        every { TokenManager.allProvidersValid(any()) } returns true

        coEvery { dao.updateProviderPriorityList(any()) } just Runs
        coEvery { ConfigSetup.updateConfigCache(ctx) } just Runs
        coEvery { TokenUpdate.tokenUpdate(ctx) } just Runs

        val newPriority = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)

        PublicPushTokenFunctions.changePushProviderPriorityList(ctx, newPriority)

        coVerify(exactly = 1) { dao.updateProviderPriorityList(newPriority) }
        coVerify(exactly = 1) { ConfigSetup.updateConfigCache(ctx) }
        coVerify(exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /** test_5: changePushProviderPriorityList(): invalid providers -> no update, no tokenUpdate */
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
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        every { TokenManager.allProvidersValid(any()) } returns false

        PublicPushTokenFunctions.changePushProviderPriorityList(ctx, listOf("bad-provider"))

        coVerify(exactly = 0) { dao.updateProviderPriorityList(any()) }
        coVerify(exactly = 0) { ConfigSetup.updateConfigCache(any()) }
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** test_5: changePushProviderPriorityList(): no config in DB -> no update, no tokenUpdate */
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
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }
}
