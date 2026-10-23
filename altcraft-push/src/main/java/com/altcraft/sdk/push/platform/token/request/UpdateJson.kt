package com.altcraft.sdk.push.platform.token.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.data.Constants.NEW_PROVIDER
import com.altcraft.sdk.push.data.Constants.NEW_TOKEN
import com.altcraft.sdk.push.data.Constants.OLD_PROVIDER
import com.altcraft.sdk.push.data.Constants.OLD_TOKEN
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for push token update requests.
 */
internal object UpdateJson {

    /**
     * Builds a JSON payload for a push token update request.
     *
     * @param data Token update request data.
     * @return JSON payload for the request.
     */
    internal fun createTokenUpdateJson(
        data: UpdateRequestModel
    ): JsonObject = buildJsonObject {
        put(OLD_TOKEN, data.oldToken)
        put(OLD_PROVIDER, data.oldProvider)
        put(NEW_TOKEN, data.newToken)
        put(NEW_PROVIDER, data.newProvider)
    }
}
