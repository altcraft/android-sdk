package com.altcraft.sdk.platform.authenticate.internal.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithAuth
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import kotlinx.serialization.KSerializer

/**
 * Describes a request to authorize a user session.
 *
 * @property url API endpoint URL.
 * @property requestId Unique request ID.
 * @property currentAcid Current user ACID.
 * @property startAnonSession Anonymous session start time in Unix time.
 */
internal data class AuthenticateRequestModel(
    val url: String,
    val requestId: String,
    val currentAcid: String?,
    val startAnonSession: Long
) : RequestData<ResponseWithAuth> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            body = AuthenticateJson.createAuthenticateJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = Constants.AUTHORIZE_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent = CoreEvents.authenticateSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent = CoreEvents.authenticateFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?> = emptyMap()

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?> = emptyMap()

    /** Serializer used to decode the server response. */
    override val responseSerializer: KSerializer<ResponseWithAuth> =
        ResponseWithAuth.serializer()

    companion object {

        /**
         * Builds request data for the Altcraft authorize request.
         *
         * Collects the current authorization header and internal user identifiers
         * required to perform the Altcraft authorization call.
         * Returns `null` if the environment cannot be created or an error occurs.
         *
         * @param context The application context.
         * @param entity Source entity with authentication request data.
         * @return An [AuthenticateRequestModel] instance or `null` on failure.
         */
        suspend fun of(
            context: Context,
            entity: AuthRequestEntity
        ): AuthenticateRequestModel? {
            return try {
                val env = Environment.create(context)
                val internal = env.auth.internal()
                AuthenticateRequestModel(
                    url = Path.authenticatePath(),
                    requestId = entity.requestId,
                    currentAcid = internal.acid,
                    startAnonSession = internal.startAnonSession
                )
            } catch (e: Exception) {
                error("getAuthenticateRequestData", e)
                null
            }
        }
    }
}
