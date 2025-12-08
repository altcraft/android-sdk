package com.altcraft.sdk.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
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
import com.altcraft.sdk.network.Request.request
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startPushEventCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
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
    suspend fun sendPushEvent(context: Context, type: String, messageUID: String?) {
        val func = "sendPushEvent"
        try {
            pushEventMutex.withLock {
                if (messageUID.isNullOrEmpty()) exception(uidIsNull)
                if (!isOnline(context)) error(func, noInternetConnect)
                val pushEventEntity = PushEventEntity(messageUID, type)

                if (request(context, pushEventEntity) is retry) {
                    entityInsert(context, pushEventEntity)
                    startPushEventCoroutineWorker(context)
                }
            }
        } catch (e: Exception) {
            error("sendPushEvent", e)
        }
    }

    /**
     * Checks whether pending push events require another retry pass.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if another retry should be scheduled; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null): Boolean {
        return try {
            sendAllPushEventMutex.withLock { logic(context, workerId) }
        } catch (e: Exception) {
            retry("isRetry :: pushEvent", e); true
        }
    }

    /**
     * Sends pending push events and decides if further retries are needed.
     *
     * @param context Application context.
     * @param id Optional worker identifier.
     * @return `true` if events remain and no newer request exists; `false` otherwise.
     */
    private suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = PUSH_EVENT_C_WORK_TAG
        val env = Environment.create(context)

        sendAllPushEvents(context, env.room, env.room.request().getAllPushEvents())
        return env.room.request().getAllPushEvents().isNotEmpty() && !hasNewRequest(
            context, tag, id
        )
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