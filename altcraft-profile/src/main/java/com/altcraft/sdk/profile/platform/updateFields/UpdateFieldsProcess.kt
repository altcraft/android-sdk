package com.altcraft.sdk.profile.platform.updateFields

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.util.MapExtension.mapToJson
import com.altcraft.sdk.profile.data.Constants.FUNCTION
import com.altcraft.sdk.profile.data.Constants.PROFILE_O_WORK_TAG
import com.altcraft.sdk.profile.data.Constants.PROFILE_QUEUE
import com.altcraft.sdk.profile.data.room.Entity
import com.altcraft.sdk.profile.platform.updateFields.request.UpdateFieldsRequest
import com.altcraft.sdk.profile.infra.Environment
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.profile.util.Log.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Volatile

/**
 * Coordinates profile update creation, storage, sending, and retry processing.
 */
internal object UpdateFieldsProcess {

    /** Signals all profile entities requested; ensures startup retries run first. */
    @Volatile
    var snapshotTaken = CompletableDeferred<Unit>()

    /** Processes profile SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(PROFILE_QUEUE)

    /** Serializes profile retry-state checks and updates. */
    private val rMutex = Mutex()

    /** Prevents concurrent execution of this profile command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing a profile update request. */
    private val request = UpdateFieldsRequest::updateFieldsRequest

    /**
     * Registers a profile fields update for delivery.
     *
     * Stores the update request in the Profile module database and starts
     * the profile update coroutine worker.
     *
     * @param context Application context initiating the update.
     * @param profileFields Optional profile fields to update.
     * @param skipTriggers Optional flag to skip triggers on the server side.
     */
    fun updateFields(
        context: Context,
        profileFields: Map<String, Any?>? = null,
        skipTriggers: Boolean? = null
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, snapshotTaken
        ) {
            val env = Environment.create(context)

            val entity = Entity(
                profileFields = profileFields?.mapToJson(),
                time = System.currentTimeMillis(),
                skipTriggers = skipTriggers,
                userTag = env.userTag()
            )

            env.room.dao().insert(entity)

            startWorker<UpdateFieldsWorker>(
                context, PROFILE_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether profile update sending should be retried.
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
     * Processes pending profile updates in chronological order.
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
        val tag = PROFILE_O_WORK_TAG
        val env = Environment.create(context)
        val dao = env.room.dao()
        val updates = dao.getAllEntitiesByTag(env.userTag())

        updates.forEach {
            if (request(context, it) !is retry) dao.delete(it)
            else if (!it.retryLimit(dao)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }
}
