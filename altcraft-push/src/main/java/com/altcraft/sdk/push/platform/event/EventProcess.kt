
package com.altcraft.sdk.push.platform.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants.FUNCTION_EVENT
import com.altcraft.sdk.push.data.Constants.PUSH_EVENT_O_WORK_TAG
import com.altcraft.sdk.push.data.room.DB
import com.altcraft.sdk.push.data.room.entity.PushEventEntity
import com.altcraft.sdk.push.platform.event.EventAPI.EventType
import com.altcraft.sdk.push.platform.event.request.EventRequest
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.push.events.ModuleEvents
import kotlin.coroutines.cancellation.CancellationException
import com.altcraft.sdk.coordination.mapSuspendCatching

/**
 * Handles push event persistence, delivery, and retry processing.
 *
 * Push events are stored locally and processed by a background worker.
 * Failed requests are retried according to the configured retry policy.
 */
internal object EventProcess {

    /** Serializes background event processing and retry-state updates. */
    private val mutex = Mutex()

    /** Reference to the function executing the push event request. */
    private val request = EventRequest::pushEventRequest

    /**
     * Stores and sends a push event, scheduling a retry if needed.
     *
     * @param context Application context.
     * @param type Push event type.
     * @param uid Unique push notification identifier.
     */
    suspend fun sendPushEvent(context: Context, type: EventType, uid: String?) {
        try {
            if (uid.isNullOrEmpty()) exception(ModuleEvents.pushUidIsNull)
            val entity = PushEventEntity(uid = uid, type = type.value)
            Environment.create(context).room.dao.event.insert(entity)
            startWorker<EventWorker>(context, PUSH_EVENT_O_WORK_TAG)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("sendPushEvent", e)
        }
    }

    /**
     * Checks whether stored push events should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     * @throws CancellationException if the coroutine is canceled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        mutex.withLock {
            logic(context, workerId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        retry("isRetry :: $FUNCTION_EVENT", e)
        true
    }

    /**
     * Processes stored push events and checks whether a retry is needed.
     *
     * @param context Application context.
     * @param id Optional worker identifier.
     * @return `true` if events remain and no newer request exists.
     */
    suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = PUSH_EVENT_O_WORK_TAG
        val room = DB.getInstance(context)
        val dao = room.dao.event

        /** Deletes completed events or updates their retry count. */
        suspend fun proc(entity: PushEventEntity) =
            if (request(context, entity) !is retry)
                dao.deleteItem(entity)
            else entity.retryLimit(dao)

        dao.getAll().mapSuspendCatching(::proc)

        return !dao.isEmpty() && !hasNewRequest(context, tag, id)
    }
}
