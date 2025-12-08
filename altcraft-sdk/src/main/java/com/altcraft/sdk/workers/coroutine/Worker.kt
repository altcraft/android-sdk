package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Process
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.data.Constants.COUNT_SERVICE_CLOSED
import com.altcraft.sdk.data.Constants.PID
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.services.manager.ServiceManager.stopService
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Contains WorkManager classes for executing requests to send push events, send mobile events,
 * subscribe, and update the push token.
 */
internal object Worker {

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
     * Checks whether the app process has changed since the WorkRequest was created.
     *
     * If the app is in the foreground and the stored PID differs from the current one,
     * it is assumed the app was restarted and all retry logic will be handled by the SDK,
     * so there is no need to rerun this work via WorkManager.
     *
     * @param inputData Data containing the original process ID ("pid") captured at enqueue time.
     */
    private fun pidChanged(
        inputData: Data
    ) = isAppInForegrounded() && inputData.getInt(PID, -1) != Process.myPid()

    /**
     * Worker that handles processing of push event.
     *
     * @param appContext Application context.
     * @param workerParams Parameters used to initialize the worker.
     */
    class PushEventCoroutineWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {

        /**
         * Executes background work and returns [Result.retry] or [Result.success].
         */
        override suspend fun doWork(): Result {
            awaitInBackground()
            return if (
                pidChanged(inputData) || !PushEvent.isRetry(applicationContext, id)
            ) Result.success() else Result.retry()
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
         * Executes background work and returns [Result.retry] or [Result.success].
         */
        override suspend fun doWork(): Result {
            awaitInBackground()
            return if (
                pidChanged(inputData) || !MobileEvent.isRetry(applicationContext, id)
            ) Result.success() else Result.retry()
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
         * Executes background work and returns [Result.retry] or [Result.success].
         */
        override suspend fun doWork(): Result {
            awaitInBackground()

            return (if (
                pidChanged(inputData) || !PushSubscribe.isRetry(applicationContext, id)
            ) Result.success() else Result.retry()).also {
                if (it == Result.success() || runAttemptCount == COUNT_SERVICE_CLOSED) {
                    stopService(applicationContext, SUBSCRIBE_SERVICE)
                }
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
         * Executes background work and returns [Result.retry] or [Result.success].
         */
        override suspend fun doWork(): Result {
            awaitInBackground()

            return (if (
                pidChanged(inputData) || !TokenUpdate.isRetry(applicationContext, id)
            ) Result.success() else Result.retry()).also {
                if (it == Result.success() || runAttemptCount == COUNT_SERVICE_CLOSED) {
                    stopService(applicationContext, UPDATE_SERVICE)
                }
            }
        }
    }
}