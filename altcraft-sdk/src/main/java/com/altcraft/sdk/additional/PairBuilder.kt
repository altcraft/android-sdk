package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.network.Response
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.SUCCESS_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UPDATE_REQUEST
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.events.EventList.pushProviderSet
import com.altcraft.sdk.interfaces.RequestData

/**
 * Internal utility for generating structured response pairs used in SDK logging and error handling.
 */
internal object PairBuilder {

    /**
     * Returns both the error and success messages for a given response and request context.
     *
     * This helper is useful when both message types are needed for event logging or processing.
     *
     * @param code The HTTP status code.
     * @param response The API response object containing error details.
     * @param request The original request metadata.
     * @return A pair containing (errorMessage, successMessage).
     */
    fun getRequestMessages(
        code: Int,
        response: DataClasses.Response?,
        request: RequestData
    ) = createErrorRequestPair(code, response, request) to createSuccessRequestPair(request)

    /**
     * Generates a success code and message after a successful server request.
     *
     * @param request The request that was successfully processed.
     * @return A Pair where:
     *   - first: a success code for request
     *   - second: a success message string.
     */
    private fun createSuccessRequestPair(
        request: RequestData
    ): Pair<Int, String> {
        return when (Response.getRequestName(request)) {
            SUBSCRIBE_REQUEST ->
                230 to "$SUCCESS_REQUEST $SUBSCRIBE_REQUEST"

            UPDATE_REQUEST ->
                231 to "$SUCCESS_REQUEST $UPDATE_REQUEST"

            UNSUSPEND_REQUEST ->
                233 to "$SUCCESS_REQUEST $UNSUSPEND_REQUEST"

            STATUS_REQUEST ->
                234 to "$SUCCESS_REQUEST $STATUS_REQUEST"

            else -> {
                val type = (request as? DataClasses.PushEventRequestData)?.type
                232 to "$SUCCESS_REQUEST $PUSH_EVENT_REQUEST. Event type: $type"
            }
        }
    }

    /**
     * Maps SDK requests to error codes:
     * - First: 5xx — server error, SDK will retry
     * - Second: 4xx — client error, no retry
     */
    private val retryableRequestErrorCodeMap = mapOf(
        SUBSCRIBE_REQUEST to (530 to 430),
        UPDATE_REQUEST to (531 to 431),
        PUSH_EVENT_REQUEST to (532 to 432),
    )

    /**
     * Generates a detailed error code and message from the server response.
     *
     * @param code The HTTP status code from the server.
     * @param response The parsed API response containing error details.
     * @param requestData The original request metadata used to enrich the message.
     * @return A Pair where:
     *   - first: a custom error code.
     *   - second: a formatted error message string.
     */
    private fun createErrorRequestPair(
        code: Int,
        response: DataClasses.Response?,
        requestData: RequestData
    ): Pair<Int, String> {
        val error = response?.error
        val text = response?.errorText
        val requestName = Response.getRequestName(requestData)
        val baseMsg = "request: $requestName, http code: $code, error: $error, errorText: $text"

        val errorMsg = if (requestData is DataClasses.PushEventRequestData) {
            "$baseMsg, type: ${requestData.type}"
        } else baseMsg

        return when (requestName) {
            in retryableRequestErrorCodeMap -> {
                val (code5xx, code4xx) = retryableRequestErrorCodeMap.getValue(requestName)
                if (code in 500..599) code5xx to errorMsg else code4xx to errorMsg
            }

            UNSUSPEND_REQUEST -> 433 to errorMsg
            STATUS_REQUEST -> 434 to errorMsg
            else -> if (code in 500..599) 539 to "unknown request: $errorMsg" else
                439 to "unknown request: $errorMsg"
        }
    }

    /**
     * Creates a pair representing the push provider token set event.
     *
     * @param data The token data containing the push provider and token string.
     * @return A pair consisting of the event code and a human-readable message.
     */
    fun createSetTokenEventPair(data: DataClasses.TokenData): Pair<Int, String> {
        val msg = "${pushProviderSet.second}${data.provider} " + "token: ${data.token}"
        return pushProviderSet.first to msg
    }
}