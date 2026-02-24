package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.WorkManager
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.coroutine.Request.mobileEventRequest
import com.altcraft.sdk.workers.coroutine.Request.prUpdateRequest
import com.altcraft.sdk.workers.coroutine.Request.pushEventRequest
import com.altcraft.sdk.workers.coroutine.Request.pushProcessingRequest
import com.altcraft.sdk.workers.coroutine.Request.subscribeRequest
import com.altcraft.sdk.workers.coroutine.Request.tokUpdateRequest

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
     * Enqueues token update worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startTokenUpdateCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(tokUpdateRequest())
    } catch (e: Exception) {
        error("startTokenUpdateCoroutineWorker", e)
    }

    /**
     * Enqueues profile update worker without awaiting its result.
     *
     * @param context Application context.
     */
    fun startProfileUpdateCoroutineWorker(context: Context) = try {
        WorkManager.getInstance(context).enqueue(prUpdateRequest())
    } catch (e: Exception) {
        error("startProfileUpdateCoroutineWorker", e)
    }

    /**
     * Enqueues push processing worker without awaiting its result.
     *
     * @param context Application context.
     * @param message Push payload.
     */
    fun startPushProcessingCoroutineWorker(
        context: Context, message: Map<String, String>
    ) = try {
        WorkManager.getInstance(context).enqueue(pushProcessingRequest(message))
    } catch (e: Exception) {
        error("startPushProcessingCoroutineWorker", e)
    }
}
