package com.altcraft.sdk.push.platform.subscription.status.request

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
import com.altcraft.sdk.push.data.Constants.PUSH_STATUS_REQUEST
import com.altcraft.sdk.push.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.util.SubFunction.uuid
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Push subscription lookup modes.
 */
internal enum class StatusMode {

    /** Returns the latest subscription regardless of provider. */
    LATEST_SUBSCRIPTION,

    /** Returns the latest subscription for the specified provider. */
    LATEST_FOR_PROVIDER,

    /** Returns the subscription matching the current context. */
    MATCH_CURRENT_CONTEXT
}

/**
 * Describes a request for retrieving the current push subscription status.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property provider Push provider identifier.
 * @property token Push token used as the subscription identifier.
 */
internal data class StatusRequestModel(
    val url: String,
    val requestId: String,
    val provider: String?,
    val token: String?,
) : RequestData<ResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.GetWithParams(
            url = url,
            requestId = requestId,
            query = buildMap {
                if (!provider.isNullOrEmpty()) put(
                    PROVIDER, provider
                )

                if (!token.isNullOrEmpty()) put(
                    SUBSCRIPTION_ID, token
                )
            }
        )

    /** Internal request identifier. */
    override val requestName: String = PUSH_STATUS_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.statusRequestSuccess

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.statusRequestFailed

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
         * Builds request data for the current push subscription status.
         *
         * Resolves the provider and token according to the specified lookup mode.
         *
         * @param context Application context.
         * @param mode Subscription lookup mode.
         * @param provider Optional provider for provider-specific lookup.
         * @return Push status request data, or `null` if an error occurs.
         */
        suspend fun of(
            context: Context,
            mode: StatusMode,
            provider: String? = null,
        ): StatusRequestModel? {
            return try {
                val environment = Environment.create(context)
                val pushToken = environment.savedToken()
                    ?: environment.currentToken()

                val (resolvedProvider, token) = when (mode) {
                    StatusMode.LATEST_SUBSCRIPTION -> null to null
                    StatusMode.MATCH_CURRENT_CONTEXT -> pushToken.provider to pushToken.token
                    StatusMode.LATEST_FOR_PROVIDER -> (provider ?: pushToken.provider) to null
                }

                StatusRequestModel(
                    url = Path.pushStatusPath(),
                    requestId = uuid(),
                    provider = resolvedProvider,
                    token = token,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getPushStatusRequestData", e)
                null
            }
        }
    }
}
