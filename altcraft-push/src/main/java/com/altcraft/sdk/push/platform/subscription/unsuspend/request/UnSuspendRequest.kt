package com.altcraft.sdk.push.platform.subscription.unsuspend.request

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
 * Executes push unSuspend requests.
 */
internal object UnSuspendRequest {

    /**
     * Executes an unSuspend request.
     *
     * Restores the current profile subscription and suspends subscriptions
     * belonging to other profiles for the same device token.
     *
     * @param context Application context.
     * @return API response containing the request result.
     */
    suspend fun unSuspendRequest(
        context: Context,
    ): ApiResponse<ResponseModel.ResponseWithProfile> {
        return try {
            val data = UnSuspendRequestModel.of(context) ?: exception(
                ModuleEvents.unSuspendRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse(event = retry("unSuspendRequest", e))
        }
    }
}
