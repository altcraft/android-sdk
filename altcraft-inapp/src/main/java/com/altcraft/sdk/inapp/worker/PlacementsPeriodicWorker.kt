package com.altcraft.sdk.inapp.worker

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess.requestPlacementsSuspend
import com.altcraft.sdk.inapp.util.Log.error
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic worker for refreshing in-app placements.
 *
 * Scheduled with the repeat interval taken from the in-app module
 * configuration (`placementsRequestInterval`, minimum 15 minutes) and an
 * initial delay equal to the same interval, so the first run happens when
 * the request throttle has elapsed.
 *
 * Unlike [PeriodicWorker], the body is executed in the foreground as well:
 * keeping placements fresh matters most while the user is active, and the
 * request itself is rate-limited by the placements request interval.
 *
 * @param appContext Application context.
 * @param workerParams Worker parameters.
 */
internal class PlacementsPeriodicWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val context = appContext

    /**
     * Requests actual in-app placements.
     *
     * A failed request is simply repeated on the next periodic run, so no
     * additional retry result is returned.
     */
    override suspend fun doWork(): Result {
        return try {
            requestPlacementsSuspend(context)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("PlacementsPeriodicWorker :: doWork", e)
            Result.failure()
        }
    }
}
