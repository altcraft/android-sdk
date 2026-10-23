package com.altcraft.sdk.push.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.data.Constants.SMID
import com.altcraft.sdk.push.data.Constants.TIME
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for push event requests.
 */
internal object EventJson {

    /**
     * Builds a JSON payload for a push event request.
     *
     * @param data Push event request data.
     * @return JSON payload for the request.
     */
    internal fun createPushEventJson(
        data: EventRequestModel
    ): JsonObject = buildJsonObject {
        put(TIME, data.time / 1000)
        put(SMID, data.uid)
    }
}
