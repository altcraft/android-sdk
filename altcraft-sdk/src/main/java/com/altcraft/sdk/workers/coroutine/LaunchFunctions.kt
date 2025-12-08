package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.WorkManager
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.coroutine.Request.mobileEventRequest
import com.altcraft.sdk.workers.coroutine.Request.pushEventRequest
import com.altcraft.sdk.workers.coroutine.Request.subscribeRequest
import com.altcraft.sdk.workers.coroutine.Request.updateRequest

/**
 * Provides entry points to enqueue coroutine-based WorkManager tasks.
 */
internal object LaunchFunctions {

    /**
     * Enqueues push event worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startPushEventCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(pushEventRequest())
    } catch (e: Exception) {
        error("startPushEventCoroutineWorker", e)
    }

    /**
     * Enqueues mobile event worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startMobileEventCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(mobileEventRequest())
    } catch (e: Exception) {
        error("startMobileEventCoroutineWorker", e)
    }

    /**
     * Enqueues subscribe worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startSubscribeCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(subscribeRequest())
    } catch (e: Exception) {
        error("startSubscribeCoroutineWorker", e)
    }

    /**
     * Enqueues update worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startUpdateCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(updateRequest())
    } catch (e: Exception) {
        error("startUpdateCoroutineWorker", e)
    }
}
