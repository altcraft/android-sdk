package com.altcraft.sdk.target.platform.target

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.SubFunction.fieldsIsObjects
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.target.data.models.TargetModels.UTM
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.Subscription
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.json.Converter.toStringJson
import com.altcraft.sdk.target.data.Constants.FUNCTION
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.data.Constants.TARGET_QUEUE
import com.altcraft.sdk.target.data.Constants.TARGET_O_WORK_TAG
import com.altcraft.sdk.target.events.ModuleEvents
import com.altcraft.sdk.target.infra.Environment
import com.altcraft.sdk.target.platform.target.request.TargetRequest
import com.altcraft.sdk.target.util.Utils.getTimeZone
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.target.util.Log.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Volatile

/**
 * Coordinates target request creation, storage, sending, and retry processing.
 */
internal object TargetProcess {
    /** Signals all target entities requested; ensures startup retries run first. */
    @Volatile
    var snapshotTaken = CompletableDeferred<Unit>()

    /** Processes target SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(TARGET_QUEUE)

    /** Serializes target retry-state checks and updates. */
    private val rMutex = Mutex()

    /** Prevents concurrent execution of this target command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing a target request. */
    private val request = TargetRequest::targetRequest

    /**
     * Sends a target request to the server.
     *
     * Prepares and enqueues delivery of a target request composed of mandatory identifiers
     * and optional metadata.
     *
     * @param context Application context initiating to send.
     * @param sid The string ID of the pixel.
     * @param eventName Target event name.
     * @param sendMessageId Optional internal message ID to link the target request.
     * @param payloadFields Optional target payload.
     * @param matching Optional matching parameters.
     * @param matchingType Optional matching mode used for the request.
     * @param profileFields Optional profile fields.
     * @param subscription Optional subscription to attach to the profile.
     * @param utmTags Optional UTM tags for attribution.
     * @param altcraftClientID Optional Altcraft client identifier.
     */
    fun target(
        context: Context,
        sid: String,
        eventName: String,
        sendMessageId: String? = null,
        payloadFields: Map<String, Any?>? = null,
        matching: Map<String, Any?>? = null,
        matchingType: String? = null,
        profileFields: Map<String, Any?>? = null,
        subscription: Subscription? = null,
        utmTags: UTM? = null,
        altcraftClientID: String = "",
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, snapshotTaken
        ) {
            val env = Environment.create(context)
            if (fieldsIsObjects(payloadFields)) exception(
                ModuleEvents.invalidPayload
            )

            val entity = Entity(
                sid = sid,
                eventName = eventName,
                userTag = env.user(),
                timeZone = getTimeZone(),
                matchingType = matchingType,
                sendMessageId = sendMessageId,
                altcraftClientID = altcraftClientID,
                profileFields = profileFields?.toStringJson(FUNCTION),
                subscription = subscription?.toStringJson(FUNCTION),
                payload = payloadFields?.toStringJson(FUNCTION),
                matching = matching?.toStringJson(FUNCTION),
                utmTags = utmTags?.toStringJson(FUNCTION)
            )

            env.room.dao().insert(entity)
            startWorker<TargetWorker>(context, TARGET_O_WORK_TAG)
        }
    }

    /**
     * Checks whether target request sending should be retried.
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
     * Processes pending target requests in chronological order.
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
        val tag = TARGET_O_WORK_TAG
        val env = Environment.create(context)
        val dao = env.room.dao()
        val events = dao.getAllEntitiesByTag(
            env.user()
        )

        events.forEach {
            if (request(context, it) !is retry) dao.delete(it)
            else if (!it.retryLimit(dao)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }
}
