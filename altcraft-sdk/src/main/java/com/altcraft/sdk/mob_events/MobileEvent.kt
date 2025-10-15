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
     * This function prepares and triggers the delivery of a mobile event composed of
     * mandatory identifiers and optional metadata.
     *
     * @param context Application or module context initiating the send operation.
     * @param sid The string ID of the pixel.
     * @param eventName event name.
     * @param payloadFields arbitrary event data as a map; will be serialized to JSON.
     * @param matching matching parameters; will be serialized to JSON.
     * @param sendMessageId Send Message ID.
     * @param profileFields Optional profile fields to include in the request.
     * @param subscription The subscription that will be added to the profile.
     * @param utmTags Optional UTM tags for campaign attribution.
     * @param altcraftClientID Altcraft client identifier.
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
     * Determines whether mobile event requests for the current user need to be retried
     *
     * This function retrieves all mobile events for the given user (identified by `userTag`),
     * ordered from oldest to newest, and attempts to process them. If a mobile event request
     * fails with a `retry` error, it checks whether the retry limit has been reached.
     * If the limit is not exceeded, the event will be retried; otherwise, the event is deleted.
     *
     * Events that are successfully processed are immediately deleted from the database.
     * The function ensures thread-safe processing of the user's mobile events using a mutex lock.
     *
     * @param context The application context used for database operations
     * @return `true` if at least one mobile event request for this user needs to be retried,
     *         otherwise `false`.
     */
    suspend fun isRetry(context: Context): Boolean {
        return try {
            sendAllMobileEventMutex.withLock {
                val config = getConfig(context) ?: exception(configIsNull)
                val tag = getUserTag(config.rToken) ?: exception(userTagIsNull)

                val room = SDKdb.getDb(context)

                room.request().allMobileEventsByTag(tag).forEach {

                    if (mobileEventRequest(context, it) is retry) {
                        return !isRetryLimit(room, it)
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