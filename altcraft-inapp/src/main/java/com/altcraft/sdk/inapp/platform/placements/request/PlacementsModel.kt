package com.altcraft.sdk.inapp.platform.placements.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.util.Log.MODULE_PREFIX
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_REQUEST
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel.ResponseWithInApps
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.placements.request.PlacementsJson.createPlacementsJson
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.util.SubFunction.uuid
import kotlinx.serialization.KSerializer

/**
 * Describes a request for retrieving in-app placement campaigns.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property acid Profile ACID value.
 * @property deviceId Device ID as UUIDv4 string.
 * @property lastPublish Last known in-app publish timestamp.
 * @property placementRulesChecksum Last known placement rules checksum.
 */
internal data class PlacementsRequestModel(
    val url: String,
    val requestId: String,
    val acid: String,
    val deviceId: String,
    val lastPublish: Long,
    val placementRulesChecksum: String,
) : RequestData<ResponseWithInApps> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = emptyMap(),
            body = createPlacementsJson(this)
        )

    /** Internal request identifier. */
    override val requestName: String = IN_APP_PLACEMENTS_REQUEST

    /** SDK event emitted when the request succeeds. */
    override val successEvent: SDKEvent =
        ModuleEvents.placementsRequestSuccessful

    /** SDK event emitted when the request fails. */
    override val failedEvent: SDKEvent =
        ModuleEvents.placementsRequestFailed

    /** Additional data attached to emitted SDK events. */
    override val eventData: Map<String, Any?> = emptyMap()

    /** Additional contextual data attached to request logs. */
    override val loggerData: Map<String, Any?> = emptyMap()

    /** Prefix used to identify in-app-module entries in SDK logs. */
    override val moduleLogPrefix = MODULE_PREFIX

    /** Serializer used to decode the server response. */
    override val responseSerializer:
            KSerializer<ResponseWithInApps>
        get() = ResponseWithInApps.serializer()

    companion object {

        /**
         * Builds request data for an in-app placements request.
         *
         * Combines the in-app placements endpoint from [Path.inAppPlacementsPath]
         * with stored in-app placement cache state from [state]. Returns `null`
         * if required data is unavailable or an error occurs.
         *
         * @param state Source entity with in-app placements request data.
         * @param requestId Optional request identifier; a new UUID is generated when null.
         * @return A [PlacementsRequestModel] instance or `null` on failure.
         */
        fun of(
            state: InAppsEntity,
            requestId: String?
        ): PlacementsRequestModel? {
            return try {
                PlacementsRequestModel(
                    url = Path.inAppPlacementsPath(),
                    requestId = requestId ?: uuid(),
                    acid = state.acid,
                    deviceId = state.deviceId,
                    lastPublish = state.lastPublish,
                    placementRulesChecksum = state.placementRulesChecksum
                )
            } catch (e: Exception) {
                error("getInAppPlacementsRequestData", e)
                null
            }
        }
    }
}
