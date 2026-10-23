package com.altcraft.sdk.push.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestData
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.data.Constants.PROVIDER
import com.altcraft.sdk.push.data.Constants.PUSH_SUBSCRIBE_REQUEST
import com.altcraft.sdk.push.data.Constants.PUSH_SUSPEND_REQUEST
import com.altcraft.sdk.push.data.Constants.PUSH_UNSUBSCRIBE_REQUEST
import com.altcraft.sdk.push.data.Constants.SUBSCRIBED
import com.altcraft.sdk.push.data.Constants.SUSPENDED
import com.altcraft.sdk.push.data.Constants.SYNC
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.platform.subscription.subscribe.request.SubscriptionJson.createPushSubscribeJson
import com.altcraft.sdk.push.util.Log.MODULE_PREFIX
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.cancellation.CancellationException

/**
 * Converts a Boolean value to its query parameter representation.
 */
private fun Boolean.toQueryValue(): String = if (this) "1" else "0"

/**
 * Describes a request for creating or updating a push subscription.
 *
 * @property url Request endpoint.
 * @property requestId Unique request identifier.
 * @property time Request creation time in epoch milliseconds.
 * @property provider Push provider identifier.
 * @property deviceToken Device push token.
 * @property status Subscription status.
 * @property sync Whether the request should be processed synchronously.
 * @property profileFields Profile fields payload.
 * @property fields Additional subscription fields payload.
 * @property cats Subscription category list.
 * @property replace Whether existing subscription data should be replaced.
 * @property skipTriggers Whether subscription triggers should be skipped.
 */
internal data class SubscribeRequestModel(
    val url: String,
    val requestId: String,
    val time: Long,
    val provider: String,
    val deviceToken: String,
    val status: String,
    val sync: Boolean,
    val profileFields: JsonElement?,
    val fields: JsonElement?,
    val cats: List<CategoryData>?,
    val replace: Boolean?,
    val skipTriggers: Boolean?,
) : RequestData<ResponseModel.ResponseWithProfile> {

    /** Network request passed to the request executor. */
    override val requestModel: GenericRequest
        get() = GenericRequest.JsonPost(
            url = url,
            requestId = requestId,
            query = mapOf(
                PROVIDER to provider,
                SYNC to sync.toQueryValue()
            ),
            body = createPushSubscribeJson(this)
        )

    /** Request identifier selected according to the subscription status. */
    override val requestName: String
        get() = when (status) {
            SUBSCRIBED -> PUSH_SUBSCRIBE_REQUEST
            SUSPENDED -> PUSH_SUSPEND_REQUEST
            else -> PUSH_UNSUBSCRIBE_REQUEST
        }

    /** Success event selected according to the subscription status. */
    override val successEvent: SDKEvent
        get() = when (status) {
            SUBSCRIBED -> ModuleEvents.subscribeRequestSuccessful
            SUSPENDED -> ModuleEvents.suspendRequestSuccessful
            else -> ModuleEvents.unsubscribeRequestSuccessful
        }

    /** Failure event selected according to the subscription status. */
    override val failedEvent: SDKEvent
        get() = when (status) {
            SUBSCRIBED -> ModuleEvents.subscribeRequestFailed
            SUSPENDED -> ModuleEvents.suspendRequestFailed
            else -> ModuleEvents.unsubscribeRequestFailed
        }

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
         * Builds request data for a push subscription operation.
         *
         * @param context Application context.
         * @param subscription Stored push subscription entity.
         * @return Push subscription request data, or `null` if required data
         * is unavailable or an error occurs.
         */
        suspend fun of(
            context: Context,
            subscription: PushSubscribeEntity,
        ): SubscribeRequestModel? {
            return try {
                val environment = Environment.create(context)
                val token = environment.currentToken()
                val url = Path.pushSubscribePath()

                SubscribeRequestModel(
                    url = url,
                    cats = subscription.cats,
                    time = subscription.time,
                    provider = token.provider,
                    deviceToken = token.token,
                    status = subscription.status,
                    sync = subscription.sync == true,
                    replace = subscription.replace,
                    fields = subscription.customFields,
                    requestId = subscription.requestID,
                    skipTriggers = subscription.skipTriggers,
                    profileFields = subscription.profileFields,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("getPushSubscribeRequestData", e)
                null
            }
        }
    }
}
