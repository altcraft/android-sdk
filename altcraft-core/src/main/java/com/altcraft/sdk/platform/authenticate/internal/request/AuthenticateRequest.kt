package com.altcraft.sdk.platform.authenticate.internal.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithAuth
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Enables functions for executing server requests.
 */
internal object AuthenticateRequest {

    /**
     * Sends an authorization request and returns the parsed response.
     *
     * @param context The application context.
     * @param entity The authorization request entity containing retryable request state.
     * @return A [ApiResponse] with a [ResponseWithAuth] if successful, or `null` if the request or parsing fails.
     */
    suspend fun authenticateRequest(
        context: Context,
        entity: AuthRequestEntity
    ): ApiResponse<ResponseWithAuth> {
        return try {
            val data = AuthenticateRequestModel.of(context, entity) ?: exception(
                CoreEvents.authorizeRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse(event = retry("authenticateRequest", e))
        }
    }
}
