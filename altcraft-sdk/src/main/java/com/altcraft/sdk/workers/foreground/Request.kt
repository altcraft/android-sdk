package com.altcraft.sdk.workers.foreground

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.altcraft.sdk.events.Events.error

/**
 * Provides methods to start foreground workers for processing push events and delivery.
 *
 * Foreground workers are used to ensure reliable background execution with system priority
 * when handling push logic.
 *
 * Launch conditions (common to all functions in this object):
 * - The app is in the background (not foregrounded).
 * - The message is not subject to Android 12+ FCM restrictions
 *   (i.e., NOT the case: provider = FCM AND device runs API 31+ with
 *   foreground service restrictions), otherwise a
 *   `ForegroundServiceStartNotAllowedException` may occur.
 * - Input data for the worker is properly prepared (see `getWorkData(...)`).
 *
 * If conditions are not met, the task is executed directly in a coroutine
 * without using WorkManager.
 */
internal object Request {

    /**
     * Starts a foreground worker to provide an active network connection
     * during the sending of a push delivery event.
     *
     * @param context The application context used to access [WorkManager].
     * @param pushData The [Data] passed to [Worker.EventForegroundWorker].
     */
    fun startEventForegroundWorker(context: Context, pushData: Data) {
        try {
            val request = OneTimeWorkRequestBuilder<Worker.EventForegroundWorker>()
                .setInputData(pushData)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            error("startEventForegroundWorker", e)
        }
    }

    /**
     * Starts a foreground worker to provide an active network connection
     * while loading push notification content (e.g., images).
     *
     * @param context The application context used to access [WorkManager].
     * @param pushData The [Data] passed to [Worker.PushForegroundWorker].
     */
    fun startPushForegroundWorker(context: Context, pushData: Data) {
        try {
            val request = OneTimeWorkRequestBuilder<Worker.PushForegroundWorker>()
                .setInputData(pushData)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            error("startPushForegroundWorker", e)
        }
    }
}
