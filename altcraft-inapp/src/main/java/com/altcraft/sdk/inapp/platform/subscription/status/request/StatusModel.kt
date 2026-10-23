package com.altcraft.sdk.inapp.platform.subscription.status.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel as CoreResponseModel
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.util.Log.MODULE_PREFIX
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.inapp.data.Constants.IN_APP_STATUS_REQUEST
import com.altcraft.sdk.inapp.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.util.SubFunction.uuid
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Describes a request for retrieving the current in-app subscription status.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property deviceId Device ID used as the subscription identifier.
 */
internal data class StatusRequestModel(
    val url: String,
    val requestId: String,
    val deviceId: String?,
) : RequestData<CoreResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.GetWithParams(
            url = url,
            requestId = requestId,
            query = buildMap {
                if (!deviceId.isNullOrEmpty()) {
                    put(SUBSCRIPTION_ID, deviceId)
                }
            }
        )

    /** Internal request identifier. */
    override val requestName: String = IN_APP_STATUS_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.statusRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.statusRequestFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?> = emptyMap()

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?> = emptyMap()

    /** Prefix used to identify in-app-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /** Serializer used to decode the server response. */
    override val responseSerializer:
            KSerializer<CoreResponseModel.ResponseWithProfile>
        get() = CoreResponseModel.ResponseWithProfile.serializer()

    companion object {

        /**
         * Builds request data for an in-app subscription status operation.
         *
         * Retrieves configuration and the current device ID.
         * Returns `null` if required data is missing or an error occurs.
         *
         * @param context The application context.
         * @return A [StatusRequestModel] instance or `null` on failure.
         */
        suspend fun of(
            context: Context,
        ): StatusRequestModel? {
            return try {
                val env = Environment.create(context)

                StatusRequestModel(
                    url = Path.inAppStatusPath(),
                    requestId = uuid(),
                    deviceId = env.internalAuth().deviceId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getStatusRequestData", e)
                null
            }
        }
    }
}
