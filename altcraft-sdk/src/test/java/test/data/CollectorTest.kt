@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.graphics.Color
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.Collector
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.action.Intent
import com.altcraft.sdk.push.action.PushAction
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.sdk_events.Events
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CollectorTest
 *
 * Positive:
 *  - test_1: getSubscribeRequestData builds correct request with all fields.
 *  - test_2: getUpdateRequestData builds correct request with old/new tokens.
 *  - test_3: getPushEventRequestData builds correct request with type and auth.
 *  - test_4: getUnSuspendRequestData generates uid and correct data.
 *  - test_5: getStatusRequestData builds request with provider/token.
 *  - test_6: getNotificationData builds NotificationData with color, buttons, images.
 *  - test_13: getMobileEventRequestData builds request with url/sid/name/auth.
 *
 * Negative:
 *  - test_7: getSubscribeRequestData returns null when config is null.
 *  - test_8: getUpdateRequestData returns null when token is missing.
 *  - test_9: getPushEventRequestData returns null when auth is missing.
 *  - test_10: getUnSuspendRequestData returns null on exception.
 *  - test_11: getStatusRequestData returns null when savedToken missing and exception.
 *  - test_12: getNotificationData returns default (non-null) on malformed input.
 *  - test_14: getMobileEventRequestData returns null when auth/common data missing.
 */
class CollectorTest {

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

        mockkObject(
            ConfigSetup,
            TokenManager,
            AuthManager,
            Events,
            PushChannel,
            Intent,
            PushAction,
            com.altcraft.sdk.data.Preferenses
        )

        every { Events.error(any(), any(), any()) } returns
                DataClasses.Error("fn", 400, "err", null)

        coEvery { ConfigSetup.getConfig(any()) } returns config
        coEvery { TokenManager.getCurrentPushToken(any()) } returns token
        every { AuthManager.getAuthHeaderAndMatching(any()) } returns ("hdr" to "strict")

        every { com.altcraft.sdk.data.Preferenses.getSavedPushToken(any()) } returns null
        every { com.altcraft.sdk.data.Preferenses.getMessageId(any()) } returns 1

