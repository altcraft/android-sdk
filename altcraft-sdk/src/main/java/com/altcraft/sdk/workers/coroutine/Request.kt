package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.MOB_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.RETRY_TIME_C_WORK
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.workers.coroutine.Worker.MobileEventCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Worker.PushEventCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Worker.SubscribeCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Worker.UpdateCoroutineWorker
import java.util.concurrent.TimeUnit
import com.altcraft.sdk.sdk_events.Events.error
import kotlinx.coroutines.guava.await
import java.util.UUID
import android.os.Process

/**
 * Utility object for creating preconfigured [OneTimeWorkRequest] instances for WorkManager.
 *
 * Contains shared [constraints] and a factory method for instantiating typed workers.
 */
internal object Request {

    /**
     * Default constraints requiring network connectivity.
     */
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Builds a [OneTimeWorkRequest] for the given [ListenableWorker] with backoff and constraints.
     *
     * @param tag Work tag identifier.
     * @return Configured [OneTimeWorkRequest].
     */

    private inline fun <reified W : ListenableWorker> createWorkRequest(
        tag: String
    ): OneTimeWorkRequest {
        val time = System.currentTimeMillis().toString()
        val pid = Process.myPid()

        val inputData = Data.Builder()
            .putInt("pid", pid)
            .build()

        return OneTimeWorkRequestBuilder<W>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_TIME_C_WORK,
                TimeUnit.SECONDS
            )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(tag)
            .addTag(time)
            .build()
    }

    /**
     * Checks whether a newer WorkRequest exists for the given list,
     * based on numeric time tags stored in WorkInfo tags.
     *
     * @param active Active WorkInfo list.
     * @param id ID of the current WorkRequest.
     * @return true if another WorkRequest has a greater time tag; false otherwise.
     */
    private fun hasNewerRequest(active: List<WorkInfo>, id: UUID): Boolean {
        val target = active
            .asSequence()
            .filter { it.id == id }
            .flatMap { it.tags.asSequence() }
            .mapNotNull { it.toLongOrNull() }
            .maxOrNull()

        val other = active
            .asSequence()
            .filter { it.id != id }
            .flatMap { it.tags.asSequence() }
            .mapNotNull { it.toLongOrNull() }
            .maxOrNull()

        return target != null && other != null && target < other
    }

    /**
     * Returns true if there is a WorkRequest with [tag]
     * whose runAttemptCount is less than [count].
     *
     * @param context application context.
     * @param tag tag used to query related WorkRequests.
     * @param count runAttemptCount of the current WorkRequest.
     * @return true if a "newer" WorkRequest exists, false otherwise.
     */
    suspend fun hasNewRequest(context: Context, tag: String, id: UUID? = null): Boolean {
        return try {
            val active = WorkManager.getInstance(context).getWorkInfosByTag(tag).await()
                .filter {
                    it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                }
            if (active.size <= 1 || id == null) return false else hasNewerRequest(active, id)
        } catch (e: Exception) {
            error("requestReplaced", e)
            false
        }
    }

    /**
     * Creates push event worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun pushEventRequest() = createWorkRequest<PushEventCoroutineWorker>(PUSH_EVENT_C_WORK_TAG)

    /**
     * Creates mobile event worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun mobileEventRequest() = createWorkRequest<MobileEventCoroutineWorker>(MOB_EVENT_C_WORK_TAG)

    /**
     *  Creates subscribe worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun subscribeRequest() = createWorkRequest<SubscribeCoroutineWorker>(SUBSCRIBE_C_WORK_TAG)

    /**
     * Creates update worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun updateRequest() = createWorkRequest<UpdateCoroutineWorker>(UPDATE_C_WORK_TAG)
}