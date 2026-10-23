package com.altcraft.sdk.inapp.platform.subscription.status.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
import com.altcraft.sdk.inapp.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes in-app subscription status requests.
 */
internal object StatusRequest {

    /**
     * Sends an in-app subscription status request.
     *
     * @param context Context for resolving config and tokens.
     * @return Result of the request as [ApiResponse].
     */
    suspend fun inAppStatusRequest(
        context: Context,
    ): ApiResponse<ResponseWithProfile> {
        return try {
            val data = StatusRequestModel.of(context) ?: exception(
                ModuleEvents.statusRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse(event = retry("inAppStatusRequest", e))
        }
    }
}