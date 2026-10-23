@file:Suppress("SpellCheckingInspection")

package test.target.serializer

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.CcDataSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.EmailSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.PushSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.SmsSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.Subscription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SubscriptionSerializerTest
 *
 * Verifies polymorphic subscription deserialization selected by the `channel` field.
 *
 * Positive scenarios:
 *  - `email` resolves to [EmailSubscription].
 *  - `push` resolves to [PushSubscription].
 *  - `sms` resolves to [SmsSubscription].
 *  - a `cc_data` channel resolves to [CcDataSubscription].
 *  - an unknown channel falls back to [CcDataSubscription].
 */
class SubscriptionSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun channelEmail_resolvesToEmailSubscription() {
        val s: Subscription = json.decodeFromString(
            """{"resource_id":1,"email":"a@b.co","channel":"email","status":"active"}"""
        )

        assertTrue(s is EmailSubscription)
        assertEquals("email", s.channel)
        assertEquals(1, s.resourceId)
        assertEquals("a@b.co", (s as EmailSubscription).email)
    }

    @Test
    fun channelPush_resolvesToPushSubscription() {
        val s: Subscription = json.decodeFromString(
            """{"resource_id":2,"provider":"android-firebase","subscription_id":"tok","channel":"push","priority":5}"""
        )

        assertTrue(s is PushSubscription)
        assertEquals("push", s.channel)
        assertEquals(2, s.resourceId)
        assertEquals("tok", (s as PushSubscription).subscriptionId)
        assertEquals(5, s.priority)
    }

    @Test
    fun channelSms_resolvesToSmsSubscription() {
        val s: Subscription = json.decodeFromString(
            """{"resource_id":3,"phone":"+79160000000","channel":"sms"}"""
        )

        assertTrue(s is SmsSubscription)
        assertEquals("sms", s.channel)
        assertEquals("+79160000000", (s as SmsSubscription).phone)
    }

    @Test
    fun ccDataChannel_resolvesToCcDataSubscription() {
        val s: Subscription = json.decodeFromString(
            """{"resource_id":4,"channel":"telegram_bot","cc_data":{"chat_id":"123"}}"""
        )

        assertTrue(s is CcDataSubscription)
        assertEquals("telegram_bot", s.channel)
        assertEquals("123", (s as CcDataSubscription).ccData["chat_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun unknownChannel_fallsBackToCcDataSubscription() {
        val s: Subscription = json.decodeFromString(
            """{"resource_id":5,"channel":"custom","cc_data":{"key":"value"},"cats":["a","b"]}"""
        )

        assertTrue(s is CcDataSubscription)
        assertEquals("custom", s.channel)
        assertEquals(listOf("a", "b"), (s as CcDataSubscription).cats)
    }
}
