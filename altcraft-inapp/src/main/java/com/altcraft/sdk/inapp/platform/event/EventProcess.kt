
package com.altcraft.sdk.inapp.platform.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.mapSuspendCatching
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.Constants.IN_APP_DELIVERY
import com.altcraft.sdk.inapp.data.Constants.IN_APP_EVENT_O_WORK_TAG
import com.altcraft.sdk.inapp.data.Constants.IN_APP_EVENT_QUEUE
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppEventData
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.event.request.EventRequest.inAppEventRequest
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.network.json.Converter.toStringJson
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.InAppEventType
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.inapp.util.Log.retry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages In-App event persistence, delivery, and retry processing.
 */
internal object EventProcess {

    /** Serializes background event processing and retry-state updates. */
    val rMutex = Mutex()

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing the current API request. */
    private val request = ::inAppEventRequest

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = "sendInAppEvent"

    /** Processes SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(
        IN_APP_EVENT_QUEUE
    )

    /**
     * Stores an in-app event and schedules background processing.
     *
     * @param context Application context initiating to send.
     * @param type In-app event type.
     * @param smid Send Message ID of the in-app message.
     * @param data Optional event payload.
     */
    fun sendInAppEvent(
        context: Context,
        type: InAppEventType,
        smid: String?,
        data: InAppEventData?
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, null
        ) {
            val env = Environment.create(context)
            if (smid.isNullOrEmpty()) exception(
                ModuleEvents.smidIsNull
            )

            val acid = env.internalAuth().acid
            val data = data?.toStringJson(FUNCTION)

            val entity = InAppEventEntity(
                type = type.value,
                smid = smid,
                acid = acid,
                data = data,
            )

            env.room.dao.event.insert(entity)
            startWorker<EventWorker>(
                context, IN_APP_EVENT_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether in-app event sending should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
     * @throws CancellationException if the coroutine is canceled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        rMutex.withLock {
            logic(context, workerId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        retry("isRetry :: $FUNCTION", e)
        true
    }

    /**
     * Processes stored in-app events concurrently and checks if retry is needed.
     *
     * @param context Application context.
     * @param id Optional worker identifier.
     * @return `true` if events remain and no newer request exists.
     */
    suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = IN_APP_EVENT_O_WORK_TAG
        val room = DB.getInstance(context)
        val inAppDao = room.dao.inApp
        val dao = room.dao.event

        /** Deletes the event on success; otherwise checks its retry limit. */
        suspend fun proc(ent: InAppEventEntity) =
            if (request(context, ent) !is retry) {
                if (ent.type == IN_APP_DELIVERY) {
                    inAppDao.setReportStatus(
                        ent.smid
                    )
                }
                dao.deleteItem(ent)
            } else ent.retryLimit(
                dao
            )

        dao.getAll().mapSuspendCatching(::proc)

        return !dao.isEmpty() && !hasNewRequest(
            context, tag, id
        )
    }

    /**
     * Helper for sending SDK-side In-App error events.
     *
     * @param context Context used to send the event.
     * @param smid Send Message ID associated with the In-App message.
     * @param error Error description associated with In-App processing.
     */
    fun sendErrorInAppEvent(
        context: Context, smid: String?, error: String?
    ) {
        sendInAppEvent(
            context, InAppEventType.ERROR, smid, InAppEventData(
                code = 500, message = error
            )
        )
    }
}
