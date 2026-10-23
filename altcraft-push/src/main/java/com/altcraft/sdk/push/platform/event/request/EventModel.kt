package com.altcraft.sdk.push.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.push.data.Constants.TYPE_KEY
import com.altcraft.sdk.push.data.Constants.UID_KEY
import com.altcraft.sdk.push.data.room.entity.PushEventEntity
import com.altcraft.sdk.push.platform.event.request.EventJson.createPushEventJson
import com.altcraft.sdk.push.util.Log.MODULE_PREFIX
import kotlinx.serialization.KSerializer

/**
 * Describes a request for sending a push notification event.
 *
 * Push events include notification delivery and notification opening.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property uid Push notification identifier.
 * @property time Event creation time.
 * @property type Push event type.
 */
internal data class EventRequestModel(
    val url: String,
    val requestId: String,
    val uid: String,
    val time: Long,
    val type: String,
) : RequestData<ResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = emptyMap(),
            body = createPushEventJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = PUSH_EVENT_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.eventRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.eventRequestFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?>
        get() = mapOf(
            UID_KEY to uid,
            TYPE_KEY to type
        )

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?>
        get() = mapOf(
            TYPE_KEY to type
        )

    /** Prefix used to identify push-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /** Serializer used to decode the server response. */
    override val responseSerializer:
            KSerializer<ResponseModel.ResponseWithProfile>
        get() = ResponseModel.ResponseWithProfile.serializer()

    companion object {

        /**
         * Builds request data for a push event operation.
         *
         * @param event Stored push event entity.
         * @return Push event request data, or `null` if an error occurs.
         */
        fun of(
            event: PushEventEntity
        ): EventRequestModel? {
            return try {
                val url = Path.eventPushPath(
                    event.type,
                )

                EventRequestModel(
                    url = url,
                    uid = event.uid,
                    time = event.time,
                    type = event.type,
                    requestId = event.requestID
                )
            } catch (e: Exception) {
                error("getPushEventRequestData", e)
                null
            }
        }
    }
}
