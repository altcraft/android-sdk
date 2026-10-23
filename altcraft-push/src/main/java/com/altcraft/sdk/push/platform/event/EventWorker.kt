package com.altcraft.sdk.push.platform.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.push.util.Utils.awaitInBackground
import com.altcraft.sdk.workers.onetime.Request.attemptOutcome
import com.altcraft.sdk.workers.onetime.Request.pidChanged

/**
 * WorkManager worker responsible for processing pending push events.
 *
 * Delegates retry handling to [EventProcess].
 *
 * @param appContext Application context used by WorkManager.
 * @param workerParams Parameters associated with the current work request.
 */
internal class EventWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Processes pending push events.
     *
     * @return [Result.success] if processing is complete or the bounded retry
     * budget is exhausted;
     * [Result.retry] if another attempt is required.
     */
    override suspend fun doWork(): Result {
        awaitInBackground()

        return attemptOutcome(runAttemptCount) {
            pidChanged(inputData) || !EventProcess.isRetry(applicationContext, id)
        }
    }
}
