package com.altcraft.sdk.inapp.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.Constants.ACID
import com.altcraft.sdk.inapp.data.Constants.CATS
import com.altcraft.sdk.inapp.data.Constants.CATS_ACTIVE
import com.altcraft.sdk.inapp.data.Constants.CATS_NAME
import com.altcraft.sdk.inapp.data.Constants.CATS_STEADY
import com.altcraft.sdk.inapp.data.Constants.CATS_TITLE
import com.altcraft.sdk.inapp.data.Constants.FIELDS
import com.altcraft.sdk.inapp.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.inapp.data.Constants.REPLACE
import com.altcraft.sdk.inapp.data.Constants.SKIP_TRIGGERS
import com.altcraft.sdk.inapp.data.Constants.STATUS
import com.altcraft.sdk.inapp.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.inapp.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.inapp.data.Constants.SYNC
import com.altcraft.sdk.inapp.data.Constants.TIME
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for in-app subscription requests.
 */
internal object SubscriptionJson {

    /**
     * Builds a JSON object for an in-app subscription request.
     *
     * @param data In-app subscription request data.
     * @return A [JsonObject] for the request.
     */
    fun createSubscriptionJson(
        data: SubscriptionRequestModel
    ) = buildJsonObject {
        val cats = buildJsonArray {
            data.cats?.forEach { category ->
                add(buildJsonObject {
                    put(CATS_NAME, category.name)
                    put(CATS_TITLE, category.title)
                    put(CATS_STEADY, category.steady)
                    put(CATS_ACTIVE, category.active)
                })
            }
        }

        val subscription = buildJsonObject {
            put(SUBSCRIPTION_ID, data.deviceId)
            put(STATUS, data.status)
            put(FIELDS, data.fields ?: JsonNull)
            put(CATS, cats)
        }

        put(SYNC, data.sync)
        put(ACID, data.acid)
        put(PROFILE_FIELDS, data.profileFields ?: JsonNull)
        put(TIME, data.time)
        put(SUBSCRIPTION, subscription)
        put(REPLACE, data.replace == true)
        put(SKIP_TRIGGERS, data.skipTriggers == true)
    }
}
