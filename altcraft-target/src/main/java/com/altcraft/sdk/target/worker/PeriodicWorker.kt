package com.altcraft.sdk.target.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.target.platform.target.TargetProcess.isRetry
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import kotlin.coroutines.cancellation.CancellationException
import com.altcraft.sdk.target.util.Log.error
import com.altcraft.sdk.target.data.Constants.TARGET_O_WORK_TAG

/**
 * Periodic worker for retrying target requests.
 *
 * @param appContext Application context.
 * @param workerParams Worker parameters.
 */
internal class PeriodicWorker(
    appContext: Context,
    workerParams: WorkerParameters
) :
    CoroutineWorker(appContext, workerParams) {
    private val context = appContext
    private val tags = listOf(
        TARGET_O_WORK_TAG
    )

    /**
     * Retries mobile events in background.
     */
    override suspend fun doWork(): Result {
        return try {
            if (!isAppInForegrounded()) {
                cancelWorkerTask(context, tags)
                isRetry(context)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("PeriodicWorker :: doWork", e)
            Result.failure()
        }
    }
}