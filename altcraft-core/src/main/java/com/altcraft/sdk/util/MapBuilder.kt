package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.Constants.HTTP_CODE_KEY
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseEventValue
import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log.error
import kotlin.coroutines.cancellation.CancellationException

/**
 * Utility for constructing structured maps used in push analytics and subscription logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object MapBuilder {

    /**
     * Merges response/request event and log data with HTTP status code.
     *
     * @param code HTTP status code.
     * @param response Response data, or `null` if unavailable.
     * @param request Request data from an SDK module.
     * @return Combined response event value.
     */
    internal fun <R> createEventValue(
        code: Int,
        response: ResponseData?,
        request: com.altcraft.sdk.network.request.RequestData<R>,
    ): ResponseEventValue =
        createEventValueGeneric(
            code,
            response,
            request.eventData,
            request.loggerData
        )

    /**
     * Creates a response event value by merging response and request data.
     *
     * HTTP status code is added to both event and logger data maps.
     *
     * @param code HTTP status code.
     * @param response Response data, or `null` if unavailable.
     * @param eventData Request event data, or `null` if unavailable.
     * @param loggerData Request logger data, or `null` if unavailable.
     * @return Combined response event value.
     */
    private fun createEventValueGeneric(
        code: Int,
        response: ResponseData?,
        eventData: Map<String, Any?>?,
        loggerData: Map<String, Any?>?
    ): ResponseEventValue {
        /** Merges response and request maps; response values take precedence. */
        fun merge(
            responseValue: Map<String, Any?>?,
            requestValue: Map<String, Any?>?
        ): Map<String, Any?> {
            return requestValue.orEmpty() +
                    responseValue.orEmpty() +
                    mapOf(HTTP_CODE_KEY to code)
        }

        return ResponseEventValue(
            eventValue = merge(response?.eventData, eventData),
            loggerValue = merge(response?.loggerData, loggerData)
        )
    }

    /**
     * Merges device, app, and custom fields into a single map.
     *
     * The application metadata is resolved from the current SDK configuration.
     *
     * @param context Used to fetch device and app fields.
     * @param customFields Optional user-defined fields.
     * @return Combined subscription data map, or empty if an error occurs.
     */
    suspend fun unionMaps(
        context: Context,
        customFields: Map<String, Any?>?
    ) = try {
        Environment.create(context).deviceFields +
                (customFields ?: emptyMap()) +
                (ConfigManager.getConfig(context)?.appInfo?.asMap() ?: emptyMap())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        error("unionMaps", e)
        emptyMap()
    }
}