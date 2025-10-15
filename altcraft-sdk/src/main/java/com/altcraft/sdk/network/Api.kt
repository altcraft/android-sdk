package com.altcraft.sdk.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

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
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.*

/**
 * Interface for server API calls related to push subscriptions.
 */
internal interface Api {
    /**
     * Subscribes a device for push notifications.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param requestId Unique request ID.
     * @param provider Push provider.
     * @param matchingMode Optional user matching mode.
     * @param sync Optional sync flag (1 or null).
     * @param requestBody Subscription data in JSON.
     * @return Server response.
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun subscribe(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Request-ID") requestId: String,
        @Query("provider") provider: String,
        @Query("matching_mode") matchingMode: String? = null,
        @Query("sync") sync: Int? = null,
        @Body requestBody: JsonObject
    ): Response<JsonElement>

    /**
     * Sends a push event (open, delivery) to the server.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param requestId Unique request ID.
     * @param matchingMode Optional matching mode.
     * @param requestBody Event data in JSON.
     * @return Server response.
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun pushEvent(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Request-ID") requestId: String,
        @Query("matching_mode") matchingMode: String,
        @Body requestBody: JsonObject
    ): Response<JsonElement>

    /**
     * Updates an existing push subscription token.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param requestId Unique request ID.
     * @param provider Push provider.
     * @param saveToken Token to replace.
     * @param requestBody New token data in JSON.
     * @return Server response.
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun update(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Request-ID") requestId: String,
        @Query("provider") provider: String?,
        @Query("subscription_id") saveToken: String?,
        @Body requestBody: JsonObject
    ): Response<JsonElement>

    /**
     * Sends an unSuspend request to the server.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param requestId Unique request identifier.
     * @param provider Push provider name.
     * @param saveToken Push token.
     * @param requestBody JSON body of the request.
     * @return Server response.
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun unSuspend(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Request-ID") requestId: String,
        @Query("provider") provider: String?,
        @Query("subscription_id") saveToken: String?,
        @Body requestBody: JsonObject
    ): Response<JsonElement>

    /**
     * Subscribes a device for push notifications.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param requestId Unique request ID.
     * @param matchingMode Optional user matching mode.
     * @param deviceToken save device token.
     * @return Server response.
     */
    @GET
    @Headers("Content-Type: application/json")
    suspend fun getProfile(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Request-ID") requestId: String,
        @Query("matching_mode") matchingMode: String,
        @Query("provider") provider: String?,
        @Query("subscription_id") deviceToken: String?
    ): Response<JsonElement>

    /**
     * Sends a mobile event to the server.
     *
     * @param url Target endpoint.
     * @param authHeader Authorization token.
     * @param sid The string ID of the pixel.
     * @param tracker Event tracker.
     * @param type Event type.
     * @param version Request version.
     * @param parts Event payload
     * @return Server response.
     */
    @Multipart
    @POST
    suspend fun mobileEvent(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Query("i") sid: String,
        @Query("tr") tracker: String,
        @Query("t") type: String,
        @Query("v") version: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonElement>
}