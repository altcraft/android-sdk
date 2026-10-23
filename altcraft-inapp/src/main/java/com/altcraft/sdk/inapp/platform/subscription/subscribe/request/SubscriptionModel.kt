package com.altcraft.sdk.inapp.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel as CoreResponseModel
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.util.Log.MODULE_PREFIX
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.inapp.data.Constants.IN_APP_SUBSCRIBE_REQUEST
import com.altcraft.sdk.inapp.data.Constants.IN_APP_UNSUBSCRIBE_REQUEST
import com.altcraft.sdk.inapp.data.Constants.SUBSCRIBED
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.subscription.subscribe.request.SubscriptionJson.createSubscriptionJson
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * Describes a request for creating or updating an in-app subscription.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property time Request creation time in epoch milliseconds.
 * @property deviceId Device ID as UUIDv4 string.
 * @property acid Profile ACID value.
 * @property status Subscription status.
 * @property sync Whether the request should be processed synchronously.
 * @property profileFields Profile fields payload.
 * @property fields Additional subscription fields payload.
 * @property cats Category list.
 * @property replace Whether existing subscription data should be replaced.
 * @property skipTriggers Whether subscription triggers should be skipped.
 */
internal data class SubscriptionRequestModel(
    val url: String,
    val requestId: String,
    val time: Long,
    val deviceId: String,
    val acid: String,
    val status: String,
    val sync: Boolean,
    val profileFields: JsonElement?,
    val fields: JsonElement?,
    val cats: List<CategoryData>?,
    val replace: Boolean?,
    val skipTriggers: Boolean?,
) : RequestData<CoreResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = emptyMap(),
            body = createSubscriptionJson(this)
        )

    /** Request identifier selected according to the subscription status. */
    override val requestName: String
        get() = when (status) {
            SUBSCRIBED -> IN_APP_SUBSCRIBE_REQUEST
            else -> IN_APP_UNSUBSCRIBE_REQUEST
        }

    /** Success event selected according to the subscription status. */
    override val successEvent: SDKEvent
        get() = when (status) {
            SUBSCRIBED -> ModuleEvents.subscribeRequestSuccessful
            else -> ModuleEvents.unsubscribeRequestSuccessful
        }

    /** Failure event selected according to the subscription status. */
    override val failedEvent: SDKEvent
        get() = when (status) {
            SUBSCRIBED -> ModuleEvents.subscribeRequestFailed
            else -> ModuleEvents.unsubscribeRequestFailed
        }

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
         * Builds request data for an in-app subscription operation.
         *
         * Combines the in-app subscription endpoint from [Path.inAppSubscribePath]
         * with values from [subscription]. Returns `null` if required data is
         * unavailable or an error occurs.
         *
         * @param subscription Source entity with in-app subscription payload data.
         * @return A [SubscriptionRequestModel] instance or `null` on failure.
         */
        fun of(
            subscription: InAppSubscribeEntity
        ): SubscriptionRequestModel? {
            return try {
                SubscriptionRequestModel(
                    url = Path.inAppSubscribePath(),
                    requestId = subscription.requestID,
                    time = subscription.time,
                    acid = subscription.acid,
                    deviceId = subscription.deviceId,
                    status = subscription.status,
                    sync = subscription.sync == true,
                    profileFields = subscription.profileFields,
                    fields = subscription.customFields,
                    cats = subscription.cats,
                    replace = subscription.replace,
                    skipTriggers = subscription.skipTriggers
                )
            } catch (e: Exception) {
                error("getSubscribeData", e)
                null
            }
        }
    }
}
