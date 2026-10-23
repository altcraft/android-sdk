package com.altcraft.sdk.push.platform.token.request

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
import com.altcraft.sdk.push.data.Constants.SYNC
import com.altcraft.sdk.push.data.Constants.TOKEN_UPDATE_REQUEST
import com.altcraft.sdk.push.platform.token.request.UpdateJson.createTokenUpdateJson
import com.altcraft.sdk.util.SubFunction.uuid
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Converts a Boolean value to its query parameter representation.
 */
private fun Boolean.toQueryValue(): String = if (this) "1" else "0"

/**
 * Describes a request for replacing the current push token or provider.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property oldToken Previously registered push token.
 * @property newToken New push token.
 * @property oldProvider Previously registered push provider.
 * @property newProvider New push provider.
 * @property sync Whether the request should be processed synchronously.
 */
internal data class UpdateRequestModel(
    val url: String,
    val requestId: String,
    val oldToken: String?,
    val newToken: String,
    val oldProvider: String?,
    val newProvider: String,
    val sync: Boolean,
) : RequestData<ResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = mapOf(
                PROVIDER to newProvider,
                SUBSCRIPTION_ID to newToken,
                SYNC to sync.toQueryValue()
            ),
            body = createTokenUpdateJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = TOKEN_UPDATE_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.tokenUpdateRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.tokenUpdateRequestFailed

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
         * Builds request data for updating the current push token.
         *
         * @param context Application context.
         * @param requestId Existing request identifier, or `null` to generate one.
         * @return Token update request data, or `null` if required data
         * is unavailable or an error occurs.
         */
        suspend fun of(
            context: Context,
            requestId: String?,
        ): UpdateRequestModel? {
            return try {
                val environment = Environment.create(context)
                val currentToken = environment.currentToken()
                val savedToken = environment.savedToken()
                val url = Path.tokenUpdatePath()

                UpdateRequestModel(
                    url = url,
                    requestId = requestId ?: uuid(),
                    oldToken = savedToken?.token,
                    newToken = currentToken.token,
                    oldProvider = savedToken?.provider,
                    newProvider = currentToken.provider,
                    sync = environment.usesRTokenAuth(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getTokenUpdateRequestData", e)
                null
            }
        }
    }
}
