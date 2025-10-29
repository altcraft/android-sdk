@file:Suppress("SpellCheckingInspection", "unused")

package test.json.serializer

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.EMAIL_CHANNEL
import com.altcraft.sdk.data.Constants.PUSH_CHANNEL
import com.altcraft.sdk.data.Constants.SMS_CHANNEL
import com.altcraft.sdk.data.DataClasses.CcDataSubscription
import com.altcraft.sdk.data.DataClasses.EmailSubscription
import com.altcraft.sdk.data.DataClasses.PushSubscription
import com.altcraft.sdk.data.DataClasses.SmsSubscription
import com.altcraft.sdk.data.DataClasses.Subscription
import com.altcraft.sdk.json.serializer.any.AnySerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert.*
import org.junit.Test

/**
 * SubscriptionSerializerTest
 *
 * test_1: deserialize email by channel.
 * test_2: deserialize push by channel.
 * test_3: deserialize sms by channel.
 * test_4: deserialize cc_data (fallback) for unknown channel.
 * test_5: roundtrip email with custom_fields.
 * test_6: roundtrip push with mixed custom_fields types.
 */
class SubscriptionSerializerTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(Any::class, AnySerializer)
        }
    }

    /** test_1: deserialize email by channel */
    @Test
    fun test_1_deserialize_email() {
        val obj = buildJsonObject {
            put("channel", EMAIL_CHANNEL)
            put("resource_id", 7)
            put("email", "a@b.c")
        }
        val payload = json.encodeToString(obj)

        val sub: Subscription = json.decodeFromString(payload)
        assertTrue(sub is EmailSubscription)
        val e = sub as EmailSubscription
        assertEquals(7, e.resourceId)
        assertEquals("a@b.c", e.email)
        assertEquals("email", e.channel)
    }

    /** test_2: deserialize push by channel */
    @Test
    fun test_2_deserialize_push() {
        val obj = buildJsonObject {
            put("channel", PUSH_CHANNEL)
            put("resource_id", 9)
            put("provider", "android-firebase")
            put("subscription_id", "sub-123")
        }
        val payload = json.encodeToString(obj)

        val sub: Subscription = json.decodeFromString(payload)
        assertTrue(sub is PushSubscription)
        val p = sub as PushSubscription
        assertEquals(9, p.resourceId)
        assertEquals("android-firebase", p.provider)
        assertEquals("sub-123", p.subscriptionId)
        assertEquals("push", p.channel)
    }

    /** test_3: deserialize sms by channel */
    @Test
    fun test_3_deserialize_sms() {
        val obj = buildJsonObject {
            put("channel", SMS_CHANNEL)
            put("resource_id", 11)
            put("phone", "+100200300")
        }
        val payload = json.encodeToString(obj)

        val sub: Subscription = json.decodeFromString(payload)
        assertTrue(sub is SmsSubscription)
        val s = sub as SmsSubscription
        assertEquals(11, s.resourceId)
        assertEquals("+100200300", s.phone)
        assertEquals("sms", s.channel)
    }

    /** test_4: deserialize cc_data (fallback) for unknown channel */
    @Test
    fun test_4_deserialize_cc_data_fallback() {
        val obj = buildJsonObject {
            put("channel", "telegram_bot")
            put("resource_id", 77)
            putJsonObject("cc_data") {
                put("chat_id", "999")
                put("bot", "@mybot")
            }
        }
        val payload = json.encodeToString(obj)

        val sub: Subscription = json.decodeFromString(payload)
        assertTrue(sub is CcDataSubscription)
        val c = sub as CcDataSubscription
        assertEquals(77, c.resourceId)
        assertEquals("telegram_bot", c.channel)
        assertEquals("999", c.ccData["chat_id"]?.toString()?.trim('"'))
        assertEquals("@mybot", c.ccData["bot"]?.toString()?.trim('"'))
    }

    /** test_5: roundtrip serialize/deserialize email keeps type and channel field */
    @Test
    fun test_5_roundtrip_email() {
        val original =  EmailSubscription(
            resourceId = 42,
            email = "round@trip.io",
            status = "enabled",
            priority = 3,
            customFields = mapOf<String, @Contextual Any?>(
                "tier" to "gold"
            ),
            cats = listOf("news")
        )

        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"channel\":\"email\""))

        val decoded: Subscription = json.decodeFromString(encoded)
        assertTrue(decoded is EmailSubscription)
        val e = decoded as EmailSubscription
        assertEquals(original.resourceId, e.resourceId)
        assertEquals(original.email, e.email)
        assertEquals("email", e.channel)
        assertEquals("gold", e.customFields?.get("tier"))
        assertEquals(listOf("news"), e.cats)
    }

    /** test_6: roundtrip push with mixed custom_fields types */
    @Test
    fun test_6_roundtrip_push_mixed_custom_fields() {
        val original: Subscription = PushSubscription(
            resourceId = 5,
            provider = "android-firebase",
            subscriptionId = "s-1",
            status = "ok",
            priority = 1,
            customFields = mapOf<String, @Contextual Any?>(
                "flag" to true,
                "score" to 12,
                "pi" to 3.14,
                "note" to "hello",
                "maybe" to null
            ),
            cats = listOf("a", "b")
        )

        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"channel\":\"push\""))

        val decoded: Subscription = json.decodeFromString(encoded)
        assertTrue(decoded is PushSubscription)
        val d = decoded as PushSubscription
        assertEquals(5, d.resourceId)
        assertEquals("android-firebase", d.provider)
        assertEquals("s-1", d.subscriptionId)
        assertEquals("push", d.channel)
        assertEquals(true, d.customFields?.get("flag"))
        assertEquals(12, d.customFields?.get("score"))
        assertEquals(3.14, d.customFields?.get("pi"))
        assertEquals("hello", d.customFields?.get("note"))
        assertTrue(d.customFields?.containsKey("maybe") == true)
        assertNull(d.customFields?.get("maybe"))
        assertEquals(listOf("a", "b"), d.cats)
    }
}
