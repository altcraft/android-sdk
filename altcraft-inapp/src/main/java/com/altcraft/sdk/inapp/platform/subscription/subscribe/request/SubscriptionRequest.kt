package com.altcraft.sdk.inapp.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.inapp.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes in-app subscription requests.
 */
internal object SubscriptionRequest {

    /**
     * Performs an in-app subscription request.
     *
     * @param context The application context.
     * @param entity An [InAppSubscribeEntity] containing in-app subscription details.
     * @return A [Event] representing the result of the in-app subscription request.
     */
    suspend fun inAppSubscribeRequest(
        context: Context,
        entity: InAppSubscribeEntity
    ): Event {
        return try {
            val data = SubscriptionRequestModel.of(entity) ?: exception(
                ModuleEvents.subscribeRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data)
                .event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("subscribeRequest", e)
        }
    }
}
