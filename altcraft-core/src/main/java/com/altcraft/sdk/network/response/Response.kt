package com.altcraft.sdk.network.response

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.util.log.Log
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.json.Converter
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.util.MapBuilder.createEventValue
import com.altcraft.sdk.util.SubFunction.isJsonString
import kotlinx.serialization.json.JsonElement
import retrofit2.Response

/**
 * Handles parsing and processing of HTTP responses.
 *
 * Emits SDK events based on HTTP status and returns the parsed response body.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Response {

    /**
     * Represents the action to take based on the HTTP response status.
     */
    internal enum class ResponseStatus { SUCCESS, RETRY, ERROR }

    /**
     * Combines a generated [com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event] with an optional typed response body.
     */
    data class ApiResponse<T : ResponseModel.ResponseData>(
        val httpCode: Int? = null,
        val response: T? = null,
        val event: SDKEventModel.Event,
    )

    /**
     * Parses an HTTP response body into a typed [ResponseModel.ResponseData].
     *
     * @param response Raw HTTP response.
     * @param request Original request containing the response serializer.
     * @return Parsed response data, or `null` if the body is empty, non-JSON, or parsing fails.
     */
    private fun <T : ResponseModel.ResponseData> parseResponse(
        response: Response<JsonElement>,
        request: RequestData<T>
    ): T? {
        return try {
            val body = if (response.isSuccessful) {
                response.body()?.toString()
            } else {
                response.errorBody()?.string()
            }

            when {
                body.isNullOrEmpty() -> null
                body.isJsonString() -> decode(
                    body, request
                )

                else -> null
            }
        } catch (e: Exception) {
            Log.error("parseResponse", e)
            null
        }
    }

    /**
     * Decodes a JSON string into a typed response model.
     *
     * @param body Raw JSON response body.
     * @param request Request containing the target response serializer.
     * @return Parsed response object.
     */
    private fun <T : ResponseModel.ResponseData> decode(
        body: String,
        request: RequestData<T>
    ): T {
        return Converter.json.decodeFromString(
            request.responseSerializer, body
        )
    }

    /**
     * Extracts response status and parsed body from the HTTP response.
     *
     * @param response Raw HTTP response.
     * @param request Original request data.
     * @return Processed response result, or `null` if processing fails.
     */
    internal fun <T : ResponseModel.ResponseData> getResponseData(
        response: Response<JsonElement>,
        request: RequestData<T>
    ): ResponseModel.ResponseResult<T>? {
        return try {
            val parsedBody = parseResponse(response, request)
            val status = responseStatus(response.code(), parsedBody)
            val successEvent = request.successEvent
            val errorEvent = request.failedEvent
            val value = createEventValue(
                response.code(),
                parsedBody,
                request
            )

            ResponseModel.ResponseResult(
                status = status,
                responseData = parsedBody,
                httpCode = response.code(),
                value = value,
                moduleLogPrefix = request
                    .moduleLogPrefix,
                errorEvent = errorEvent,
                successEvent = successEvent,
            )
        } catch (e: Exception) {
            Log.error("getResponseData", e)
            null
        }
    }

    /**
     * Determines response status based on HTTP and API error codes.
     *
     * @param httpCode HTTP response status code.
     * @param parsedBody Parsed API response body.
     * @return SUCCESS, RETRY, or ERROR.
     */
    private fun responseStatus(
        httpCode: Int?, parsedBody: ResponseModel.ResponseData?
    ): ResponseStatus {
        val apiError = (
                parsedBody as? ResponseModel.BaseResponseData
                )?.error

        return when {
            httpCode in 500..599 -> ResponseStatus.RETRY

            (httpCode !in 200..299 && httpCode != 304) ||
                    (apiError != null && apiError != 0) ->
                ResponseStatus.ERROR

            else -> ResponseStatus.SUCCESS
        }
    }

    /**
     * Creates an SDK [SDKEventModel.Event] from the processed response result.
     *
     * @param result Processed response result.
     * @return Success, retry, or error event depending on the response status.
     */
    internal fun <T : ResponseModel.ResponseData> eventGeneration(
        result: ResponseModel.ResponseResult<T>
    ): SDKEventModel.Event {
        val function = "${result.moduleLogPrefix}processResponse"

        return when (result.status) {
            ResponseStatus.SUCCESS -> Events.event(
                function,
                result.successEvent,
                result.value.eventValue,
                result.value.loggerValue
            )

            ResponseStatus.RETRY -> Events.retry(
                function,
                result.errorEvent,
                result.value.eventValue,
                result.value.loggerValue
            )

            ResponseStatus.ERROR -> Events.error(
                function,
                result.errorEvent,
                result.value.eventValue,
                result.value.loggerValue
            )
        }
    }

    /**
     * Processes an HTTP response and returns both the generated SDK event
     * and the parsed typed response body.
     *
     * @param requestData Request data containing response type information.
     * @param response Raw HTTP response.
     * @return [ApiResponse] containing the SDK event and parsed response body.
     */
    internal fun <T : ResponseModel.ResponseData> processResponse(
        requestData: RequestData<T>,
        response: Response<JsonElement>
    ): ApiResponse<T> {
        return try {
            val data = getResponseData(response, requestData) ?: exception(
                CoreEvents.responseDataIsNull
            )

            ApiResponse(data.httpCode, data.responseData, eventGeneration(data))
        } catch (e: Exception) {
            ApiResponse(httpCode = null, response = null, Events.retry("processResponse", e))
        }
    }
}