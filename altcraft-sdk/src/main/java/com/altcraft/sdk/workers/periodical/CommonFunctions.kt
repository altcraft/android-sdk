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
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import com.altcraft.sdk.core.Retry.pushModuleIsActive
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.RETRY_TIME_P_WORK
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalMobileEventWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalPushEventWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalSubscribeWorker
import com.altcraft.sdk.workers.periodical.LaunchFunctions.startPeriodicalUpdateWorker
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
     * Ensures that required periodic workers are scheduled.
     *
     * Starts the mobile events worker, and, if push is enabled, also
     * push event, token update, and subscribe workers when they are not running.
     *
     * @param context Application context.
     */
    internal suspend fun periodicalWorkerControl(context: Context) {
        try {
            if (isNotStart(getWorkData(context, MOBILE_EVENT_P_WORK_NANE)?.firstOrNull()?.state))
                startPeriodicalMobileEventWorker(context)

            if (!pushModuleIsActive(context)) return

            if (isNotStart(getWorkData(context, PUSH_EVENT_P_WORK_NANE)?.firstOrNull()?.state))
                startPeriodicalPushEventWorker(context)
            if (isNotStart(getWorkData(context, UPDATE_P_WORK_NANE)?.firstOrNull()?.state))
                startPeriodicalUpdateWorker(context)
            if (isNotStart(getWorkData(context, SUB_P_WORK_NANE)?.firstOrNull()?.state))
                startPeriodicalSubscribeWorker(context)
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
    private fun isNotStart(state: State?) = state == null || state == State.CANCELLED

    /**
     * Returns a list of WorkInfo for the given work name.
     *
     * @param context Application context.
     * @param workName Unique work name.
     * @return List of WorkInfo or null if retrieval fails.
     */
    private suspend fun getWorkData(context: Context, workName: String): List<WorkInfo>? {
        return try {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).await()
        } catch (e: Exception) {
            error("getWorkState", e)
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
        WorkManager.getInstance(context).cancelUniqueWork(UPDATE_P_WORK_NANE)
        WorkManager.getInstance(context).cancelUniqueWork(PUSH_EVENT_P_WORK_NANE)
        WorkManager.getInstance(context).cancelUniqueWork(MOBILE_EVENT_P_WORK_NANE)
    }
}
