package com.altcraft.sdk.network.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody

/**
 * Generic HTTP request model for module execution via [RequestExecutor].
 *
 * [url] holds the request path (relative to the core API URL). The core
 * [RequestExecutor] prepends the resolved API URL before sending a request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface GenericRequest {
    val url: String

    /**
     * Raw GET request without Authorization, Request-ID, matching mode,
     * or query parameters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Get(
        override val url: String
    ) : GenericRequest

    /**
     * Authenticated GET request with Request-ID, matching mode,
     * and query parameters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class GetWithParams(
        override val url: String,
        val requestId: String,
        val query: Map<String, String?> = emptyMap()
    ) : GenericRequest

    /**
     * Authenticated POST request with a JSON body, Request-ID,
     * matching mode, and query parameters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class JsonPost(
        override val url: String,
        val requestId: String,
        val query: Map<String, String?> = emptyMap(),
        val body: JsonObject
    ) : GenericRequest

    /**
     * Authenticated multipart POST request with Request-ID,
     * matching mode, query parameters, and multipart body parts.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class MultipartPost(
        override val url: String,
        val requestId: String,
        val query: Map<String, String?> = emptyMap(),
        val parts: List<MultipartBody.Part>
    ) : GenericRequest
}