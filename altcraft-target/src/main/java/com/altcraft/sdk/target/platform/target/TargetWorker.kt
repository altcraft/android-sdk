package com.altcraft.sdk.target.platform.target

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.target.util.Utils.awaitInBackground
import com.altcraft.sdk.workers.onetime.Request.attemptOutcome
import com.altcraft.sdk.workers.onetime.Request.pidChanged

/**
 * WorkManager worker responsible for processing pending target requests.
 *
 * Delegates retry handling to [TargetProcess].
 *
 * @param appContext Application context used by WorkManager.
 * @param workerParams Parameters associated with the current work request.
 */
internal class TargetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Processes pending target requests.
     *
     * @return [Result.success] if processing is complete or the bounded retry
     * budget is exhausted;
     * [Result.retry] if another attempt is required.
     */
    override suspend fun doWork(): Result {
        awaitInBackground()

        return attemptOutcome(runAttemptCount) {
            pidChanged(inputData) || !TargetProcess.isRetry(applicationContext, id)
        }
    }
}
