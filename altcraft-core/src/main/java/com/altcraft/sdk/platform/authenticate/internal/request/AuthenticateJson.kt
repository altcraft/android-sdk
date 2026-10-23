package com.altcraft.sdk.platform.authenticate.internal.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.ACID
import com.altcraft.sdk.data.Constants.START_TIME
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for SDK requests such as authorize operations.
 */
internal object AuthenticateJson {

    /**
     * Creates a JSON payload for authorize request.
     *
     * @param data Authorize request data.
     * @return A [JsonObject] for the request.
     */
    fun createAuthenticateJson(
        data: AuthenticateRequestModel
    ): JsonObject = buildJsonObject {
        put(ACID, data.currentAcid)
        put(START_TIME, data.startAnonSession)
    }
}
