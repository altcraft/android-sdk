package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.data.Constants.RESPONSE_WITH_HTTP_CODE
import com.altcraft.sdk.data.Constants.TYPE
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.device.DeviceInfo.getDeviceFields
import com.altcraft.sdk.interfaces.RequestData

/**
 * Utility for constructing structured maps used in push analytics and subscription logic.
 *
 */
internal object MapBuilder {

    /**
     * Builds a map with push event details for logging or analytics.
     *
     * Includes:
     * - "code": HTTP status code
     * - "data": Full response object
     * - "uid": Cleaned UID without "delivery"/"open"
     * - "type": Event type extracted from UID
     *
     * @param code HTTP response code
     * @param response Response data
     * @param requestData Original request (should be PushEventRequestData)
     * @return Map with non-null fields: "uid", "type", "code", "data"
     */
    fun createEventValue(
        code: Int,
        response: DataClasses.Response?,
        requestData: RequestData,
    ): Map<String, Any?> {
        val (uid, type) = (requestData as? DataClasses.PushEventRequestData)?.let {
            it.uid to it.type
        } ?: (null to null)

        val res = DataClasses.ResponseWithHttpCode(httpCode = code, response = response)

        return mapOf(RESPONSE_WITH_HTTP_CODE to res, UID to uid, TYPE to type).filterValues {
            it != null
        }
    }

    /**
     * Merges device, app, and custom fields into a single map.
     *
     * Later values override earlier ones by key.
     *
     * @param context Used to fetch device-specific fields.
     * @param config Provides app-related metadata.
     * @param customFields Optional user-defined fields.
     * @return Combined subscription data map, or empty if error occurs.
     */
    fun unionMaps(
        context: Context,
        config: ConfigurationEntity,
        customFields: Map<String, Any?>?
    ): Map<String, Any?> {
        return try {
            listOfNotNull(getDeviceFields(context), customFields, config.appInfo?.asMap())
                .fold(emptyMap()) { acc, map ->
                    acc + map
                }
        } catch (e: Exception) {
            error("unionMaps", e)
            emptyMap()
        }
    }
}