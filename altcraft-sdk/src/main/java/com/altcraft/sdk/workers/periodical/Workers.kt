package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.data.room.RoomRequest.clearOldMobileEventsFromRoom
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.push.token.TokenUpdate.tokenUpdate
import com.altcraft.sdk.data.room.RoomRequest.clearOldPushEventsFromRoom
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelMobileEventWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelPushEventWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelSubscribeWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelUpdateWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.awaitCancel

/**
 * Defines periodic WorkManager workers for push delivery, subscription retries, and FCM token checks.
 *
 * Each worker executes background-safe logic when the app is not in the foreground.
 */
internal object Workers {

    /**
     * A periodic worker for performing retry operations on push events.
     *
     * @constructor Creates a new instance of `RetryPushEventWorker`.
     * @param appContext The application context.
     * @param workerParams Parameters to setup the worker.
     */
    class RetryPushEventWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        private val context = appContext

        /**
         * Scans for unsent push events and processes their delivery.
         *
         * @return `Result.success()` if all events are processed successfully,
         *         `Result.failure()` if a critical error occurs.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelPushEventWorkerTask)
                    PushEvent.isRetry(context)
                    clearOldPushEventsFromRoom(SDKdb.getDb(context))
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryPushEventWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * A periodic worker for performing retry operations on mobile events.
     *
     * @constructor Creates a new instance of `RetryPushEventWorker`.
     * @param appContext The application context.
     * @param workerParams Parameters to setup the worker.
     */
    class RetryMobileEventWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        private val context = appContext

        /**
         * Scans for unsent mobile events and processes their delivery.
         *
         * @return `Result.success()` if all events are processed successfully,
         *         `Result.failure()` if a critical error occurs.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelMobileEventWorkerTask)
                    MobileEvent.isRetry(context)
                    clearOldMobileEventsFromRoom(SDKdb.getDb(context))
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryMobileEventWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * A worker that performs the retry logic for push subscription tasks.
     *
     * @constructor Creates a new instance of `RetrySubscribeWorker`.
     * @param appContext The application context.
     * @param workerParams Parameters to setup the worker.
     */
    class RetrySubscribeWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        private val context = appContext

        /**
         * Handles the retry process for subscription operations.
         *
         * @return `Result.success()` if the operation completes successfully,
         *         `Result.failure()` if an error occurs.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelSubscribeWorkerTask)
                    PushSubscribe.isRetry(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetrySubscribeWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * A worker manager who performs checks and updates the push token.
     *
     * @constructor Creates a new instance of `RetryUpdateWorker`.
     * @param appContext The application context.
     * @param workerParams Parameters to setup the worker.
     */
    class RetryUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {
        private val context = appContext

        /**
         * Handles the retry process for token update operations.
         *
         * @return `Result.success()` if the operation completes successfully,
         *         `Result.failure()` if an error occurs.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelUpdateWorkerTask)
                    tokenUpdate(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryUpdateWorker :: doWork", e)
                Result.failure()
            }
        }
    }
}