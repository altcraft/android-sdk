package com.altcraft.sdk.workers.onetime

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.os.Process
import androidx.annotation.RestrictTo
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.data.Constants.PID
import com.altcraft.sdk.data.Constants.RETRY_TIME_C_WORK
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import kotlinx.coroutines.guava.await
import kotlin.coroutines.cancellation.CancellationException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Utility object for creating and enqueuing preconfigured [OneTimeWorkRequest] instances.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Request {

    /**
     * Default constraints requiring network connectivity.
     */
    @PublishedApi
    internal val constraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Builds a [OneTimeWorkRequest] for the given worker type with backoff and constraints.
     *
     * @param tag Work tag identifier.
     * @return Configured [OneTimeWorkRequest].
     */
    @PublishedApi
    internal inline fun <reified W : ListenableWorker> createWorkRequest(
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
     * Creates and enqueues a worker of type [W].
     *
     * Usage:
     * `startWorker<MyWorker>(context, MY_WORK_TAG)`
     *
     * @param context Application context.
     * @param tag Work tag identifier.
     */
    inline fun <reified W : ListenableWorker> startWorker(
        context: Context,
        tag: String
    ) {
        try {
            WorkManager.getInstance(context).enqueue(
                createWorkRequest<W>(tag)
            )
        } catch (e: Exception) {
            error(tag, e)
        }
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
     * Returns true if there is a newer WorkRequest with [tag].
     *
     * @param context Application context.
     * @param tag Tag used to query related WorkRequests.
     * @param id ID of the current WorkRequest.
     * @return true if a newer WorkRequest exists; false otherwise.
     */
    suspend fun hasNewRequest(
        context: Context,
        tag: String,
        id: UUID? = null
    ): Boolean {
        return try {
            val active = WorkManager.getInstance(context)
                .getWorkInfosByTag(tag)
                .await()
                .filter {
                    it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.RUNNING
                }

            if (active.size <= 1 || id == null) {
                false
            } else {
                hasNewerRequest(active, id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("requestReplaced", e)
            false
        }
    }

    /**
     * Checks whether there is an active worker with the given tag.
     *
     * @param context Application context.
     * @param tag Work tag used to find related workers.
     * @return true if a worker is enqueued or running; false otherwise.
     */
    suspend fun hasRunningWorker(context: Context, tag: String): Boolean {
        return WorkManager.getInstance(context).getWorkInfosByTag(tag).await().any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    }

    /**
     * Checks whether the app process has changed since the WorkRequest was created.
     *
     * A different PID indicates that the app was restarted.
     * When the app is in the foreground, the SDK handles all retry logic.
     * In this case, the work does not need to be rerun by WorkManager.
     *
     * @param inputData Data containing the process ID captured when the work was enqueued.
     */
    fun pidChanged(
        inputData: Data
    ) = isAppInForegrounded() && inputData.getInt(PID, -1) != Process.myPid()

    /**
     * Bounds one-time worker retries: a persistent failure that prevents retry
     * counters from advancing (e.g. database or authentication errors caught
     * by the isRetry catch-all) must not reschedule the worker forever.
     *
     * The attempt budget is checked before [shouldStop] is evaluated, so an
     * exhausted worker stops without running another processing pass.
     *
     * @param runAttemptCount Number of attempts already made for the current work request.
     * @param shouldStop Deferred check; `true` when the worker must not retry.
     * @return [ListenableWorker.Result.success] when the retry budget is exhausted
     * or [shouldStop] holds; [ListenableWorker.Result.retry] otherwise.
     */
    inline fun attemptOutcome(
        runAttemptCount: Int,
        shouldStop: () -> Boolean
    ): ListenableWorker.Result =
        if (runAttemptCount > MAX_RETRY_COUNT || shouldStop()) ListenableWorker.Result.success()
        else ListenableWorker.Result.retry()
}
