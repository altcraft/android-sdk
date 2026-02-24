package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.profile.ProfileUpdate
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate.pushTokenUpdate
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelMobileEventWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelProfileUpdateWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelPushEventWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelSubscribeWorkerTask
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelTokenUpdateWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.awaitCancel

/**
 * Periodic WorkManager workers for retrying SDK background operations.
 */
internal object Workers {

    /**
     * Periodic worker for retrying push event delivery.
     *
     * @constructor Creates a new instance of `RetryPushEventWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class RetryPushEventWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext

        /**
         * Runs retry logic for push events when the app is in background.
         *
         * @return `Result.success()` on normal completion,
         *         `Result.failure()` on exception.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelPushEventWorkerTask)
                    PushEvent.isRetry(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryPushEventWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * Periodic worker for retrying mobile event delivery.
     *
     * @constructor Creates a new instance of `RetryMobileEventWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class RetryMobileEventWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext

        /**
         * Runs retry logic for mobile events when the app is in background.
         *
         * @return `Result.success()` on normal completion,
         *         `Result.failure()` on exception.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelMobileEventWorkerTask)
                    MobileEvent.isRetry(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryMobileEventWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * Periodic worker for retrying push subscription requests.
     *
     * @constructor Creates a new instance of `RetrySubscribeWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class RetrySubscribeWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext

        /**
         * Runs retry logic for subscriptions when the app is in background.
         *
         * @return `Result.success()` on normal completion,
         *         `Result.failure()` on exception.
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
     * Periodic worker for retrying push token update.
     *
     * @constructor Creates a new instance of `RetryTokenUpdateWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class RetryTokenUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext

        /**
         * Runs retry logic for token update when the app is in background.
         *
         * @return `Result.success()` on normal completion,
         *         `Result.failure()` on exception.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelTokenUpdateWorkerTask)
                    pushTokenUpdate(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryTokenUpdateWorker :: doWork", e)
                Result.failure()
            }
        }
    }

    /**
     * Periodic worker for retrying profile update.
     *
     * @constructor Creates a new instance of `RetryProfileUpdateWorker`.
     * @param appContext Application context.
     * @param workerParams Worker parameters.
     */
    class RetryProfileUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

        private val context = appContext

        /**
         * Runs retry logic for profile update when the app is in background.
         *
         * @return `Result.success()` on normal completion,
         *         `Result.failure()` on exception.
         */
        override suspend fun doWork(): Result {
            return try {
                if (!isAppInForegrounded()) {
                    awaitCancel(context, ::cancelProfileUpdateWorkerTask)
                    ProfileUpdate.isRetry(context)
                }
                Result.success()
            } catch (e: Exception) {
                error("RetryProfileUpdateWorker :: doWork", e)
                Result.failure()
            }
        }
    }
}