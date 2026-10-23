package com.altcraft.sdk.platform.authenticate.internal

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.workers.onetime.Request.attemptOutcome
import com.altcraft.sdk.workers.onetime.Request.pidChanged
import com.altcraft.sdk.util.SubFunction.awaitInBackground

/**
 * Worker that performs authenticate operations.
 *
 * @param appContext Application context.
 * @param workerParams Parameters used to initialize the worker.
 */
internal class AuthenticateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Executes background work and returns [Result.retry] or [Result.success].
     *
     * Retries are bounded: the budget is exhausted after
     * [com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT] attempts.
     */
    override suspend fun doWork(): Result {
        awaitInBackground()

        return attemptOutcome(runAttemptCount) {
            pidChanged(inputData) || !AuthenticateProcess.isRetry(applicationContext, id)
        }
    }
}
