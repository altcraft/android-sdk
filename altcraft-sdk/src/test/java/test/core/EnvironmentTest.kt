package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.push.token.TokenManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * RoomEntitiesTest
 *
 * Positive scenarios:
 *  - test_1: ConfigurationEntity.isValid() returns true when apiUrl is not empty and providers are valid.
 *  - test_2: ConfigurationEntity.isValid() calls allProvidersValid with providerPriorityList.
 *  - test_4: SubscribeEntity sets default retryCount and maxRetryCount correctly.
 *  - test_5: PushEventEntity sets default retryCount and maxRetryCount correctly.
 *  - test_6: MobileEventEntity sets default retryCount and maxRetryCount correctly.
 *  - test_7: SubscribeEntity generates non-empty uid when not provided.
 *
 * Negative scenarios:
 *  - test_3: ConfigurationEntity.isValid() returns false when apiUrl is empty.
 */
class RoomEntitiesTest {

    @Before
    fun setUp() {
        mockkObject(TokenManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: ConfigurationEntity.isValid() returns true when apiUrl is not empty and providers are valid. */
    @Test
    fun test_1_configuration_isValid_true_whenApiUrlNotEmpty_andProvidersValid() {
        val providerPriority = listOf("android-firebase", "android-huawei")
        every { TokenManager.allProvidersValid(providerPriority) } returns true

        val entity = ConfigurationEntity(
            id = 1,
            icon = null,
            apiUrl = "https://api.example.com",
            rToken = "r-token",
            appInfo = DataClasses.AppInfo("app", "1.0.0", "pkg"),
            usingService = true,
            serviceMessage = null,
            pushReceiverModules = listOf("fcm"),
            providerPriorityList = providerPriority,
            pushChannelName = "name",
            pushChannelDescription = "desc"
        )

        assertTrue(entity.isValid())
    }

    /** test_2: ConfigurationEntity.isValid() calls allProvidersValid with providerPriorityList. */
    @Test
    fun test_2_configuration_isValid_callsAllProvidersValid_withProviderPriorityList() {
        val providerPriority = listOf("FCM", "HMS")
        every { TokenManager.allProvidersValid(providerPriority) } returns true

        val entity = ConfigurationEntity(
            id = 2,
            apiUrl = "https://api.example.com",
            rToken = null,
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = providerPriority,
            pushChannelName = null,
            pushChannelDescription = null
        )

        entity.isValid()

        verify(exactly = 1) { TokenManager.allProvidersValid(providerPriority) }
    }

    /** test_3: ConfigurationEntity.isValid() returns false when apiUrl is empty. */
    @Test
    fun test_3_configuration_isValid_false_whenApiUrlEmpty() {
        val providerPriority = listOf("FCM")
        every { TokenManager.allProvidersValid(providerPriority) } returns true

        val entity = ConfigurationEntity(
            id = 3,
            apiUrl = "",
            rToken = null,
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = providerPriority,
            pushChannelName = null,
            pushChannelDescription = null
        )

        assertFalse(entity.isValid())
    }

    /** test_4: SubscribeEntity sets default retryCount and maxRetryCount correctly. */
    @Test
    fun test_4_subscribeEntity_defaultRetryCounts_areSetFromConstants() {
        val entity = SubscribeEntity(
            userTag = "user",
            status = "SUBSCRIBED",
            sync = 1,
            profileFields = JsonNull,
            customFields = JsonNull,
            cats = emptyList(),
            replace = true,
            skipTriggers = false
        )

        assertEquals(START_RETRY_COUNT, entity.retryCount)
        assertEquals(MAX_RETRY_COUNT, entity.maxRetryCount)
    }

    /** test_5: PushEventEntity sets default retryCount and maxRetryCount correctly. */
    @Test
    fun test_5_pushEventEntity_defaultRetryCounts_areSetFromConstants() {
        val entity = PushEventEntity(
            uid = UUID.randomUUID().toString(),
            type = "open"
        )

        assertEquals(START_RETRY_COUNT, entity.retryCount)
        assertEquals(MAX_RETRY_COUNT, entity.maxRetryCount)
    }

    /** test_6: MobileEventEntity sets default retryCount and maxRetryCount correctly. */
    @Test
    fun test_6_mobileEventEntity_defaultRetryCounts_areSetFromConstants() {
        val entity = MobileEventEntity(
            userTag = "user",
            timeZone = 3,
            sid = "sid",
            altcraftClientID = "client",
            eventName = "event",
            payload = null,
            matching = null,
            matchingType = null,
            profileFields = null,
            subscription = null,
            sendMessageId = null,
            utmTags = null
        )

        assertEquals(START_RETRY_COUNT, entity.retryCount)
        assertEquals(MAX_RETRY_COUNT, entity.maxRetryCount)
    }

    /** test_7: SubscribeEntity generates non-empty uid when not provided explicitly. */
    @Test
    fun test_7_subscribeEntity_generatesNonEmptyUid_whenNotProvided() {
        val entity = SubscribeEntity(
            userTag = "user",
            status = "SUBSCRIBED",
            sync = 1,
            profileFields = null,
            customFields = null,
            cats = null,
            replace = null,
            skipTriggers = null
        )

        assertTrue(entity.uid.isNotBlank())
    }
}
