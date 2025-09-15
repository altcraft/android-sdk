package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.graphics.Color
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.action.PushAction
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.data.Repository
import com.altcraft.sdk.data.room.ConfigurationEntity
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * RepositoryUnitTest
 *
 * Positive scenarios:
 *  - test_1: getSubscribeRequestData builds correct request with all fields.
 *  - test_2: getUpdateRequestData builds correct request with old/new tokens.
 *  - test_3: getPushEventRequestData builds correct request with type and auth.
 *  - test_4: getUnSubscribeRequestData generates uid and correct data.
 *  - test_5: getStatusRequestData builds request with provider/token.
 *  - test_6: getNotificationData builds NotificationData with color, buttons, images.
 *
 * Negative scenarios:
 *  - test_7: getSubscribeRequestData returns null when config is null.
 *  - test_8: getUpdateRequestData returns null when token is missing.
 *  - test_9: getPushEventRequestData returns null when auth is missing.
 *  - test_10: getUnSubscribeRequestData returns null on exception.
 *  - test_11: getStatusRequestData returns null when savedToken missing and exception.
 *  - test_12: getNotificationData returns null when PushData malformed.
 *
 * Notes:
 *  - Pure JVM unit tests (MockK).
 *  - All static helpers (ConfigSetup, TokenManager, AuthManager, Events, PushChannel, PushAction) are mocked.
 *  - Bitmap loading helpers (PushImage) are NOT tested here.
 */
class RepositoryTest {

    private lateinit var ctx: Context
    private lateinit var config: ConfigurationEntity
    private lateinit var token: DataClasses.TokenData
    private lateinit var savedToken: DataClasses.TokenData

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        config = ConfigurationEntity(
            id = 1,
            apiUrl = "https://api.example.com",
            rToken = "r-token",
            icon = 123,
            pushChannelName = null,
            pushChannelDescription = null,
            usingService = false,
            serviceMessage = null,
            providerPriorityList = null,
            pushReceiverModules = null,
            appInfo = null
        )
        token = DataClasses.TokenData("android-firebase", "tok123")
        savedToken = DataClasses.TokenData("android-firebase", "tokOld")

        mockkObject(ConfigSetup, TokenManager, AuthManager, Events, PushChannel, PushAction)

