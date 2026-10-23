package com.altcraft.sdk.target.platform.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.util.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.target.data.Constants.TARGET_NAME_KEY
import com.altcraft.sdk.target.data.Constants.TARGET_REQUEST
import com.altcraft.sdk.target.data.Constants.TRACKER_MOB
import com.altcraft.sdk.target.data.Constants.TYPE_MOB
import com.altcraft.sdk.target.data.Constants.VERSION_MOB
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.AsyncResponse
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.events.ModuleEvents
import com.altcraft.sdk.target.platform.target.request.TargetParts.createRequestParts
import com.altcraft.sdk.target.util.Log.MODULE_PREFIX
import okhttp3.MultipartBody
import kotlinx.serialization.KSerializer

/**
 * Describes a target request sent as a multipart POST request.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property sid Pixel identifier.
 * @property name Target event name included in SDK events and logs.
 * @property parts Multipart body parts.
 */
internal data class TargetRequestModel(
    val url: String,
    val requestId: String,
    val sid: String,
    val name: String,
    val parts: List<MultipartBody.Part>,
) : RequestData<AsyncResponse> {

    /**
     * Network request model passed to the SDK request executor.
     *
     * Contains all transport-level data required to perform the multipart POST request.
     */
    override val requestModel: GenericRequest
        get() = GenericRequest.MultipartPost(
            url = url,
            requestId = requestId,
            query = mapOf(
                "i" to sid,
                "tr" to TRACKER_MOB,
                "t" to TYPE_MOB,
                "v" to VERSION_MOB
            ),
            parts = parts
        )

    /**
     * Serializer used to decode the server response into [AsyncResponse].
     */
    override val responseSerializer: KSerializer<AsyncResponse> =
        AsyncResponse.serializer()

    /**
     * SDK event emitted when the target request cannot be completed successfully.
     */
    override val failedEvent = ModuleEvents.requestFailed

    /**
     * SDK event emitted when the target request completes successfully.
     */
    override val successEvent = ModuleEvents.requestSuccessful

    /**
     * Additional data attached to the success or failure SDK event.
     *
     * Includes the target event name under [TARGET_NAME_KEY].
     */
    override val eventData: Map<String, Any?>
        get() = mapOf(
            TARGET_NAME_KEY to name
        )

    /**
     * Additional contextual data included in SDK logs for this request.
     *
     * Includes the target event name under [TARGET_NAME_KEY].
     */
    override val loggerData: Map<String, Any?>
        get() = mapOf(
            TARGET_NAME_KEY to name
        )

    /**
     * Prefix used to identify target-module entries in SDK logs.
     */
    override val moduleLogPrefix = MODULE_PREFIX

    /**
     * Internal request type identifier used in SDK logs and request processing.
     */
    override val requestName: String = TARGET_REQUEST

    companion object {

        /**
         * Builds request data for a target event operation.
         *
         * @param event Stored target event entity.
         * @return Target request data, or `null` if an error occurs.
         */
         fun of(event: Entity): TargetRequestModel? {
            return try {
                val url = Path.targetPath()
                val parts = createRequestParts(
                    event
                ) ?: exception(
                    ModuleEvents.partsIsNull
                )

                TargetRequestModel(
                    url = url,
                    requestId = event.requestID,
                    sid = event.sid,
                    name = event.eventName,
                    parts = parts
                )
            } catch (e: Exception) {
                error("getTargetRequestData", e)
                null
            }
        }
    }
}
