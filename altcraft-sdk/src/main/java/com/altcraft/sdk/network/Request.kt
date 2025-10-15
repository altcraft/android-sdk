package com.altcraft.sdk.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.checkingNotificationPermission
import com.altcraft.sdk.data.Constants.LATEST_SUBSCRIPTION
import com.altcraft.sdk.data.Constants.LATEST_FOR_PROVIDER
import com.altcraft.sdk.data.Constants.MATCH_CURRENT_CONTEXT
import com.altcraft.sdk.data.Constants.TRACKER_MOB
import com.altcraft.sdk.data.Constants.TYPE_MOB
import com.altcraft.sdk.data.Constants.VERSION_MOB
import com.altcraft.sdk.network.Response.processResponse
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Repository.getMobileEventRequestData
import com.altcraft.sdk.data.Repository.getStatusRequestData
import com.altcraft.sdk.data.Repository.getPushEventRequestData
import com.altcraft.sdk.data.Repository.getSubscribeRequestData
import com.altcraft.sdk.data.Repository.getUnSubscribeRequestData
import com.altcraft.sdk.data.Repository.getUpdateRequestData
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.sdk_events.EventList.pushEventRequestDataIsNull
import com.altcraft.sdk.sdk_events.EventList.permissionDenied
import com.altcraft.sdk.sdk_events.EventList.profileRequestDataIsNull
import com.altcraft.sdk.sdk_events.EventList.pushSubscribeRequestDataIsNull
import com.altcraft.sdk.sdk_events.EventList.tokenUpdateRequestDataIsNull
import com.altcraft.sdk.sdk_events.EventList.unSuspendRequestDataIsNull
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.json.JsonFactory
import com.altcraft.sdk.mob_events.PartsFactory.createMobileEventParts
import com.altcraft.sdk.network.Network.getRetrofit
import com.altcraft.sdk.sdk_events.EventList.mobileEventPartsIsNull
import com.altcraft.sdk.sdk_events.EventList.mobEventRequestDataIsNull

/**
 * Enables functions for executing server requests.
 */
internal object Request {
    
    /**
     * Executes the subscription process by checking permissions, retrieving necessary data,
     * and sending a request to the server via Retrofit.
     *
     * @param context The application context used for permission checks and data retrieval.
     * @param item A `SubscribeEntity` object containing subscription details.
     * @return `DataClasses.Event`, representing the outcome of the operation.
     *
     * @throws Exception If an error occurs, the function triggers a retry (`retry`).
     */
    suspend fun subscribeRequest(
        context: Context,
        item: SubscribeEntity
    ): DataClasses.Event {
        return try {
            if (!checkingNotificationPermission(context)) exception(permissionDenied)
            val data =
                getSubscribeRequestData(context, item) ?: exception(pushSubscribeRequestDataIsNull)

            val json = JsonFactory.createSubscribeJson(data)

            val response = getRetrofit().subscribe(
                data.url,
                data.authHeader,
                data.uid,
                data.provider,
                data.matchingMode,
                data.sync,
                json
            )

            processResponse(data, response)
        } catch (e: Exception) {
            retry("subscribeRequest", e)
        }
    }

    /**
     * Handles the process of updating a device token.
     *
     * This function retrieves the update request data, validates required authentication parameters,
     * and sends the token update request using the `update` API call. If the request fails,
     * it triggers a retry mechanism.
     *
     * @param context The application context.
     * @param requestId The unique identifier for the update request.
     * @return A [DataClasses.Event] representing the outcome of the update process.
     */
    suspend fun updateRequest(
        context: Context,
        requestId: String
    ): DataClasses.Event {
        return try {
            val data =
                getUpdateRequestData(context, requestId) ?: exception(tokenUpdateRequestDataIsNull)
            val json = JsonFactory.createUpdateJson(data)

            val response = getRetrofit().update(
                data.url,
                data.authHeader,
                data.uid,
                data.newProvider,
                data.oldToken,
                json
            )

            processResponse(data, response)

        } catch (e: Exception) {
            retry("updateRequest", e)
        }
    }

