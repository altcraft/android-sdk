package com.altcraft.sdk.inapp.platform.placements.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel.ResponseWithInApps
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes in-app placements requests.
 */
internal object PlacementsRequest {

    /**
     * Requests available in-app placements.
     *
     * @param context Application context.
     * @param state Stored in-app state placements.
     * @return Result of the request as [ApiResponse].
     */
    suspend fun inAppPlacementsRequest(
        context: Context,
        state: InAppsEntity,
        requestId: String?
    ): ApiResponse<ResponseWithInApps> {
        return try {
            val data = PlacementsRequestModel.of(state, requestId) ?: exception(
                ModuleEvents.placementsRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse(event = retry("inAppPlacementsRequest", e))
        }
    }
}
