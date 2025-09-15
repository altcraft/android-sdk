package com.altcraft.sdk.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.CATS
import com.altcraft.sdk.data.Constants.CATS_ACTIVE
import com.altcraft.sdk.data.Constants.CATS_NAME
import com.altcraft.sdk.data.Constants.CATS_STEADY
import com.altcraft.sdk.data.Constants.CATS_TITLE
import com.altcraft.sdk.data.Constants.FIELDS
import com.altcraft.sdk.data.Constants.NEW_PROVIDER
import com.altcraft.sdk.data.Constants.NEW_TOKEN
import com.altcraft.sdk.data.Constants.OLD_PROVIDER
import com.altcraft.sdk.data.Constants.OLD_TOKEN
import com.altcraft.sdk.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.data.Constants.PROVIDER
import com.altcraft.sdk.data.Constants.REPLACE
import com.altcraft.sdk.data.Constants.SKIP_TRIGGERS
import com.altcraft.sdk.data.Constants.SMID
import com.altcraft.sdk.data.Constants.STATUS
import com.altcraft.sdk.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.data.Constants.TIME
import com.altcraft.sdk.data.DataClasses
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonObject

/**
 * `JsonFactory` generates JSON payloads for various SDK requests like
 * subscription, token update, push events, and unSuspend operations.
 */
internal object JsonFactory {

    /**
     * Builds a JSON object for a push subscription request.
     *
     * @param data Subscription details.
     * @return JSON object ready to send, or empty if error occurs.
     */
    internal fun createSubscribeJson(
        data: DataClasses.SubscribeRequestData
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
            put(SUBSCRIPTION_ID, data.deviceToken)
            put(PROVIDER, data.provider)
            put(STATUS, data.status)
            put(FIELDS, data.fields ?: JsonNull)
            put(CATS, cats)
        }

        put(TIME, data.time)
        put(SUBSCRIPTION_ID, data.deviceToken)
        put(SUBSCRIPTION, subscription)
        put(PROFILE_FIELDS, data.profileFields ?: JsonNull)
        put(REPLACE, data.replace == true)
        put(SKIP_TRIGGERS, data.skipTriggers == true)
    }

    /**
     * Creates a JSON payload for a push event request.
     *
     * @param data Event data.
     * @return A [JsonObject] for the request.
     */
    fun createPushEventJson(
        data: DataClasses.PushEventRequestData
    ) = buildJsonObject {
        put(TIME, data.time)
        put(SMID, data.uid)
    }

    /**
     * Creates a JSON payload for token update.
     *
     * @param data Token update data.
     * @return A [JsonObject] for the request.
     */
    fun createUpdateJson(
        data: DataClasses.UpdateRequestData
    ) = buildJsonObject {
        put(OLD_TOKEN, data.oldToken)
        put(OLD_PROVIDER, data.oldProvider)
        put(NEW_TOKEN, data.newToken)
        put(NEW_PROVIDER, data.newProvider)
    }

    /**
     * Creates a JSON payload for unSuspend.
     *
     * @param data UnSuspend request data.
     * @return A [JsonObject] for the request.
     */
    fun createUnSuspendJson(
        data: DataClasses.UnSuspendRequestData
    ) = buildJsonObject {
        val subscription = buildJsonObject {
            put(SUBSCRIPTION_ID, data.token)
            put(PROVIDER, data.provider)
        }
        put(SUBSCRIPTION, subscription)
        put(REPLACE, true)
    }
}