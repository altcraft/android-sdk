package com.altcraft.sdk.inapp.platform.static

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
 * WorkManager worker responsible for downloading in-app static resources.
 *
 * Delegates retry handling to [StaticProcess].
 *
 * @param appContext Application context used by WorkManager.
 * @param workerParams Parameters associated with the current work request.
 */
internal class StaticWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Downloads in-app static resources.
     *
     * @return [Result.success] if processing is complete or the bounded retry
     * budget is exhausted;
     * [Result.retry] if another attempt is required.
     */
    override suspend fun doWork(): Result {
        awaitInBackground()

        return attemptOutcome(runAttemptCount) {
            pidChanged(inputData) || !StaticProcess.isRetry(applicationContext, id)
        }
    }
}
