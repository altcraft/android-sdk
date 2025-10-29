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
 *  - test_1: deleteDeviceToken() routes to FCM and invokes completion callback
 *  - test_2: deleteDeviceToken() routes to HMS and invokes completion callback
 *  - test_3: deleteDeviceToken() routes to RuStore and invokes completion callback
 *  - test_4: forcedTokenUpdate() deletes current token, triggers TokenUpdate.tokenUpdate(), then calls completion
 *  - test_5: changePushProviderPriorityList() config exists & providers valid -> updates list, refreshes cache, triggers tokenUpdate
 *
 * Negative scenarios:
 *  - test_6: forcedTokenUpdate() when getPushToken() returns null -> no delete, no tokenUpdate, no completion
 *  - test_7: changePushProviderPriorityList() invalid providers -> no update, no tokenUpdate
 *  - test_8: changePushProviderPriorityList() no config in DB -> no update, no tokenUpdate
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
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Log::class)
    }

    /** - test_1: deleteDeviceToken routes to FCM and invokes completion callback. */
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

    /** - test_2: deleteDeviceToken routes to HMS and invokes completion callback. */
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

    /** - test_3: deleteDeviceToken routes to RuStore and invokes completion callback. */
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

    /** - test_4: forcedTokenUpdate deletes current token by provider, triggers TokenUpdate.tokenUpdate, then calls completion. */
    @Test
    fun forcedTokenUpdate_deletes_then_updates_then_completes() = runBlocking {
        mockkObject(PublicPushTokenFunctions)

        every {
            PublicPushTokenFunctions.forcedTokenUpdate(any(), any())
        } answers { callOriginal() }

        coEvery { PublicPushTokenFunctions.getPushToken(ctx) } returns DataClasses.TokenData(
            FCM_PROVIDER, "tok-123"
        )

        coEvery {
            PublicPushTokenFunctions.deleteDeviceToken(eq(ctx), eq(FCM_PROVIDER), any())
        } answers {
            thirdArg<() -> Unit>().invoke()
        }

        coEvery { TokenUpdate.tokenUpdate(ctx) } just Runs

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        coVerify(exactly = 1) { PublicPushTokenFunctions.getPushToken(ctx) }
        coVerify(exactly = 1) {
            PublicPushTokenFunctions.deleteDeviceToken(eq(ctx), eq(FCM_PROVIDER), any())
        }
        coVerify(timeout = TIMEOUT_MS, exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    /** - test_5: forcedTokenUpdate with null getPushToken → no delete, no tokenUpdate, no completion. */
    @Test
    fun forcedTokenUpdate_tokenNull_noops() = runBlocking {
        mockkObject(PublicPushTokenFunctions)
        every {
            PublicPushTokenFunctions.forcedTokenUpdate(any(), any())
        } answers { callOriginal() }

        coEvery { PublicPushTokenFunctions.getPushToken(ctx) } returns null
        coEvery { TokenUpdate.tokenUpdate(ctx) } just Runs

        val latch = CountDownLatch(1)

        PublicPushTokenFunctions.forcedTokenUpdate(ctx) { latch.countDown() }

        val completed = latch.await(500, TimeUnit.MILLISECONDS)
        assertTrue(!completed)

        coVerify(exactly = 1) { PublicPushTokenFunctions.getPushToken(ctx) }
        coVerify(exactly = 0) { PublicPushTokenFunctions.deleteDeviceToken(any(), any(), any()) }
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    // ---------------- changePushProviderPriorityList ----------------

    /** - test_6: changePushProviderPriorityList happy-path → update list, refresh cache, trigger tokenUpdate. */
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

    /** - test_7: changePushProviderPriorityList with invalid providers → no update, no tokenUpdate. */
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

    /** - test_8: changePushProviderPriorityList with no config in DB → no update, no tokenUpdate. */
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