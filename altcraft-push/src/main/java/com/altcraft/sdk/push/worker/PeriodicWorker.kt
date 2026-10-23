package com.altcraft.sdk.push.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.data.Constants.ONE_TIME_WORKER_TAGS
import com.altcraft.sdk.push.platform.event.EventProcess
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.push.platform.token.TokenUpdateProcess
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic worker for retrying push requests.
 */
internal class PeriodicWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val context = appContext

    /** Retries push requests in background. */
    override suspend fun doWork(): Result {
        return try {
            if (!isAppInForegrounded()) {
                cancelWorkerTask(
                    context,
                    ONE_TIME_WORKER_TAGS
                )
                TokenUpdateProcess.isRetry(context)
                SubscriptionProcess.isRetry(context)
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
