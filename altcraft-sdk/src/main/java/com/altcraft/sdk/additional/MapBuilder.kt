package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.sdk_events.Events.error
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
     * Builds a map containing API request data added to SDK events.
     *
     * The map may include:
     * - "code": HTTP response code
     * - "data": Full response object
     *
     * If the request represents a push event, the map additionally includes:
     * - "uid": Unique notification identifier
     * - "type": Event type ("delivery" or "open")
     *
     * If the request represents a mobile event, the map additionally includes:
     * - "name": Name of the mobile event
     *
     * @param code HTTP response code
     * @param response Response data
     * @param requestData request object
     * @return A map containing non-null fields relevant to the event type
     */
    fun createEventValue(
        code: Int,
        response: DataClasses.Response?,
        requestData: RequestData,
    ): Map<String, Any?> {
        val (uid, type) = (requestData as? DataClasses.PushEventRequestData)?.let {
            it.uid to it.type
        } ?: (null to null)

        val name = (requestData as? DataClasses.MobileEventRequestData)?.name

        val res = DataClasses.ResponseWithHttpCode(httpCode = code, response = response)

        return mapOf(RESPONSE_WITH_HTTP_CODE to res, UID to uid, TYPE to type, NAME to name)
            .filterValues { it != null }
    }

    /**
     * Merges device, app, and custom fields into a single map.
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