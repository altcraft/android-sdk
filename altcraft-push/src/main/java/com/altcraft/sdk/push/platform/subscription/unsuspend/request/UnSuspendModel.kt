package com.altcraft.sdk.push.platform.subscription.unsuspend.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.util.Log.MODULE_PREFIX
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.data.Constants.PROVIDER
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.push.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.util.SubFunction.uuid
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Describes a request for restoring a suspended push subscription.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property provider Push provider identifier.
 * @property token Push token associated with the suspended subscription.
 */
internal data class UnSuspendRequestModel(
    val url: String,
    val requestId: String,
    val provider: String,
    val token: String,
) : RequestData<ResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = mapOf(
                PROVIDER to provider,
                SUBSCRIPTION_ID to token
            ),
            body = UnSuspendJson.createUnSuspendJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = UNSUSPEND_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.unSuspendRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.unSuspendRequestFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?> = emptyMap()

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?> = emptyMap()

    /** Prefix used to identify push-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /** Serializer used to decode the server response. */
    override val responseSerializer:
            KSerializer<ResponseModel.ResponseWithProfile>
        get() = ResponseModel.ResponseWithProfile.serializer()

    companion object {

        /**
         * Builds request data for restoring a suspended push subscription.
         *
         * @param context Application context.
         * @return UnSuspend request data, or `null` if required data
         * is unavailable or an error occurs.
         */
        suspend fun of(
            context: Context,
        ): UnSuspendRequestModel? {
            return try {
                val environment = Environment.create(context)
                val token = environment.currentToken()
                val url = Path.unSuspendPath()

                UnSuspendRequestModel(
                    url = url,
                    requestId = uuid(),
                    provider = token.provider,
                    token = token.token,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getUnSuspendRequestData", e)
                null
            }
        }
    }
}
