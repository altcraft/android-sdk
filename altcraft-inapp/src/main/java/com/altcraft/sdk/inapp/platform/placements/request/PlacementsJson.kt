package com.altcraft.sdk.inapp.platform.placements.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.Constants.ACID
import com.altcraft.sdk.inapp.data.Constants.DEVICE_ID
import com.altcraft.sdk.inapp.data.Constants.LAST_PUBLISH
import com.altcraft.sdk.inapp.data.Constants.PLACEMENT_RULES_CHECKSUM
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for in-app placements requests.
 */
internal object PlacementsJson {

    /**
     * Creates a JSON payload for an in-app placements request.
     *
     * @param data In-app placements request data.
     * @return A [JsonObject] for the request.
     */
    fun createPlacementsJson(
        data: PlacementsRequestModel
    ) = buildJsonObject {
        put(ACID, data.acid)
        put(DEVICE_ID, data.deviceId)
        put(LAST_PUBLISH, data.lastPublish)
        put(
            PLACEMENT_RULES_CHECKSUM,
            data.placementRulesChecksum
        )
    }
}
