package com.altcraft.sdk.profile.platform.updateFields.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.profile.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.profile.data.Constants.SKIP_TRIGGERS
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for profile update requests.
 */
internal object UpdateFieldsJson {

    /**
     * Builds a JSON payload for a profile update request.
     *
     * @param data Profile update request data.
     * @return JSON payload for the request.
     */
    internal fun createRequestJson(
        data: UpdateFieldsRequestModel
    ): JsonObject = buildJsonObject {
        put(PROFILE_FIELDS, data.profileFields ?: JsonNull)
        put(SKIP_TRIGGERS, data.skipTriggers)
    }
}
