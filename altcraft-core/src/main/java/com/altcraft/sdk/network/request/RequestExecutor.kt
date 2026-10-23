package com.altcraft.sdk.network.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseData
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.network.NetworkClient
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.network.response.Response.processResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes network requests supplied by SDK feature modules.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object RequestExecutor {

    /**
     * Executes and processes the provided SDK request.
     *
     * @param context application context.
     * @param requestData request and response processing data.
     * @return processed API response.
     */
    suspend fun <T : ResponseData> execute(
        context: Context,
        requestData: RequestData<T>
    ): ApiResponse<T> {
        val response = executeRequest(
            context = context,
            request = requestData.requestModel
        )

        return processResponse(
            requestData = requestData,
            response = response
        )
    }

    /**
     * Executes a raw GET request without Authorization, Request-ID,
     * matching mode, or query parameters.
     *
     * Used for loading public content (for example in-app campaign content).
     *
     * @param url Content URL (absolute).
     * @return Raw JSON body, or `null` on failure.
     */
    suspend fun getRaw(url: String): JsonElement? {
        return try {
            NetworkClient.api.get(
                url = url,
                requestId = null,
                authHeader = null,
                matchingMode = null,
                query = emptyMap()
            ).body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getRaw", e)
            null
        }
    }

    /**
     * Loads a raw text resource relative to the resolution of the current API URL.
     *
     * Used by the in-app module to load static JS/CSS bundles.
     * The blocking read runs on [Dispatchers.IO], so the function is safe
     * to invoke from any dispatcher.
     *
     * @param context Application context.
     * @param path Resource path relative to the API URL.
     * @return Raw text body, or `null` on failure.
     */
    suspend fun getRawText(context: Context, path: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                URL(
                    resolveApiUrl(context).fullUrl(path)
                ).readText()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getRawText", e)
            null
        }
    }

    /**
     * Executes the provided network request.
     *
     * The API URL is resolved from the current SDK configuration and prepended
     * to the request path, keeping [GenericRequest.url] relative.
     *
     * @param context application context.
     * @param request network request data.
     * @return raw Retrofit response.
     */
    private suspend fun executeRequest(
        context: Context,
        request: GenericRequest
    ): Response<JsonElement> {
        val apiUrl = resolveApiUrl(
            context
        )

        return when (request) {

            is GenericRequest.Get ->
                NetworkClient.api.get(
                    url = apiUrl.fullUrl(
                        request.url
                    ),
                    authHeader = null,
                    requestId = null,
                    matchingMode = null,
                    query = emptyMap()
                )

            is GenericRequest.GetWithParams -> {
                val auth = Environment.create(
                    context
                ).auth.external
                NetworkClient.api.get(
                    url = apiUrl.fullUrl(
                        request.url
                    ),
                    authHeader = auth.header(),
                    requestId = request.requestId,
                    matchingMode = auth.matching(),
                    query = request.query.filterQuery()
                )
            }

            is GenericRequest.JsonPost -> {
                val auth = Environment.create(
                    context
                ).auth.external
                NetworkClient.api.postJson(
                    url = apiUrl.fullUrl(
                        request.url
                    ),
                    authHeader = auth.header(),
                    requestId = request.requestId,
                    matchingMode = auth.matching(),
                    query = request.query.filterQuery(),
                    body = request.body
                )
            }

            is GenericRequest.MultipartPost -> {
                val auth = Environment.create(
                    context
                ).auth.external
                NetworkClient.api.postMultipart(
                    url = apiUrl.fullUrl(
                        request.url
                    ),
                    authHeader = auth.header(),
                    requestId = request.requestId,
                    matchingMode = auth.matching(),
                    query = request.query.filterQuery(),
                    parts = request.parts
                )
            }
        }
    }

    /** Keeps only non-null query values. */
    private fun Map<String, String?>.filterQuery(): Map<String, String> =
        mapNotNull { (key, value) ->
            value?.let { key to it }
        }.toMap()

    /**
     * Resolves the current API URL from the SDK configuration.
     *
     * @param context Application context used to access the SDK environment.
     * @return The configured API URL.
     */
    private suspend fun resolveApiUrl(context: Context): String = Environment
        .create(context)
        .config()
        .apiUrl

    /** Combines the resolved API URL with a request path. */
    private fun String.fullUrl(path: String) = trimEnd('/') + "/" + path.trimStart(
        '/'
    )
}