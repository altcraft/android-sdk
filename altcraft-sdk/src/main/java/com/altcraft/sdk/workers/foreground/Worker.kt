package com.altcraft.sdk.workers.foreground

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.data.Constants.SERVICE_TYPE_DATA
import com.altcraft.sdk.data.Constants.SERVICE_TYPE_MSG
import com.altcraft.sdk.events.EventList.foregroundInfoIsNull
import com.altcraft.sdk.events.EventList.notificationErr
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.extension.DataExtension.toStringMap
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.push.PushPresenter.showPush
import com.altcraft.sdk.services.manager.ServiceManager.createServiceNotification
import kotlinx.coroutines.guava.await

/**
 * Contains foreground workers for processing push events and displaying notifications.
 *
 * Ensures execution under background restrictions by using WorkManager with foreground
 * service context.
 */
internal object Worker {

    val e = foregroundInfoIsNull

    /**
     * Foreground worker that sends delivery tracking event for a received push notification.
     *
     * This worker is intended to run in the background with foreground execution privileges
     * to guarantee network access. It ensures that the delivery event reaches the server
     * even under system-imposed background restrictions.
     *
     * @param context Application context used for network and notification services.
     * @param workerParams WorkManager input parameters.
     */
    class EventForegroundWorker(
        context: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result {
            val context = applicationContext
            val pushData = inputData.toStringMap()
            return try {
                setForegroundAsync(createForegroundInfo(context, 1001) ?: exception(e)).await()

                sendPushEvent(context, DELIVERY, pushData[UID_KEY])

                Result.success()
            } catch (e: Exception) {
                error("EventForegroundWorker :: doWork", e)
                sendPushEvent(context, DELIVERY, pushData[UID_KEY])
                Result.failure()
            }
        }
    }

    /**
     * Foreground worker that loads full content for a received push notification.
     *
     * This worker provides network access in background to fetch and render
     * push content (e.g. image, text) for display, complying with system limitations
     * via foreground execution.
     *
     * @param context Application context used for content and notification rendering.
     * @param workerParams WorkManager input parameters.
     */
    class PushForegroundWorker(
        context: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result {
            val context = applicationContext
            val pushData = inputData.toStringMap()
            return try {
                setForegroundAsync(createForegroundInfo(context, 1002) ?: exception(e)).await()

                showPush(context, pushData)

                Result.success()
            } catch (e: Exception) {
                error("PushForegroundWorker :: doWork", e)
                showPush(context, pushData)
                Result.failure()
            }
        }
    }

    /**
     * Creates a [ForegroundInfo] required for running the worker as a foreground service.
     *
     * Handles channel creation if required, builds a notification, and returns
     * the appropriate [ForegroundInfo] based on API level.
     *
     * @param context Application context used to access system services.
     * @return A configured [ForegroundInfo], or `null` if creation fails.
     */
    private suspend fun createForegroundInfo(context: Context, id: Int): ForegroundInfo? {
        return try {
            val push = createServiceNotification(context) ?: exception(notificationErr)

            when {
                Build.VERSION.SDK_INT >= 34 -> ForegroundInfo(id, push, SERVICE_TYPE_MSG)
                Build.VERSION.SDK_INT >= 29 -> ForegroundInfo(id, push, SERVICE_TYPE_DATA)
                else -> ForegroundInfo(id, push)
            }
        } catch (e: Exception) {
            error("createForegroundInfo", e)
            null
        }
    }
}