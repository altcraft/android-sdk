package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.CHECK_P_WORK_NANE
import com.altcraft.sdk.data.Constants.EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.RETRY_TIME_P_WORK
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalPushEventWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalSubscribeWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalUpdateWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalTokenCheckWorker
import kotlinx.coroutines.guava.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Contains helpers for scheduling periodic background tasks with WorkManager.
 */
object CommonFunctions {

    /**
     * Network constraint: work only runs with active connection.
     */
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Starts the first missing or cancelled periodic worker.
     *
     * Checks the status of all periodic workers and starts the first one
     * that is either not running or was cancelled.
     *
     * @param context Application context.
     */
    internal suspend fun periodicalWorkerControl(context: Context) {
        try {
            getAllWorkerState(context)?.let {
                if (isNotStart(it.updateWorkState)) startPeriodicalUpdateWorker(context)
                if (isNotStart(it.pushEventWorkState)) startPeriodicalPushEventWorker(context)
                if (isNotStart(it.tokenCheckWorkState)) startPeriodicalTokenCheckWorker(context)
                if (isNotStart(it.subscribeWorkState)) startPeriodicalSubscribeWorker(context)
            }
        } catch (e: Exception) {
            error("periodicalWorkerControl", e)
        }
    }

    /**
     * Determines whether a periodic worker should be (re)started.
     *
     * Returns true if the given state is null or indicates that the worker was cancelled.
     *
     * @param state The current state of the worker.
     * @return True if the worker needs to be started or restarted.
     */
    private fun isNotStart(state: WorkInfo.State?): Boolean {
        return state == null || state == WorkInfo.State.CANCELLED
    }

    /**
     * Returns a list of WorkInfo for the given work name.
     *
     * @param context Application context.
     * @param workName Unique work name.
     * @return List of WorkInfo or null if retrieval fails.
     */
    private suspend fun getWorkState(context: Context, workName: String): List<WorkInfo>? {
        return try {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).await()
        } catch (e: Exception) {
            error("getWorkState", e)
            null
        }
    }

    /**
     * Returns the current state of all periodic workers.
     *
     * @param context Application context.
     * @return WorkersState containing individual work states or null on failure.
     */
    private suspend fun getAllWorkerState(context: Context): DataClasses.WorkersState? {
        return try {
            DataClasses.WorkersState(
                getWorkState(context, UPDATE_P_WORK_NANE)?.firstOrNull()?.state,
                getWorkState(context, EVENT_P_WORK_NANE)?.firstOrNull()?.state,
                getWorkState(context, CHECK_P_WORK_NANE)?.firstOrNull()?.state,
                getWorkState(context, SUB_P_WORK_NANE)?.firstOrNull()?.state
            )
        } catch (e: Exception) {
            error("getAllWorkerState", e)
            null
        }
    }

    /**
     * Creates a periodic work request with retry interval and constraints.
     *
     * @param workerClass CoroutineWorker class to run.
     * @return Configured PeriodicWorkRequest.
     */
    internal fun createRequest(workerClass: Class<out CoroutineWorker>): PeriodicWorkRequest {
        return PeriodicWorkRequest.Builder(workerClass, RETRY_TIME_P_WORK, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
    }

    /**
     * Enqueues a unique periodic work request using [WorkManager].
     * Replaces existing work with the same name if present.
     *
     * Exceptions during scheduling are caught and logged.
     *
     * @param context Application context.
     * @param workName Unique name identifying the periodic work.
     * @param request Configured [PeriodicWorkRequest] to schedule.
     */
    internal fun createWorker(context: Context, workName: String, request: PeriodicWorkRequest) {
        try {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        } catch (e: Exception) {
            error("createWorker", e)
        }
    }

    /**
     * Suspends the coroutine until the given cancellation task completes.
     *
     * @param context The context required by the task.
     * @param task A function that accepts a [Context] and a completion callback.
     */
    internal suspend fun awaitCancel(context: Context, task: (Context, () -> Unit) -> Unit) =
        suspendCoroutine { continuation -> task(context) { continuation.resume(Unit) } }

    /**
     * Cancels all scheduled periodic workers.
     *
     * @param context Application context.
     */
    internal fun cancelPeriodicalWorkersTask(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SUB_P_WORK_NANE)
        WorkManager.getInstance(context).cancelUniqueWork(EVENT_P_WORK_NANE)
        WorkManager.getInstance(context).cancelUniqueWork(UPDATE_P_WORK_NANE)
        WorkManager.getInstance(context).cancelUniqueWork(CHECK_P_WORK_NANE)
    }
}
