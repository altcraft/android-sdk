package com.altcraft.sdk.inapp.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.inapp.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes in-app event requests.
 */
internal object EventRequest {

    /**
     * Handles the delivery of an in-app event to the server.
     *
     * @param context The application context.
     * @param event The in-app event entity containing event details.
     * @return An [Event] representing the result of the request.
     */
    suspend fun inAppEventRequest(
        context: Context,
        event: InAppEventEntity
    ): Event {
        return try {
            val data = EventRequestModel.of(context, event) ?: exception(
                ModuleEvents.eventRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data).event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("inAppEventRequest", e)
        }
    }
}
