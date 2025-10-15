package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager.checkServiceClosed
import com.altcraft.sdk.services.manager.ServiceManager.closeService
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Contains WorkManager classes for executing requests to send push events, subscribe,
 * and update the push token
 */
internal object Worker {

    internal var retryUpdate = 0
    internal var retrySubscribe = 0

    /**
     * Suspends execution if the app is running in the background.
     *
     * Adds a delay of 1500 ms when the application is not in the foreground,
     * otherwise returns immediately.
     */
    suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(1500)
    }

    /**
     * Worker that handles processing of push event data.
     *
     * @param appContext Application context.
     * @param workerParams Parameters used to initialize the worker.
     */
    class PushEventCoroutineWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {
        /**
         * Executes the background task.
         * Returns success, retry, or failure based on internal logic.
         *
         * @return Result of the background task.
         */
        override suspend fun doWork(): Result {

            awaitInBackground()

            val retry = PushEvent.isRetry(applicationContext)

            return if (retry) {
                Result.retry()
            } else Result.success()
        }
    }

    /**
     * Worker that handles processing of mobile event data.
     *
     * @param appContext Application context.
     * @param workerParams Parameters used to initialize the worker.
     */
    class MobileEventCoroutineWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {
        /**
         * Executes the background task.
         * Returns success, retry, or failure based on internal logic.
         *
         * @return Result of the background task.
         */
        override suspend fun doWork(): Result {

            awaitInBackground()

            val retry = MobileEvent.isRetry(applicationContext)

            return if (retry) {
                Result.retry()
            } else Result.success()
        }
    }

    /**
     * Worker that handles subscription logic.
     *
     * @param appContext Application context
     * @param workerParams Parameters used to initialize the worker.
     */
    class SubscribeCoroutineWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {

        /**
         * Executes the background task.
         * Returns success, retry, or failure based on internal logic.
         *
         * @return Result of the background task.
         */
        override suspend fun doWork(): Result {

            awaitInBackground()

            val retry = PushSubscribe.isRetry(applicationContext)

            return if (retry) {
                checkServiceClosed(
                    applicationContext,
                    PUSH_SUBSCRIBE_SERVICE,
                    ++retrySubscribe
                )
                Result.retry()
            } else {
                closeService(applicationContext, PUSH_SUBSCRIBE_SERVICE, true)
                Result.success()
            }
        }
    }

    /**
     * Worker that performs profile update operations.
     *
     * @param appContext Application context.
     * @param workerParams Parameters used to initialize the worker.
     */
    class UpdateCoroutineWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {

        /** Unique identifier for this update execution session. */
        val uid = UUID.randomUUID().toString()

        /**
         * Executes the background task.
         * Returns success, retry, or failure based on internal logic.
         *
         * @return Result of the background task.
         */
        override suspend fun doWork(): Result {

            awaitInBackground()

            val retry = TokenUpdate.isRetry(applicationContext, uid)

            return if (retry) {
                checkServiceClosed(
                    applicationContext,
                    TOKEN_UPDATE_SERVICE,
                    ++retrySubscribe
                )
                Result.retry()
            } else {
                closeService(applicationContext, TOKEN_UPDATE_SERVICE, true)
                Result.success()
            }
        }
    }
}