package com.altcraft.sdk.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Interface for SDK server API calls:
 * subscriptions, updates, push/mobile events, and profile requests.
 */
internal interface Api {

    /**
     * Executes a generic GET request.
     */
    @GET
    suspend fun get(
        @Url url: String,
        @Header("Authorization") authHeader: String?,
        @Header("Request-ID") requestId: String?,
        @Query("matching_mode") matchingMode: String?,
        @QueryMap query: Map<String, String>
    ): Response<JsonElement>

    /**
     * Executes a generic POST request with a JSON body.
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun postJson(
        @Url url: String,
        @Header("Authorization") authHeader: String?,
        @Header("Request-ID") requestId: String,
        @Query("matching_mode") matchingMode: String?,
        @QueryMap query: Map<String, String>,
        @Body body: JsonObject
    ): Response<JsonElement>

    /**
     * Executes a generic POST multipart request.
     */
    @Multipart
    @POST
    suspend fun postMultipart(
        @Url url: String,
        @Header("Authorization") authHeader: String?,
        @Header("Request-ID") requestId: String,
        @Query("matching_mode") matchingMode: String?,
        @QueryMap query: Map<String, String>,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonElement>
}
