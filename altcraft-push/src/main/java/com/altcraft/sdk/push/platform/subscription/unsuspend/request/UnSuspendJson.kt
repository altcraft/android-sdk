package com.altcraft.sdk.push.platform.subscription.unsuspend.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.data.Constants.PROVIDER
import com.altcraft.sdk.push.data.Constants.REPLACE
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION_ID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for push unSuspend requests.
 */
internal object UnSuspendJson {

    /**
     * Builds a JSON payload for restoring a suspended subscription.
     *
     * @param data Subscription restoration request data.
     * @return JSON payload for the request.
     */
    internal fun createUnSuspendJson(
        data: UnSuspendRequestModel
    ): JsonObject = buildJsonObject {
        val subscription = buildJsonObject {
            put(SUBSCRIPTION_ID, data.token)
            put(PROVIDER, data.provider)
        }

        put(SUBSCRIPTION, subscription)
        put(REPLACE, true)
    }
}
