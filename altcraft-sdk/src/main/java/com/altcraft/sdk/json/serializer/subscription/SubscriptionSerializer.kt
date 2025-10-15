package com.altcraft.sdk.json.serializer.subscription

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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Content-based polymorphic serializer for Subscription using the channel field. */
object SubscriptionSerializer :
    JsonContentPolymorphicSerializer<Subscription>(Subscription::class) {

    /**
     * Selects the concrete `Subscription` deserializer based on the `channel` field.
     *
     * Mapping:
     * - `EMAIL_CHANNEL`   → `EmailSubscription`
     * - `PUSH_CHANNEL`    → `PushSubscription`
     * - `SMS_CHANNEL`     → `SmsSubscription`
     * - else              → `CcDataSubscription`
     */
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<Subscription> {
        val obj = element as? JsonObject
        val channel = obj?.get("channel")?.let { it as? JsonPrimitive }?.contentOrNull

        return when (channel) {
            EMAIL_CHANNEL -> EmailSubscription.serializer()
            PUSH_CHANNEL -> PushSubscription.serializer()
            SMS_CHANNEL -> SmsSubscription.serializer()
            else -> CcDataSubscription.serializer()
        }
    }
}