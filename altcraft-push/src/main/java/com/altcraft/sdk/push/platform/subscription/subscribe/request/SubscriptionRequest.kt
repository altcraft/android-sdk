package com.altcraft.sdk.push.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.data.Constants.SUBSCRIBED
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.util.Utils.hasNotificationPermission
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes push subscription requests.
 */
internal object SubscriptionRequest {

    /**
     * Executes a push subscription request.
     *
     * Checks notification permission for subscription requests, builds request
     * data, executes the request, and returns the resulting SDK event.
     *
     * @param context Application context.
     * @param entity Stored push subscription request.
     * @return Resulting SDK event.
     */
    suspend fun pushSubscribeRequest(
        context: Context,
        entity: PushSubscribeEntity
    ): Event {
        return try {
            if (!hasNotificationPermission(context) && entity.status == SUBSCRIBED) exception(
                ModuleEvents.pushPermissionDenied
            )

            val data = SubscribeRequestModel.of(context, entity) ?: exception(
                ModuleEvents.subscribeRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data).event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("subscribeRequest", e)
        }
    }
}
