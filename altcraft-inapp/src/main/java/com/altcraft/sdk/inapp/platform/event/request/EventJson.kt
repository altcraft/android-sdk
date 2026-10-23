package com.altcraft.sdk.inapp.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.Constants.ACID
import com.altcraft.sdk.inapp.data.Constants.CODE
import com.altcraft.sdk.inapp.data.Constants.DATA
import com.altcraft.sdk.inapp.data.Constants.LINK
import com.altcraft.sdk.inapp.data.Constants.MESSAGE
import com.altcraft.sdk.inapp.data.Constants.NAME
import com.altcraft.sdk.inapp.data.Constants.SMID
import com.altcraft.sdk.inapp.data.Constants.TIME
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Generates JSON payloads for in-app event requests.
 */
internal object EventJson {

    /**
     * Creates a JSON payload for an in-app event request.
     *
     * Event mapping:
     * - delivery — no extra data
     * - show — no extra data
     * - click — { "name": "...", "link": "..." }
     * - error — { "code": ..., "message": "..." }
     * - close — no extra data
     *
     * @param data In-app event request data.
     * @return A [JsonObject] for the request.
     */
    fun createEventJson(
        data: EventRequestModel
    ) = buildJsonObject {
        put(TIME, data.time)
        put(SMID, data.smid)
        put(ACID, data.acid)

        data.data?.let { payload ->
            put(DATA, buildJsonObject {
                payload.name?.let {
                    put(NAME, it)
                }
                payload.href?.let {
                    put(LINK, it)
                }
                payload.code?.let {
                    put(CODE, it)
                }
                payload.message?.let {
                    put(MESSAGE, it)
                }
            })
        }
    }
}
