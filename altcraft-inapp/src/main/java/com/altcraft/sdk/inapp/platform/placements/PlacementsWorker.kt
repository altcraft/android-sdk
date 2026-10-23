package com.altcraft.sdk.inapp.platform.placements

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.inapp.util.Utils.awaitInBackground
import com.altcraft.sdk.workers.onetime.Request.attemptOutcome
import com.altcraft.sdk.workers.onetime.Request.pidChanged

/**
 * WorkManager worker responsible for requesting in-app placements.
 *
 * Delegates retry handling to [PlacementsProcess].
 *
 * @param appContext Application context used by WorkManager.
 * @param workerParams Parameters associated with the current work request.
 */
internal class PlacementsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Requests in-app placements.
     *
     * @return [Result.success] if processing is complete or the bounded retry
     * budget is exhausted;
     * [Result.retry] if another attempt is required.
     */
    override suspend fun doWork(): Result {
        awaitInBackground()

        return attemptOutcome(runAttemptCount) {
            pidChanged(inputData) || !PlacementsProcess.isRetry(applicationContext, id)
        }
    }
}