    /**
     * Handles the delivery of a push event to the server.
     *
     * This function retrieves the necessary request data, validates required authentication
     * parameters, and sends the push event using the `pushEvent` API call. If the request fails
     * due to a server error or network issues, it retries the operation.
     *
     * @param context The application context.
     * @param event The push event entity containing event details.
     * @return A [DataClasses.Event] representing the result of the request.
     */
    suspend fun pushEventRequest(
        context: Context,
        event: PushEventEntity
    ): DataClasses.Event {
        return try {
            val data =
                getPushEventRequestData(context, event) ?: exception(pushEventRequestDataIsNull)
            val json = JsonFactory.createPushEventJson(data)

            val response = getRetrofit().pushEvent(
                data.url,
                data.authHeader,
                data.uid,
                data.matchingMode,
                json
            )

            processResponse(data, response)

        } catch (e: Exception) {
            retry("eventRequest", e)
        }
    }

    /**
     * Handles the delivery of a mobile event to the server.
     *
     * This function retrieves the necessary request data, validates required authentication
     * parameters, and sends the mobile event using the `mobileEvent` API call. If the request fails
     * due to a server error or network issues, it retries the operation.
     *
     * @param context The application context.
     * @param event   The mobile event entity containing event details.
     * @return A [DataClasses.Event] representing the result of the request.
     */
    suspend fun mobileEventRequest(
        context: Context,
        event: MobileEventEntity
    ): DataClasses.Event {
        val func = "mobileEventRequest"
        return try {
            val data =
                getMobileEventRequestData(context, event) ?: exception(mobEventRequestDataIsNull)
            val parts = createMobileEventParts(event) ?: return error(func, mobileEventPartsIsNull)

            val response = getRetrofit().mobileEvent(
                data.url,
                data.authHeader,
                data.sid,
                TRACKER_MOB,
                TYPE_MOB,
                VERSION_MOB,
                parts = parts
            )

            processResponse(data, response)
        } catch (e: Exception) {
            retry("mobileEventRequest", e)
        }
    }

    /**
     * Executes an `unSuspend` request used during the user re-authentication process.
     *
     * This function is called when a user needs to be re-logged into the system.
     * If the request fails due to a network or server error, it will trigger the error handler.
     *
     * @param context The application context.
     * @return A [DataClasses.Event] representing the result of the API call.
     */
    suspend fun unSuspendRequest(
        context: Context,
    ): DataClasses.Event {
        return try {
            val data = getUnSubscribeRequestData(context) ?: exception(unSuspendRequestDataIsNull)
            val json = JsonFactory.createUnSuspendJson(data)

            val response = getRetrofit().unSuspend(
                data.url,
                data.authHeader,
                data.uid,
                data.provider,
                data.token,
                json
            )

            processResponse(data, response)

        } catch (e: Exception) {
            error("profileRequest", e)
        }
    }

    /**
     * Sends a subscription status request using the specified matching [mode].
     *
     * - [LATEST_SUBSCRIPTION]: latest subscription overall
     * - [LATEST_FOR_PROVIDER]: latest subscription for a given provider
     * - [MATCH_CURRENT_CONTEXT]: subscription matching current token and profile
     *
     * @param context Application context for resolving config and tokens.
     * @param mode Matching mode used to determine provider and token.
     * @param targetProvider Optional provider override for [LATEST_FOR_PROVIDER] mode.
     * @return Result of the request as [DataClasses.Event].
     */
    suspend fun statusRequest(
        context: Context,
        mode: String,
        targetProvider: String? = null
    ): DataClasses.Event {
        return try {
            val data = getStatusRequestData(context) ?: exception(profileRequestDataIsNull)

            val (provider, token) = when (mode) {
                LATEST_SUBSCRIPTION -> null to null
                MATCH_CURRENT_CONTEXT -> data.provider to data.token
                else -> (targetProvider ?: data.provider) to null
            }

            val response = getRetrofit().getProfile(
                data.url,
                data.authHeader,
                data.uid,
                data.matchingMode,
                provider,
                token
            )

            processResponse(data, response)

        } catch (e: Exception) {
            error("profileRequest", e)
        }
    }
}