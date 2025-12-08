package com.altcraft.sdk.mob_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.StringBuilder.mobileEventPayloadInvalid
import com.altcraft.sdk.additional.SubFunction.fieldsIsObjects
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.auth.AuthManager.getUserTag
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.concurrency.withInitReady
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.core.Retry.awaitMobileEventRetryStarted
import com.altcraft.sdk.data.Constants.MOB_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.RoomRequest.allMobileEventsByTag
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.device.DeviceInfo.getTimeZoneForMobEvent
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.json.Converter.toStringJson
import com.altcraft.sdk.network.Request.request
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.userTagIsNullE
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startMobileEventCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

internal object MobileEvent {

    private val mobileEventMutex = Mutex()
    private val sendAllMobileEventMutex = Mutex()

    /**
     * Sends a mobile event to the server.
     *
     * Prepares and enqueues delivery of a mobile event composed of mandatory identifiers
     * and optional metadata.
     *
     * @param context Application context initiating the send.
     * @param sid The string ID of the pixel.
     * @param eventName Event name.
     * @param sendMessageId Optional internal message ID to link the event.
     * @param payloadFields Optional event payload.
     * @param matching Optional matching parameters.
     * @param matchingType Optional matching mode used for the request.
     * @param profileFields Optional profile fields.
     * @param subscription Optional subscription to attach to the profile.
     * @param utmTags Optional UTM tags for attribution.
     * @param altcraftClientID Optional Altcraft client identifier.
     */
    fun sendMobileEvent(
        context: Context,
        sid: String,
        eventName: String,
        sendMessageId: String? = null,
        payloadFields: Map<String, Any?>? = null,
        matching: Map<String, Any?>? = null,
        matchingType: String? = null,
        profileFields: Map<String, Any?>? = null,
        subscription: DataClasses.Subscription? = null,
        utmTags: DataClasses.UTM? = null,
        altcraftClientID: String = "",
    ) {
        val func = "sendMobileEvent"
        val initGate = InitBarrier.current()
        CommandQueue.MobileEventCommandQueue.submit {
            try {
                withInitReady(func, gate = initGate) {
                    awaitMobileEventRetryStarted()
                    mobileEventMutex.withLock {
                        if (!isOnline(context)) error(func, noInternetConnect)
                        val config = getConfig(context) ?: exception(configIsNotSet)
                        val tag = getUserTag(config.rToken) ?: exception(userTagIsNullE)

                        if (fieldsIsObjects(payloadFields)) exception(
                            472 to mobileEventPayloadInvalid(eventName)
                        )

                        val entity = MobileEventEntity(
                            userTag = tag,
                            sid = sid,
                            eventName = eventName,
                            matchingType = matchingType,
                            sendMessageId = sendMessageId,
                            timeZone = getTimeZoneForMobEvent(),
                            altcraftClientID = altcraftClientID,
                            profileFields = profileFields?.toStringJson(func),
                            subscription = subscription?.toStringJson(func),
                            payload = payloadFields?.toStringJson(func),
                            matching = matching?.toStringJson(func),
                            utmTags = utmTags?.toStringJson(func)
                        )

                        entityInsert(context, entity)
                        startMobileEventCoroutineWorker(context)
                    }
                }
            } catch (e: Exception) {
                error("sendMobileEvent", e)
            }
        }
    }

    /**
     * Checks whether mobile event sending should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null): Boolean {
        return try {
            sendAllMobileEventMutex.withLock { logic(context, workerId) }
        } catch (e: Exception) {
            retry("isRetry :: MobileEvent", e); true
        }
    }

    /**
     * Processes pending mobile events in chronological order.
     *
     * - On success, deletes the entity and continues.
     * - On retry-eligible failure with remaining retries:
     *   returns `true` if no newer request exists; otherwise `false`.
     * - On exceeded retry limit, deletes the entity and proceeds.
     *
     * @param context Application context for DB and config access.
     * @param id Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     */
    private suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = MOB_EVENT_C_WORK_TAG
        val env = Environment.create(context)
        val events = allMobileEventsByTag(env.room, env.userTag())

        events.forEach {
            if (request(context, it) !is retry) entityDelete(env.room, it)
            else if (!isRetryLimit(env.room, it)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }
}

