package com.altcraft.sdk.target.platform.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.target.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.events.ModuleEvents
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes target event requests.
 */
internal object TargetRequest {

    /**
     * Executes a target event request.
     *
     * Builds request data, executes the request, and returns the resulting SDK event.
     *
     * @param context Application context.
     * @param event Stored target event entity.
     * @return Resulting SDK event.
     */
    suspend fun targetRequest(
        context: Context,
        event: Entity,
    ): Event {
        return try {
            val data = TargetRequestModel.of(event) ?: exception(
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