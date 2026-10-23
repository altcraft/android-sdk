package com.altcraft.sdk.push.platform.subscription.status

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithHttpCode
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
import com.altcraft.sdk.push.util.Log
import kotlin.coroutines.cancellation.CancellationException
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.push.data.Constants
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.subscription.status.request.StatusMode
import com.altcraft.sdk.push.platform.subscription.status.request.StatusRequest

/**
 * Provides push subscription status operations.
 */
@Keep
object StatusAPI {

    /**
     * Returns the latest push subscription status.
     *
     * @param context Application context.
     * @return Server response with HTTP status code, or `null` if the request fails.
     */
    suspend fun latest(
        context: Context
    ): ResponseWithHttpCode<ResponseWithProfile>? {
        return try {
            val response = StatusRequest.pushStatusRequest(
                context,
                StatusMode.LATEST_SUBSCRIPTION
            )

            ResponseWithHttpCode(
                httpCode = response.httpCode,
                response = response.response
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("push.status.latest", e)
            null
        }
    }

    /**
     * Returns the status matching the current token and provider.
     *
     * @param context Application context.
     * @return Server response with HTTP status code, or `null` if the request fails.
     */
    suspend fun current(
        context: Context
    ): ResponseWithHttpCode<ResponseWithProfile>? {
        return try {
            val response = StatusRequest.pushStatusRequest(
                context,
                StatusMode.MATCH_CURRENT_CONTEXT
            )

            ResponseWithHttpCode(
                httpCode = response.httpCode,
                response = response.response
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("push.status.current", e)
            null
        }
    }

    /**
     * Returns the latest subscription status for the specified provider.
     *
     * @param context Application context.
     * @param provider Optional push provider identifier.
     * @return Server response with HTTP status code, or `null` if the request fails.
     */
    suspend fun latestForProvider(
        context: Context,
        provider: String? = null
    ): ResponseWithHttpCode<ResponseWithProfile>? {
        if (provider != null && provider !in Constants.validProviders) {
            SDKException.exception(ModuleEvents.invalidPushProvider)
        }

        return try {
            val response = StatusRequest.pushStatusRequest(
                context,
                StatusMode.LATEST_FOR_PROVIDER,
                provider
            )

            ResponseWithHttpCode(
                httpCode = response.httpCode,
                response = response.response
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("push.status.latest_for_provider", e)
            null
        }
    }
}
