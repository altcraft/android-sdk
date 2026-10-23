package com.altcraft.sdk.profile.platform.updateFields.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.profile.util.Log.error
import com.altcraft.sdk.profile.util.Log.MODULE_PREFIX
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.profile.data.Constants.PROFILE_REQUEST
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.AsyncResponse
import com.altcraft.sdk.profile.data.room.Entity
import com.altcraft.sdk.profile.events.ModuleEvents
import com.altcraft.sdk.profile.platform.updateFields.request.UpdateFieldsJson.createRequestJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Describes a profile update request sent as a JSON POST request.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property profileFields Profile fields payload.
 * @property skipTriggers Flag to skip triggers on the server side.
 */
internal data class UpdateFieldsRequestModel(
    val url: String,
    val requestId: String,
    val profileFields: JsonElement?,
    val skipTriggers: Boolean?,
) : RequestData<AsyncResponse> {

    /**
     * Network request model passed to the SDK request executor.
     */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = emptyMap(),
            body = createRequestJson(this)
        )

    /**
     * Serializer used to decode the server response into [AsyncResponse].
     */
    override val responseSerializer: KSerializer<AsyncResponse> =
        AsyncResponse.serializer()

    /**
     * SDK event emitted when the profile update request cannot be completed successfully.
     */
    override val failedEvent = ModuleEvents.requestFailed

    /**
     * SDK event emitted when the profile update request completes successfully.
     */
    override val successEvent = ModuleEvents.requestSuccessful

    /**
     * Additional data attached to the success or failure SDK event.
     */
    override val eventData: Map<String, Any?>
        get() = emptyMap()

    /**
     * Additional contextual data included in SDK logs for this request.
     */
    override val loggerData: Map<String, Any?>
        get() = emptyMap()

    /** Prefix used to identify profile-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /**
     * Internal request type identifier used in SDK logs and request processing.
     */
    override val requestName: String = PROFILE_REQUEST

    companion object {

        /**
         * Builds request data for a profile update operation.
         *
         * @param entity Stored profile update entity.
         * @return Profile update request data, or `null` if an error occurs.
         */
        fun of(
            entity: Entity,
        ): UpdateFieldsRequestModel? {
            return try {
                val url = Path.profileUpdatePath()

                UpdateFieldsRequestModel(
                    url = url,
                    requestId = entity.requestID,
                    profileFields = entity.profileFields,
                    skipTriggers = entity.skipTriggers
                )
            } catch (e: Exception) {
                error("getProfileUpdateRequestData", e)
                null
            }
        }
    }
}