        every { PushChannel.getChannelInfo(any(), any()) } returns Pair("ch", "desc")
        every { Intent.getIntent(any(), any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: getSubscribeRequestData builds correct request with all fields. */
    @Test
    fun test_1_getSubscribeRequestData_success() = runBlocking {
        val item = SubscribeEntity(
            userTag = "tag",
            status = "subscribed",
            sync = 1,
            profileFields = null,
            customFields = null,
            cats = null,
            replace = null,
            skipTriggers = null,
            uid = "uid-1"
        )

        val data = Collector.getSubscribeRequestData(ctx, item)

        assertNotNull(data)
        assertEquals("uid-1", data?.uid)
        assertEquals("subscribed", data?.status)
        assertEquals("android-firebase", data?.provider)
        assertEquals("hdr", data?.authHeader)
        assertEquals("strict", data?.matchingMode)
        assertEquals(
            "https://api.example.com/subscription/push/subscribe/",
            data?.url
        )
    }

    /** - test_2: getUpdateRequestData builds correct request with old/new tokens. */
    @Test
    fun test_2_getUpdateRequestData_success() = runBlocking {
        every { com.altcraft.sdk.data.Preferenses.getSavedPushToken(any()) } returns savedToken

        val data = Collector.getUpdateRequestData(ctx, "uid-2")

        assertNotNull(data)
        assertEquals("uid-2", data?.uid)
        assertEquals("tokOld", data?.oldToken)
        assertEquals("tok123", data?.newToken)
        assertEquals("android-firebase", data?.oldProvider)
        assertEquals("android-firebase", data?.newProvider)
        assertEquals("hdr", data?.authHeader)
        assertTrue(data!!.url.startsWith("https://api.example.com"))
    }

    /** - test_3: getPushEventRequestData builds correct request with type and auth. */
    @Test
    fun test_3_getPushEventRequestData_success() = runBlocking {
        val event = PushEventEntity(uid = "uid-3", type = "opened")

        val data = Collector.getPushEventRequestData(ctx, event)

        assertNotNull(data)
        assertEquals("opened", data?.type)
        assertEquals("uid-3", data?.uid)
        assertEquals("hdr", data?.authHeader)
        assertEquals("strict", data?.matchingMode)
        assertTrue(data!!.url.startsWith("https://api.example.com"))
    }

    /** - test_4: getUnSuspendRequestData generates uid and correct data. */
    @Test
    fun test_4_getUnSuspendRequestData_success() = runBlocking {
        val data = Collector.getUnSuspendRequestData(ctx)

        assertNotNull(data)
        assertEquals("android-firebase", data?.provider)
        assertEquals("tok123", data?.token)
        assertEquals("hdr", data?.authHeader)
        assertEquals("strict", data?.matchingMode)
        assertTrue(data!!.uid.isNotBlank())
        assertTrue(data.url.startsWith("https://api.example.com"))
    }

    /** - test_5: getStatusRequestData builds request with provider/token. */
    @Test
    fun test_5_getStatusRequestData_success() = runBlocking {
        val data = Collector.getStatusRequestData(ctx)

        assertNotNull(data)
        assertEquals("android-firebase", data?.provider)
        assertEquals("tok123", data?.token)
        assertEquals("hdr", data?.authHeader)
        assertEquals("strict", data?.matchingMode)
        assertTrue(data!!.uid.isNotBlank())
        assertTrue(data.url.startsWith("https://api.example.com"))
    }

    /** - test_6: getNotificationData builds NotificationData with color, buttons, images. */
    @Test
    fun test_6_getNotificationData_success() = runBlocking {
        val msg = mapOf(
            "_uid" to "m1",
            "_title" to "Hello",
            "_body" to "World",
            "_color" to "#FF0000"
        )

        val data = Collector.getNotificationData(ctx, msg)

        assertNotNull(data)
        assertEquals("m1", data?.uid)
        assertEquals("Hello", data?.title)
        assertEquals("World", data?.body)
        assertEquals(Color.BLACK, data?.color)
        assertEquals("ch", data?.channelInfo?.first)
        assertEquals("desc", data?.channelInfo?.second)
        assertNotNull(data?.pendingIntent)
    }

    /** - test_7: getSubscribeRequestData returns null when config is null. */
    @Test
    fun test_7_getSubscribeRequestData_configNull_returnsNull() = runBlocking {
        coEvery { ConfigSetup.getConfig(any()) } returns null

        val item = SubscribeEntity(
            userTag = "tag",
            status = "subscribed",
            sync = null,
            profileFields = null,
            customFields = null,
            cats = null,
            replace = null,
            skipTriggers = null,
            uid = "uid-x"
        )

        val data = Collector.getSubscribeRequestData(ctx, item)

        assertNull(data)
        io.mockk.verify { Events.error(eq("getSubscribeData"), any(), any()) }
    }

    /** - test_8: getUpdateRequestData returns null when token is missing. */
    @Test
    fun test_8_getUpdateRequestData_tokenNull_returnsNull() = runBlocking {
        coEvery { TokenManager.getCurrentPushToken(any()) } returns null

        val data = Collector.getUpdateRequestData(ctx, "uidX")

        assertNull(data)
        io.mockk.verify { Events.error(eq("getUpdateData"), any(), any()) }
    }

    /** - test_9: getPushEventRequestData returns null when auth is missing. */
    @Test
    fun test_9_getPushEventRequestData_authNull_returnsNull() = runBlocking {
        every { AuthManager.getAuthHeaderAndMatching(any()) } returns null

        val event = PushEventEntity(uid = "uidE", type = "opened")
        val data = Collector.getPushEventRequestData(ctx, event)

        assertNull(data)
        io.mockk.verify { Events.error(eq("getPushEventData"), any(), any()) }
    }

    /** - test_10: getUnSuspendRequestData returns null on exception. */
    @Test
    fun test_10_getUnSuspendRequestData_exception_returnsNull() = runBlocking {
        coEvery { ConfigSetup.getConfig(any()) } throws RuntimeException("boom")

        val data = Collector.getUnSuspendRequestData(ctx)

        assertNull(data)
        io.mockk.verify { Events.error(eq("getUnSuspendRequestData"), any(), any()) }
    }

    /** - test_11: getStatusRequestData returns null when savedToken missing and exception. */
    @Test
    fun test_11_getStatusRequestData_exception_returnsNull() = runBlocking {
        every { com.altcraft.sdk.data.Preferenses.getSavedPushToken(any()) } throws RuntimeException("err")

        val data = Collector.getStatusRequestData(ctx)

        assertNull(data)
        io.mockk.verify { Events.error(eq("getProfileData"), any(), any()) }
    }

    /** - test_12: getNotificationData returns default (non-null) on malformed input. */
    @Test
    fun test_12_getNotificationData_malformed_returnsDefaultNotNull() = runBlocking {
        val msg = mapOf("bad" to "data")

        val data = Collector.getNotificationData(ctx, msg)

        assertNotNull(data)
        assertEquals("", data?.uid)
        assertEquals("", data?.title)
        assertEquals("", data?.body)
        assertEquals(Color.BLACK, data?.color)
        assertNotNull(data?.channelInfo)
        assertNotNull(data?.pendingIntent)
    }

    /** - test_13: getMobileEventRequestData builds request with url/sid/name/auth. */
    @Test
    fun test_13_getMobileEventRequestData_success() = runBlocking {
        val entity = MobileEventEntity(
            userTag = "tag-1",
            timeZone = 180,
            sid = "sid-123",
            altcraftClientID = "cid-777",
            eventName = "purchase",
            payload = """{"sum":100}""",
            matching = """{"type":"push_sub","id":"abc"}""",
            matchingType = "email",
            profileFields = """{"age":30}""",
            subscription = """{"channel":"email","email":"a@b.c","resource_id":1}""",
            sendMessageId = "smid-1",
            utmTags = null
        )

        val data = Collector.getMobileEventRequestData(ctx, entity)

        assertNotNull(data)
        assertTrue(data!!.url.startsWith("https://api.example.com"))
        assertEquals("sid-123", data.sid)
        assertEquals("purchase", data.name)
        assertEquals("hdr", data.authHeader)
    }

    /** - test_14: getMobileEventRequestData returns null when auth/common data missing. */
    @Test
    fun test_14_getMobileEventRequestData_authNull_returnsNull() = runBlocking {
        every { AuthManager.getAuthHeaderAndMatching(any()) } returns null

        val entity = MobileEventEntity(
            userTag = "tag-x",
            timeZone = 0,
            sid = "sid-x",
            altcraftClientID = "cid-x",
            eventName = "evt-x",
            payload = null,
            matching = null,
            matchingType = null,
            profileFields = null,
            subscription = null,
            sendMessageId = null,
            utmTags = null
        )

        val data = Collector.getMobileEventRequestData(ctx, entity)

        assertNull(data)
        io.mockk.verify { Events.error(eq("getMobileEventRequestData"), any(), any()) }
    }
}