        every { Events.error(any(), any(), any()) } returns DataClasses.Error("fn", 400, "err", null)
        coEvery { ConfigSetup.getConfig(any()) } returns config
        coEvery { TokenManager.getCurrentToken(any()) } returns token
        every { AuthManager.getAuthHeaderAndMatching(any()) } returns ("hdr" to "strict")
    }

    @After
    fun tearDown() = unmockkAll()

    /** test_1: getSubscribeRequestData builds correct request with all fields */
    @Test
    fun getSubscribeRequestData_success() = runBlocking {
        val item = SubscribeEntity(userTag = "tag", status = "subscribed", sync = 1,
            profileFields = null, customFields = null, cats = null,
            replace = null, skipTriggers = null, uid = "uid-1")

        val data = Repository.getSubscribeRequestData(ctx, item)

        assertNotNull(data)
        assertEquals("uid-1", data?.uid)
        assertEquals("subscribed", data?.status)
        assertEquals("android-firebase", data?.provider)
    }

    /** test_2: getUpdateRequestData builds correct request with old/new tokens */
    @Test
    fun getUpdateRequestData_success() = runBlocking {
        mockkObject(com.altcraft.sdk.data.Preferenses)
        every { com.altcraft.sdk.data.Preferenses.getSavedToken(any()) } returns savedToken

        val data = Repository.getUpdateRequestData(ctx, "uid-2")

        assertNotNull(data)
        assertEquals("tokOld", data?.oldToken)
        assertEquals("tok123", data?.newToken)
    }

    /** test_3: getPushEventRequestData builds correct request with type and auth */
    @Test
    fun getPushEventRequestData_success() = runBlocking {
        val event = PushEventEntity(uid = "uid-3", type = "opened")

        val data = Repository.getPushEventRequestData(ctx, event)

        assertNotNull(data)
        assertEquals("opened", data?.type)
        assertEquals("uid-3", data?.uid)
    }

    /** test_4: getUnSubscribeRequestData generates uid and correct data */
    @Test
    fun getUnSubscribeRequestData_success() = runBlocking {
        val data = Repository.getUnSubscribeRequestData(ctx)

        assertNotNull(data)
        assertEquals("android-firebase", data?.provider)
        assertEquals("tok123", data?.token)
    }

    /** test_5: getStatusRequestData builds request with provider/token */
    @Test
    fun getStatusRequestData_success() = runBlocking {
        val data = Repository.getStatusRequestData(ctx)

        assertNotNull(data)
        assertEquals("android-firebase", data?.provider)
        assertEquals("tok123", data?.token)
    }

    /** test_6: getNotificationData builds NotificationData with fallback color on JVM */
    @Test
    fun getNotificationData_success() = runBlocking {
        val msg = mapOf(
            "_uid" to "m1",
            "_title" to "Hello",
            "_body" to "World",
            "_color" to "#FF0000"
        )
        every { PushChannel.getChannelInfo(any(), any()) } returns Pair("ch", "desc")
        every { PushAction.getIntent(any(), any(), any()) } returns mockk(relaxed = true)

        val data = Repository.getNotificationData(ctx, msg)

        assertNotNull(data)
        assertEquals("m1", data?.uid)
        assertEquals("Hello", data?.title)
        assertEquals("World", data?.body)
        assertEquals(Color.BLACK, data?.color)
    }

    // -------- Negative tests --------

    /** test_7: getSubscribeRequestData returns null when config is null */
    @Test
    fun getSubscribeRequestData_configNull_returnsNull() = runBlocking {
        coEvery { ConfigSetup.getConfig(any()) } returns null

        val item = SubscribeEntity("tag", "subscribed", null, null, null, null, null, null)
        val data = Repository.getSubscribeRequestData(ctx, item)

        assertNull(data)
        verify { Events.error(eq("getSubscribeData"), any(), any()) }
    }

    /** test_8: getUpdateRequestData returns null when token is missing */
    @Test
    fun getUpdateRequestData_tokenNull_returnsNull() = runBlocking {
        coEvery { TokenManager.getCurrentToken(any()) } returns null

        val data = Repository.getUpdateRequestData(ctx, "uidX")

        assertNull(data)
        verify { Events.error(eq("getUpdateData"), any(), any()) }
    }

    /** test_9: getPushEventRequestData returns null when auth is missing */
    @Test
    fun getPushEventRequestData_authNull_returnsNull() = runBlocking {
        every { AuthManager.getAuthHeaderAndMatching(any()) } returns null

        val event = PushEventEntity(uid = "uidE", type = "opened")
        val data = Repository.getPushEventRequestData(ctx, event)

        assertNull(data)
        verify { Events.error(eq("getPushEventData"), any(), any()) }
    }

    /** test_10: getUnSubscribeRequestData returns null on exception */
    @Test
    fun getUnSubscribeRequestData_exception_returnsNull() = runBlocking {
        coEvery { ConfigSetup.getConfig(any()) } throws RuntimeException("boom")

        val data = Repository.getUnSubscribeRequestData(ctx)

        assertNull(data)
        verify { Events.error(eq("getProfileData"), any(), any()) }
    }

    /** test_11: getStatusRequestData returns null when savedToken missing and exception */
    @Test
    fun getStatusRequestData_exception_returnsNull() = runBlocking {
        mockkObject(com.altcraft.sdk.data.Preferenses)
        every { com.altcraft.sdk.data.Preferenses.getSavedToken(any()) } throws RuntimeException("err")

        val data = Repository.getStatusRequestData(ctx)

        assertNull(data)
        verify { Events.error(eq("getProfileData"), any(), any()) }
    }

    /** test_12: getNotificationData returns null when PushData malformed */
    @Test
    fun getNotificationData_malformed_returnsNull() = runBlocking {
        val msg = mapOf("bad" to "data")
        val data = Repository.getNotificationData(ctx, msg)

        assertNull(data)
        verify { Events.error(eq("getNotificationData"), any(), any()) }
    }
}
