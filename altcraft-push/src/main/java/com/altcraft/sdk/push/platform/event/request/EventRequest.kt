package com.altcraft.sdk.push.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.data.room.entity.PushEventEntity
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes push event requests.
 */
internal object EventRequest {

    /**
     * Executes a push event request.
     *
     * Builds request data, executes the request, and returns the resulting SDK event.
     *
     * @param context Application context.
     * @param event Stored push event.
     * @return Resulting SDK event.
     */
    suspend fun pushEventRequest(
        context: Context,
        event: PushEventEntity,
    ): Event {
        return try {
            val data = EventRequestModel.of(event) ?: exception(
                ModuleEvents.eventRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
                .event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("pushEventRequest", e)
        }
    }
}
