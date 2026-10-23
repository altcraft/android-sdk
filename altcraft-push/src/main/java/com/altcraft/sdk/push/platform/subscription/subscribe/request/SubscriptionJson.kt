package com.altcraft.sdk.push.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.data.Constants.CATS
import com.altcraft.sdk.push.data.Constants.CATS_ACTIVE
import com.altcraft.sdk.push.data.Constants.CATS_NAME
import com.altcraft.sdk.push.data.Constants.CATS_STEADY
import com.altcraft.sdk.push.data.Constants.CATS_TITLE
import com.altcraft.sdk.push.data.Constants.FIELDS
import com.altcraft.sdk.push.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.push.data.Constants.PROVIDER
import com.altcraft.sdk.push.data.Constants.REPLACE
import com.altcraft.sdk.push.data.Constants.SKIP_TRIGGERS
import com.altcraft.sdk.push.data.Constants.STATUS
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.push.data.Constants.TIME
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for push subscription requests.
 */
internal object SubscriptionJson {

    /**
     * Builds a JSON payload for a push subscription request.
     *
     * @param data Push subscription request data.
     * @return JSON payload for the request.
     */
    internal fun createPushSubscribeJson(
        data: SubscribeRequestModel
    ): JsonObject = buildJsonObject {
        val cats = buildJsonArray {
            data.cats?.forEach { category ->
                add(
                    buildJsonObject {
                        put(CATS_NAME, category.name)
                        put(CATS_TITLE, category.title)
                        put(CATS_STEADY, category.steady)
                        put(CATS_ACTIVE, category.active)
                    }
                )
            }
        }

        val subscription = buildJsonObject {
            put(SUBSCRIPTION_ID, data.deviceToken)
            put(PROVIDER, data.provider)
            put(STATUS, data.status)
            put(FIELDS, data.fields ?: JsonNull)
            put(CATS, cats)
        }

        put(TIME, data.time / 1000)
        put(SUBSCRIPTION_ID, data.deviceToken)
        put(SUBSCRIPTION, subscription)
        put(PROFILE_FIELDS, data.profileFields ?: JsonNull)
        put(REPLACE, data.replace == true)
        put(SKIP_TRIGGERS, data.skipTriggers == true)
    }
}
