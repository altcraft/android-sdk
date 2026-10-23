package com.altcraft.sdk.target.data.serializer

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.data.Constants.EMAIL_CHANNEL
import com.altcraft.sdk.target.data.Constants.PUSH_CHANNEL
import com.altcraft.sdk.target.data.Constants.SMS_CHANNEL
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.CcDataSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.EmailSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.PushSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.SmsSubscription
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.Subscription
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Selects a subscription serializer based on the channel field. */
internal object SubscriptionSerializer :
    JsonContentPolymorphicSerializer<Subscription>(Subscription::class) {

    /**
     * Returns the serializer matching the subscription channel.
     *
     * @param element subscription JSON element.
     * @return matching subscription serializer.
     */
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<Subscription> {
        val obj = element as? JsonObject
        val channel = (obj?.get("channel") as? JsonPrimitive)?.contentOrNull

        return when (channel) {
            EMAIL_CHANNEL -> EmailSubscription.serializer()
            PUSH_CHANNEL -> PushSubscription.serializer()
            SMS_CHANNEL -> SmsSubscription.serializer()
            else -> CcDataSubscription.serializer()
        }
    }
}