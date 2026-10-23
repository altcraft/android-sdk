package com.altcraft.sdk.push.platform.token.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.push.events.ModuleEvents
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes push token update requests.
 */
internal object UpdateRequest {

    /**
     * Executes a push token update request.
     *
     * Builds request data, executes the request, and returns the resulting SDK event.
     *
     * @param context Application context.
     * @param requestId Request identifier, or `null` to use the latest pending request.
     * @return Resulting SDK event.
     */
    suspend fun tokenUpdateRequest(
        context: Context,
        requestId: String?,
    ): Event {
        return try {
            val data = UpdateRequestModel.of(context, requestId) ?: exception(
                ModuleEvents.tokenUpdateRequestDataIsNull
            )

            RequestExecutor.execute(context = context, requestData = data).event
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retry("tokenUpdateRequest", e)
        }
    }
}