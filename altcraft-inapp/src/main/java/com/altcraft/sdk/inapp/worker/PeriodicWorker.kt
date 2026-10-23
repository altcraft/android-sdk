package com.altcraft.sdk.inapp.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.data.Constants.ONE_TIME_WORKER_TAGS
import com.altcraft.sdk.inapp.platform.event.EventProcess
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess.requestPlacements
import com.altcraft.sdk.inapp.platform.static.StaticProcess
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic worker for retrying pending in-app requests.
 *
 * @param appContext Application context.
 * @param workerParams Worker parameters.
 */
internal class PeriodicWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val context = appContext

    /**
     * Retries pending in-app requests in background.
     */
    override suspend fun doWork(): Result {
        return try {
            if (!isAppInForegrounded()) {
                cancelWorkerTask(context, ONE_TIME_WORKER_TAGS)
                requestPlacements(context, workerReq = true)
                SubscriptionProcess.isRetry(context)
                StaticProcess.loadStatic(context)
                EventProcess.isRetry(context)
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