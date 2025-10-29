package com.altcraft.sdk.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.network.Request.pushEventRequest
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.uidIsNull
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startPushEventCoroutineWorker
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles push event processing and ensures reliable delivery to the server.
 *
 * The system ensures that push events are not lost and are retried if necessary.
 */
internal object PushEvent {

    private val pushEventMutex = Mutex()
    private val sendAllPushEventMutex = Mutex()

    /**
     * Attempts to send the given push event to the server.
     * If the request cannot be completed (e.g., due to network or server issues),
     * the event is stored locally and WorkManager is triggered to retry later.
     *
     * @param context Application context.
     * @param type Event type ("delivery" or "open").
     * @param messageUID Unique event identifier. Must not be null or empty.
     */
    suspend fun sendPushEvent(
        context: Context,
        type: String,
        messageUID: String?,
    ) {
        val func = "sendPushEvent"
        try {
            pushEventMutex.withLock {
                if (messageUID.isNullOrEmpty()) exception(uidIsNull)
                if (!isOnline(context)) error(func, noInternetConnect)

                val event = PushEventEntity(messageUID, type)

                if (pushEventRequest(context, event) is retry) {
                    entityInsert(context, event)
                    startPushEventCoroutineWorker(context)
                }
            }
        } catch (e: Exception) {
            error(func, e)
        }
    }

    /**
     * Sends all push events and checks whether any remain in the database after sending.
     *
     * If any events are still stored after the attempt (e.g., due to errors or retry limits),
     * the function returns `true`. The return value is used by WorkManager to decide
     * whether a retry operation should be scheduled.
     *
     * @param context The application context used to access the local database.
     * @return `true` if there are remaining push events in the database; `false` otherwise.
     */
    suspend fun isRetry(context: Context): Boolean {
        return try {
            sendAllPushEventMutex.withLock {
                val room = SDKdb.getDb(context)
                val events = room.request().getAllPushEvents()

                sendAllPushEvents(context, room, events)

                room.request().getAllPushEvents().isNotEmpty()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            retry("isRetry :: push event", e)
            true
        }
    }

    /**
     * Sends all given push events concurrently and deletes those that are successfully processed.
     *
     * @param context The application context.
     * @param room The local SDK database instance.
     * @param events A list of push events to process.
     */
    suspend fun sendAllPushEvents(context: Context, room: SDKdb, events: List<PushEventEntity>) {
        coroutineScope {
            events.map { event ->
                async<Unit> {
                    try {
                        if (pushEventRequest(context, event) !is retry) entityDelete(room, event)
                        else isRetryLimit(room, event)
                    } catch (e: Exception) {
                        retry("sendAllPushEvents", e)
                    }
                }
            }
        }
    }
}