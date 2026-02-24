package com.altcraft.sdk.profile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.auth.AuthManager.getUserTag
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.coordination.withInitReady
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.core.InitialOperations.awaitProfileUpdateRetryStarted
import com.altcraft.sdk.data.Constants.PR_UPDATE_C_WORK_TAG
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.ProfileUpdateEntity
import com.altcraft.sdk.data.room.RoomRequest.allProfileUpdatesByTag
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.MapExtension.mapToJson
import com.altcraft.sdk.network.Request.request
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.userTagIsNullE
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startProfileUpdateCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Profile update manager.
 *
 * Registers profile update requests, stores them in the local DB,
 * and coordinates sending and retry flow via workers/coroutines
 * until delivery succeeds or retries are exhausted.
 */
internal object ProfileUpdate {

    private val profileUpdateMutex = Mutex()
    private val sendAllProfileUpdateMutex = Mutex()

    /**
     * Enqueues a profile update for delivery.
     *
     * Stores the update request in DB and starts the profile-update coroutine worker.
     *
     * @param context Application context initiating the update.
     * @param profileFields Optional profile fields to update.
     * @param skipTriggers Optional flag to skip triggers on the server side.
     */
    fun updateProfileFields(
        context: Context,
        profileFields: Map<String, Any?>? = null,
        skipTriggers: Boolean? = null
    ) {
        val func = "updateProfile"
        val initGate = InitBarrier.current()

        CommandQueue.ProfileUpdateCommandQueue.submit {
            try {
                withInitReady(func, gate = initGate) {
                    awaitProfileUpdateRetryStarted()
                    profileUpdateMutex.withLock {
                        if (!isOnline(context)) error(func, noInternetConnect)
                        val config = getConfig(context) ?: exception(configIsNotSet)
                        val tag = getUserTag(config.rToken) ?: exception(userTagIsNullE)

                        val entity = ProfileUpdateEntity(
                            profileFields = profileFields?.mapToJson(),
                            time = System.currentTimeMillis(),
                            skipTriggers = skipTriggers,
                            userTag = tag
                        )

                        entityInsert(context, entity)
                        startProfileUpdateCoroutineWorker(
                            context
                        )
                    }
                }
            } catch (e: Exception) {
                error(func, e)
            }
        }
    }

    /**
     * Checks whether profile update sending should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null): Boolean {
        return try {
            sendAllProfileUpdateMutex.withLock { logic(context, workerId) }
        } catch (e: Exception) {
            retry("isRetry :: ProfileUpdate", e)
            true
        }
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
        val tag = PR_UPDATE_C_WORK_TAG
        val env = Environment.create(context)
        val updates = allProfileUpdatesByTag(env.room, env.userTag())

        updates.forEach {
            if (request(context, it) !is retry) entityDelete(env.room, it)
            else if (!isRetryLimit(env.room, it)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }
}