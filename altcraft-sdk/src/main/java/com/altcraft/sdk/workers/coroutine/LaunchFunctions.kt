package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_C_WORK_NAME
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_NAME
import com.altcraft.sdk.data.Constants.SUB_C_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_NANE
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.workers.coroutine.Request.mobileEventRequest
import com.altcraft.sdk.workers.coroutine.Request.pushEventRequest
import com.altcraft.sdk.workers.coroutine.Request.subscribeRequest
import com.altcraft.sdk.workers.coroutine.Request.updateRequest

/**
 * Provides entry points to launch coroutine-based WorkManager tasks
 * as unique work chains with predefined policies.
 *
 * Used to enqueue background jobs for push events, subscription, and updates
 * without awaiting their results.
 */
internal object LaunchFunctions {

    private val policy = ExistingWorkPolicy.REPLACE

    /**
     * Starts the push event worker as a unique work chain without waiting for its result.
     *
     * @param context Application context.
     */
    fun startPushEventCoroutineWorker(context: Context) {
        try {
            WorkManager.getInstance(context).beginUniqueWork(
                PUSH_EVENT_C_WORK_NAME, policy, pushEventRequest()
            ).enqueue()
        } catch (e: Exception) {
            error("startPushEventCoroutineWorker", e)
        }
    }

    /**
     * Starts the mobile event worker as a unique work chain without waiting for its result.
     *
     * @param context Application context.
     */
    fun startMobileEventCoroutineWorker(context: Context) {
        try {
            WorkManager.getInstance(context).beginUniqueWork(
                MOBILE_EVENT_C_WORK_NAME, policy, mobileEventRequest()
            ).enqueue()
        } catch (e: Exception) {
            error("startPushEventCoroutineWorker", e)
        }
    }

    /**
     * Starts the subscribe worker as a unique work chain without waiting for its result.
     *
     * @param context Application context.
     */
    fun startSubscribeCoroutineWorker(context: Context) {
        try {
            WorkManager.getInstance(context).beginUniqueWork(
                SUB_C_WORK_NANE, policy, subscribeRequest()
            ).enqueue()
        } catch (e: Exception) {
            error("startSubscribeCoroutineWorker", e)
        }
    }

    /**
     * Starts the update worker as a unique work chain without waiting for its result.
     *
     * @param context Application context.
     */
    fun startUpdateCoroutineWorker(context: Context) {
        try {
            WorkManager.getInstance(context).beginUniqueWork(
                UPDATE_C_WORK_NANE, policy, updateRequest()
            ).enqueue()
        } catch (e: Exception) {
            error("startUpdateCoroutineWorker", e)
        }
    }
}