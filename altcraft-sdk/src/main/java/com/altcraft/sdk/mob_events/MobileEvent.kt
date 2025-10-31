package com.altcraft.sdk.mob_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.StringBuilder.mobileEventPayloadInvalid
import com.altcraft.sdk.additional.SubFunction.fieldsIsObjects
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.auth.AuthManager.getUserTag
import com.altcraft.sdk.concurrency.CommandQueue.MobileEventCommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.concurrency.withInitReady
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.device.DeviceInfo.getTimeZoneForMobEvent
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.json.Converter.toStringJson
import com.altcraft.sdk.network.Request.mobileEventRequest
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.EventList.configIsNull
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.userTagIsNull
import com.altcraft.sdk.sdk_events.EventList.userTagIsNullE
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startMobileEventCoroutineWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        MobileEventCommandQueue.submit {
            try {
                withInitReady(func, gate = initGate) {
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
     * Determines whether mobile event processing should be retried for the current user tag.
     *
     * Iterates through mobile events ordered by time (oldest first):
     * - If the request succeeds, the entity is deleted and processing continues.
     * - If it fails with a retry error and the limit is not reached, returns `true`.
     * - If the limit is reached, logs and deletes the entity. If no more entries remain,
     *   returns `false`; otherwise continues with the next record.
     *
     * Ensures sequential execution via a mutex to prevent concurrent access.
     *
     * @param context Application context for config, user tag, and DB access.
     * @return `true` if another retry is required; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context): Boolean {
        return try {
            sendAllMobileEventMutex.withLock {
                val config = getConfig(context) ?: exception(configIsNull)
                val tag = getUserTag(config.rToken) ?: exception(userTagIsNull)

                val room = SDKdb.getDb(context)

                room.request().allMobileEventsByTag(tag).forEach {
                    if (mobileEventRequest(context, it) is retry) {
                        if (!isRetryLimit(room, it)) return true
                    } else {
                        entityDelete(room, it)
                    }
                }
                false
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            retry("isRetry :: mobileEvent", e)
            true
        }
    }
}