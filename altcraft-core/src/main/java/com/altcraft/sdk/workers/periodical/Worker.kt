package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.data.Constants.AUTH_REQUEST_O_WORK_TAG
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic WorkManager workers for retrying SDK background operations.
 */
internal object Worker {

    /**
     * Periodic worker for retrying authentication requests.
     *
     * @constructor Creates a new instance of `RetryAuthRequestWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class PeriodicWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext
        private val tags = listOf(
            AUTH_REQUEST_O_WORK_TAG
        )

        /**
         * Retries authentication requests in background.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    cancelWorkerTask(context, tags)
                    AuthenticateProcess.isRetry(context)
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
}
