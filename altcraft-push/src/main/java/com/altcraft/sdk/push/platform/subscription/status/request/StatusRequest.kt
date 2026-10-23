package com.altcraft.sdk.push.platform.subscription.status.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.push.events.ModuleEvents
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes push subscription status requests.
 */
internal object StatusRequest {

    /**
     * Executes a push subscription status request.
     *
     * Retrieves the current subscription status according to the specified mode.
     *
     * @param context Application context.
     * @param mode Subscription lookup mode.
     * @param provider Optional provider used by provider-specific modes.
     * @return API response containing the request result.
     */
    suspend fun pushStatusRequest(
        context: Context,
        mode: StatusMode,
        provider: String? = null,
    ): ApiResponse<ResponseModel.ResponseWithProfile> {
        return try {
            val data = StatusRequestModel.of(
                context = context, mode = mode, provider = provider
            ) ?: exception(
                ModuleEvents.statusRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse(event = retry("pushStatusRequest", e))
        }
    }
}
