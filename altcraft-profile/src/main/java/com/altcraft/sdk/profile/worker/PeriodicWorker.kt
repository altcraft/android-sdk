package com.altcraft.sdk.profile.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.profile.util.Log.error
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess.isRetry
import com.altcraft.sdk.profile.data.Constants.PROFILE_O_WORK_TAG
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic worker for retrying profile update requests.
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
        PROFILE_O_WORK_TAG
    )

    /**
     * Retries profile updates in background.
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
            error("$tags :: doWork", e)
            Result.failure()
        }
    }
}
