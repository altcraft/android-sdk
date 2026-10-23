@file:Suppress("SpellCheckingInspection")

package test.push.models

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.config.PushConfiguration
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.platform.event.request.Path.eventPushPath
import com.altcraft.sdk.push.platform.subscription.status.request.Path.pushStatusPath
import com.altcraft.sdk.push.platform.subscription.subscribe.request.Path.pushSubscribePath
import com.altcraft.sdk.push.platform.subscription.unsuspend.request.Path.unSuspendPath
import com.altcraft.sdk.push.platform.token.request.Path.tokenUpdatePath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ModelsTest
 *
 * Verifies push model serialization, endpoint paths, and configuration validity.
 */
class ModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** TokenData round-trips through JSON with all fields intact. */
    @Test
    fun tokenData_roundTripsThroughJson() {
        val original = TokenData("android-firebase", "tok-123")

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<TokenData>(encoded)

        assertEquals(original, decoded)
        assertEquals("android-firebase", decoded.provider)
        assertEquals("tok-123", decoded.token)
    }

    /** Push endpoint paths are stable. */
    @Test
    fun paths_areStable() {
        assertEquals("/subscription/push/update/", tokenUpdatePath())
        assertEquals("/subscription/push/status/", pushStatusPath())
        assertEquals("/subscription/push/subscribe/", pushSubscribePath())
        assertEquals("/subscription/push/unsuspend/", unSuspendPath())
        assertEquals("/event/push/delivery", eventPushPath("delivery"))
        assertEquals("/event/push/open", eventPushPath("open"))
    }

    /** Push configuration validates the provider priority list. */
    @Test
    fun pushConfiguration_validatesProviderPriority() {
        assertTrue(
            PushConfiguration(providerPriority = listOf("android-firebase", "android-rustore")).valid
        )
        assertFalse(
            PushConfiguration(providerPriority = listOf("android-firebase", "google")).valid
        )
        assertTrue(PushConfiguration().valid)
    }
}
