package com.altcraft.sdk.profile.platform.updateFields.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.profile.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.profile.data.room.Entity
import com.altcraft.sdk.profile.events.ModuleEvents
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes profile update requests.
 */
internal object UpdateFieldsRequest {

    /**
     * Executes a profile update request.
     *
     * Builds request data, executes the request, and returns the resulting SDK event.
     *
     * @param context Application context.
     * @param entity Stored profile update entity.
     * @return Resulting SDK event.
     */
    suspend fun updateFieldsRequest(
        context: Context,
        entity: Entity,
    ): Event {
        return try {
            val data =
                UpdateFieldsRequestModel.of(entity) ?: exception(
                    ModuleEvents.requestDataIsNull
                )

            RequestExecutor.execute(context = context, requestData = data).event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("request", e)
        }
    }
}
