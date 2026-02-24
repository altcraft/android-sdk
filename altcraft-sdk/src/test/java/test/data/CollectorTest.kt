// Created by Andrey Pogodin.
//
// Copyright © 2025 Altcraft. All rights reserved.

@file:OptIn(ExperimentalCoroutinesApi::class)

package test.data

import android.content.Context
import com.altcraft.altcraftsdk.R
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Collector
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.ProfileUpdateEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.PushImage
import com.altcraft.sdk.push.token.TokenManager
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * CollectorTest
 *
 * Positive scenarios:
 * - test_1: getSubscribeRequestData builds request data from Environment and SubscribeEntity.
 * - test_2: getTokenUpdateRequestData builds request data and sync=true when rToken is not empty.
 * - test_3: getTokenUpdateRequestData builds request data and sync=false when rToken is empty.
 * - test_4: getPushEventRequestData builds request data from Environment and PushEventEntity.
 * - test_5: getMobileEventRequestData builds request data from Environment and MobileEventEntity.
 * - test_6: getUnSuspendRequestData builds request data from Environment.
 * - test_7: getStatusRequestData uses saved token when it exists.
 * - test_8: getStatusRequestData uses current token when saved token is absent.
 * - test_9: getProfileUpdateRequestData builds request data from Environment and ProfileUpdateEntity.
 * - test_10: getNotificationData builds notification data and uses defaults when config icon is null.
 */
class CollectorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = mockk(relaxed = true)
        mockkObject(Environment.Companion)
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    private fun tokenData(provider: String, token: String): DataClasses.TokenData {
        return DataClasses.TokenData(provider = provider, token = token)
    }

    private fun configEntity(
        icon: Int? = null,
        apiUrl: String = "https://api.example.com",
        rToken: String? = null
    ): ConfigurationEntity {
        return ConfigurationEntity(
            id = 1,
            icon = icon,
            apiUrl = apiUrl,
            rToken = rToken,
            appInfo = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )
    }

    private fun mockEnv(
        apiUrl: String = "https://api.example.com",
        rToken: String? = "rtoken-123",
        authHeader: String = "Bearer auth-xyz",
        matchingMode: String = "strict",
        provider: String = "fcm",
        token: String = "token-abc",
        savedToken: DataClasses.TokenData? = null
    ): Environment {
        val env = mockk<Environment>(relaxed = true)

        every { Environment.create(any()) } returns env

        val cfg = configEntity(apiUrl = apiUrl, rToken = rToken)
        coEvery { env.config() } returns cfg
        coEvery { env.auth() } returns Pair(authHeader, matchingMode)
        coEvery { env.token() } returns tokenData(provider, token)

        every { env.savedToken } returns savedToken

        return env
    }

    /** - test_1: getSubscribeRequestData builds request data from Environment and SubscribeEntity. */
    @Test
    fun test_1_getSubscribeRequestData_buildsCorrectData() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            rToken = "rt-1",
            authHeader = "Bearer A",
            matchingMode = "mm",
            provider = "fcm",
            token = "t1"
        )

        val entity = mockk<SubscribeEntity>(relaxed = true)
        every { entity.requestID } returns "req-1"
        every { entity.time } returns 123456789L
        every { entity.status } returns "active"
        every { entity.sync } returns 1
        every { entity.profileFields } returns null
        every { entity.customFields } returns null
        every { entity.cats } returns null
        every { entity.replace } returns true
        every { entity.skipTriggers } returns true

        val data = Collector.getSubscribeRequestData(context, entity)

        assertNotNull(data)
        assertEquals("req-1", data.requestId)
        assertEquals(123456789L, data.time)
        assertEquals("rt-1", data.rToken)
        assertEquals("Bearer A", data.authHeader)
        assertEquals("mm", data.matchingMode)
        assertEquals("fcm", data.provider)
        assertEquals("t1", data.deviceToken)
        assertEquals("active", data.status)
        assertEquals(true, data.sync)
        assertEquals(true, data.replace)
        assertEquals(true, data.skipTriggers)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_2: getTokenUpdateRequestData builds request data and sync=true when rToken is not empty. */
    @Test
    fun test_2_getTokenUpdateRequestData_syncTrue_whenRTokenNotEmpty() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            rToken = "rt-1",
            authHeader = "Bearer A",
            provider = "fcm",
            token = "new-token",
            savedToken = tokenData("fcm", "old-token")
        )

        val data = Collector.getTokenUpdateRequestData(context, requestId = "req-2")

        assertNotNull(data)
        assertEquals("req-2", data.requestId)
        assertEquals("old-token", data.oldToken)
        assertEquals("new-token", data.newToken)
        assertEquals("fcm", data.oldProvider)
        assertEquals("fcm", data.newProvider)
        assertEquals("Bearer A", data.authHeader)
        assertEquals(true, data.sync)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_3: getTokenUpdateRequestData builds request data and sync=false when rToken is empty. */
    @Test
    fun test_3_getTokenUpdateRequestData_syncFalse_whenRTokenEmpty() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            rToken = "",
            authHeader = "Bearer A",
            provider = "fcm",
            token = "new-token",
            savedToken = null
        )

        val data = Collector.getTokenUpdateRequestData(context, requestId = "req-3")

        assertNotNull(data)
        assertEquals(false, data.sync)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_4: getPushEventRequestData builds request data from Environment and PushEventEntity. */
    @Test
    fun test_4_getPushEventRequestData_buildsCorrectData() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A",
            matchingMode = "mm"
        )

        val entity = mockk<PushEventEntity>(relaxed = true)
        every { entity.requestID } returns "req-4"
        every { entity.uid } returns "uid-1"
        every { entity.time } returns 111L
        every { entity.type } returns "open"

        val data = Collector.getPushEventRequestData(context, entity)

        assertNotNull(data)
        assertEquals("req-4", data.requestId)
        assertEquals("uid-1", data.uid)
        assertEquals(111L, data.time)
        assertEquals("open", data.type)
        assertEquals("Bearer A", data.authHeader)
        assertEquals("mm", data.matchingMode)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_5: getMobileEventRequestData builds request data from Environment and MobileEventEntity. */
    @Test
    fun test_5_getMobileEventRequestData_buildsCorrectData() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A"
        )

        val entity = mockk<MobileEventEntity>(relaxed = true)
        every { entity.requestID } returns "req-5"
        every { entity.sid } returns "sid-1"
        every { entity.eventName } returns "purchase"

        val data = Collector.getMobileEventRequestData(context, entity)

        assertNotNull(data)
        assertEquals("req-5", data.requestId)
        assertEquals("sid-1", data.sid)
        assertEquals("purchase", data.name)
        assertEquals("Bearer A", data.authHeader)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_6: getUnSuspendRequestData builds request data from Environment. */
    @Test
    fun test_6_getUnSuspendRequestData_buildsCorrectData() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A",
            matchingMode = "mm",
            provider = "fcm",
            token = "t1"
        )

        val data = Collector.getUnSuspendRequestData(context)

        assertNotNull(data)
        assertEquals("fcm", data.provider)
        assertEquals("t1", data.token)
        assertEquals("Bearer A", data.authHeader)
        assertEquals("mm", data.matchingMode)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_7: getStatusRequestData uses saved token when it exists. */
    @Test
    fun test_7_getStatusRequestData_usesSavedToken_whenExists() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A",
            matchingMode = "mm",
            savedToken = tokenData("fcm", "saved-token")
        )

        mockkObject(TokenManager)
        coEvery { TokenManager.getCurrentPushToken(any()) } returns tokenData("fcm", "current-token")

        val data = Collector.getStatusRequestData(context)

        assertNotNull(data)
        assertEquals("fcm", data.provider)
        assertEquals("saved-token", data.token)
        assertEquals("Bearer A", data.authHeader)
        assertEquals("mm", data.matchingMode)

        verify(exactly = 1) { Environment.create(any()) }
        coVerify(exactly = 1) { TokenManager.getCurrentPushToken(any()) }
    }

    /** - test_8: getStatusRequestData uses current token when saved token is absent. */
    @Test
    fun test_8_getStatusRequestData_usesCurrentToken_whenSavedAbsent() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A",
            matchingMode = "mm",
            savedToken = null
        )

        mockkObject(TokenManager)
        coEvery { TokenManager.getCurrentPushToken(any()) } returns tokenData("hms", "current-token")

        val data = Collector.getStatusRequestData(context)

        assertNotNull(data)
        assertEquals("hms", data.provider)
        assertEquals("current-token", data.token)
        assertEquals("Bearer A", data.authHeader)
        assertEquals("mm", data.matchingMode)

        verify(exactly = 1) { Environment.create(any()) }
        coVerify(exactly = 1) { TokenManager.getCurrentPushToken(any()) }
    }

    /** - test_9: getProfileUpdateRequestData builds request data from Environment and ProfileUpdateEntity. */
    @Test
    fun test_9_getProfileUpdateRequestData_buildsCorrectData() = runTest {
        mockEnv(
            apiUrl = "https://api.example.com",
            authHeader = "Bearer A"
        )

        val entity = mockk<ProfileUpdateEntity>(relaxed = true)
        every { entity.requestID } returns "req-9"
        every { entity.profileFields } returns null
        every { entity.skipTriggers } returns true

        val data = Collector.getProfileUpdateRequestData(context, entity)

        assertNotNull(data)
        assertEquals("req-9", data.requestId)
        assertEquals("Bearer A", data.authHeader)
        assertEquals(true, data.skipTriggers)

        verify(exactly = 1) { Environment.create(any()) }
    }

    /** - test_10: getNotificationData builds notification data and uses defaults when config icon is null. */
    @Test
    fun test_10_getNotificationData_buildsCorrectData_withDefaultIcon() = runTest {
        mockkObject(ConfigSetup)
        mockkObject(PushImage)
        mockkObject(PushChannel)
        mockkObject(SubFunction)
        mockkObject(Preferenses)

        val message = mapOf(
            "uid" to "u1",
            "title" to "Hello",
            "body" to "World",
            "url" to "https://example.com",
            "color" to "#FF0000",
            "extra" to "x-1"
        )

        val pushData = com.altcraft.sdk.push.PushData(message)

        val cfg = configEntity(icon = null, apiUrl = "https://api.example.com", rToken = null)
        coEvery { ConfigSetup.getConfig(any()) } returns cfg

        coEvery { PushImage.loadSmallImage(any(), any()) } returns null
        coEvery { PushImage.loadLargeImage(any(), any()) } returns null

        val channelInfo: Pair<String, String> = "channel" to "desc"
        every { PushChannel.getChannelInfo(any(), any()) } returns channelInfo

        every { SubFunction.getIconColor(any()) } returns 123
        every { Preferenses.getMessageId(any()) } returns 77

        val data = Collector.getNotificationData(context, message)

        assertNotNull(data)

        assertEquals(pushData.uid, data.uid)
        assertEquals(pushData.title, data.title)
        assertEquals(pushData.body, data.body)

        assertEquals(R.drawable.icon, data.icon)
        assertEquals(77, data.messageId)
        assertEquals(channelInfo, data.channelInfo)
        assertEquals(null, data.smallImg)
        assertEquals(null, data.largeImage)
        assertEquals(123, data.color)

        assertEquals(pushData.url, data.url)
        assertEquals(pushData.extra, data.extra)

        assertEquals(pushData.buttons, data.buttons)

        coEvery { ConfigSetup.getConfig(any()) } returns cfg
        coEvery { PushImage.loadSmallImage(any(), any()) } returns null
        coEvery { PushImage.loadLargeImage(any(), any()) } returns null
    }
}
