package com.altcraft.sdk.inapp.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel as CoreResponseModel
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.util.Log.MODULE_PREFIX
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.inapp.data.Constants.IN_APP_EVENT_REQUEST
import com.altcraft.sdk.inapp.data.Constants.SMID_KEY
import com.altcraft.sdk.inapp.data.Constants.TYPE_KEY
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppEventData
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.event.request.EventJson.createEventJson
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.network.json.Converter.fromStringJson
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Describes a request for sending an in-app event.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property type In-app event type.
 * @property time Event time in epoch milliseconds.
 * @property smid Send Message ID.
 * @property acid Profile ACID value.
 * @property data Optional event payload for `click` and `error` events.
 */
internal data class EventRequestModel(
    val url: String,
    val requestId: String,
    val type: String,
    val time: Long,
    val smid: String,
    val acid: String,
    val data: InAppEventData? = null,
) : RequestData<CoreResponseModel.AsyncResponse> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = emptyMap(),
            body = createEventJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = IN_APP_EVENT_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.eventRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.eventRequestFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?>
        get() = mapOf(
            SMID_KEY to smid,
            TYPE_KEY to type
        )

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?>
        get() = mapOf(
            TYPE_KEY to type
        )

    /** Prefix used to identify in-app-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /** Serializer used to decode the server response. */
    override val responseSerializer:
            KSerializer<CoreResponseModel.AsyncResponse>
        get() = CoreResponseModel.AsyncResponse.serializer()

    companion object {

        /**
         * Builds request data for an in-app event operation.
         *
         * Collects the request URL, authentication data, and event payload values
         * from the current SDK environment and [event].
         *
         * @param context The application context.
         * @param event Source entity with in-app event data.
         * @return An [EventRequestModel] instance or `null` on failure.
         */
        suspend fun of(
            context: Context,
            event: InAppEventEntity
        ): EventRequestModel? {
            return try {
                val env = Environment.create(
                    context
                )

                EventRequestModel(
                    url = Path.eventInAppPath(
                        event.type
                    ),
                    requestId = event.requestID,
                    smid = event.smid,
                    time = event.time,
                    type = event.type,
                    acid = env.internalAuth().acid,
                    data = event.data.fromStringJson(
                        "getInAppEventRequestData"
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getInAppEventRequestData", e)
                null
            }
        }
    }
}
