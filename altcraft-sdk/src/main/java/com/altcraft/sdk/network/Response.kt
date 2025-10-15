package com.altcraft.sdk.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.MapBuilder.createEventValue
import com.altcraft.sdk.additional.PairBuilder.getRequestMessages
import com.altcraft.sdk.additional.SubFunction.isJsonString
import com.altcraft.sdk.additional.SubFunction.stringContainsHtml
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UPDATE_REQUEST
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.sdk_events.EventList.responseDataIsNull
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.interfaces.RequestData
import com.altcraft.sdk.json.Converter.json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response

/**
 * Handles parsing and processing of HTTP responses.
 *
 * Emits appropriate events based on status and error codes.
 */
internal object Response {

    /**
     * Represents the type of action to take based on the result of a network or backend response.
     *
     * Used to guide how the system should react to a given response:
     * - [SUCCESS]: The operation completed successfully.
     * - [RETRY]: A recoverable error occurred; the operation should be retried.
     * - [ERROR]: A non-recoverable error occurred; the operation failed.
     */
    enum class ResponseStatus { SUCCESS, RETRY, ERROR }

    /**
     * Parses a JSON response into an [DataClasses.Response] object.
     *
     * @param response The server response containing the status code and body data.
     * @return An `AsyncSubscribeResponse` object if parsing is successful, otherwise `null`.
     */
    private fun parseResponse(response: Response<JsonElement>): DataClasses.Response? {
        return try {
            val body = if (response.isSuccessful) response.body()?.toString()
            else response.errorBody()?.string()
            when {
                body.isNullOrEmpty() || stringContainsHtml(body)-> null
                body.isJsonString() -> json.decodeFromString<DataClasses.Response>(body)
                else -> DataClasses.Response(error = null, errorText = body, null)
            }
        } catch (e: Exception) {
            error("parseResponse", e)
            null
        }
    }

    /**
     * Extracts the status code and response data from the server response.
     *
     * @param response The server response containing the status code and body data.
     * @return Processed response data or null on failure.
     */
    private fun getResponseData(
        response: Response<JsonElement>, request: RequestData
    ): DataClasses.ResponseResult? {
        return try {
            parseResponse(response).let {
                val msg = getRequestMessages(response.code(), it, request)
                val value = createEventValue(response.code(), it, request)
                val status = responseStatus(response.code())

                DataClasses.ResponseResult(
                    status = status,
                    response = it,
                    error = it?.error,
                    httpCode = response.code(),
                    errorPair = msg.first,
                    successPair = msg.second,
                    eventValue = value
                )
            }
        } catch (e: Exception) {
            error("getResponseData", e)
            null
        }
    }

    /**
     * Determines the response status based on the given HTTP and internal error codes.
     *
     * @param code  HTTP status code (nullable).
     * @return The corresponding [ResponseStatus] indicating how the response should be handled.
     */
    private fun responseStatus(code: Int?): ResponseStatus {
        return when (code) {
            in 200..299 -> ResponseStatus.SUCCESS
            in 500..599 -> ResponseStatus.RETRY
            else -> ResponseStatus.ERROR
        }
    }

    /**
     * Analyzes the server response and generates a corresponding event based on the result.
     *
     * @param requestData The original request metadata used to determine context and build the event.
     * @param response The raw HTTP response containing the status code and response body.
     * @return A typed [DataClasses.Event]: success, retry, or error.
     */
    fun processResponse(
        requestData: RequestData,
        response: Response<JsonElement>
    ): DataClasses.Event {
        val func = "processResponse"
        return try {
            val data = getResponseData(response, requestData) ?: exception(responseDataIsNull)
            when (data.status) {
                ResponseStatus.SUCCESS -> event(func, data.successPair, data.eventValue)
                ResponseStatus.RETRY -> retry(func, data.errorPair, data.eventValue)
                ResponseStatus.ERROR -> error(func, data.errorPair, data.eventValue)
            }
        } catch (e: Exception) {
            retry("processResponse:: ${getRequestName(requestData)}", e)
        }
    }

    /**
     * Determines the function identifier pair based on the request type.
     *
     * @param requestData The request object to inspect.
     * @return A pair of strings representing the function code and short label.
     */
    fun getRequestName(requestData: RequestData): String {
        return when (requestData) {
            is DataClasses.MobileEventRequestData -> MOBILE_EVENT_REQUEST
            is DataClasses.UnSuspendRequestData -> UNSUSPEND_REQUEST
            is DataClasses.SubscribeRequestData -> SUBSCRIBE_REQUEST
            is DataClasses.StatusRequestData -> STATUS_REQUEST
            is DataClasses.UpdateRequestData -> UPDATE_REQUEST
            else -> PUSH_EVENT_REQUEST
        }
    }
}