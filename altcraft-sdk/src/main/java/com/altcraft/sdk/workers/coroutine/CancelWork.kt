package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.sdk_events.Events.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides suspend-based cancellation utilities for coroutine-based workers.
 *
 * Used to cancel WorkManager tasks identified by specific tags such as
 * [SUBSCRIBE_C_WORK_TAG], [UPDATE_C_WORK_TAG], and [PUSH_EVENT_C_WORK_TAG].
 */
internal object CancelWork {

    /**
     * Cancels subscription worker tasks and triggers a callback.
     *
     * @param context The application context.
     * @param onComplete Callback when canceled.
     */
    fun cancelSubscribeWorkerTask(context: Context, onComplete: () -> Unit) {
        cancelWorkerByTag(context, SUBSCRIBE_C_WORK_TAG, onComplete)
    }

    /**
     * Cancels update worker tasks and triggers a callback.
     *
     * @param context The application context.
     * @param onComplete Callback when canceled.
     */
    fun cancelUpdateWorkerTask(context: Context, onComplete: () -> Unit) {
        cancelWorkerByTag(context, UPDATE_C_WORK_TAG, onComplete)
    }

    /**
     * Cancels push event worker tasks and triggers a callback.
     *
     * @param context The application context.
     * @param onComplete Callback when canceled.
     */
    fun cancelPushEventWorkerTask(context: Context, onComplete: () -> Unit) {
        cancelWorkerByTag(context, PUSH_EVENT_C_WORK_TAG, onComplete)
    }

    /**
     * Cancels push event worker tasks and triggers a callback.
     *
     * @param context The application context.
     * @param onComplete Callback when canceled.
     */
    fun cancelMobileEventWorkerTask(context: Context, onComplete: () -> Unit) {
        cancelWorkerByTag(context, MOBILE_EVENT_C_WORK_TAG, onComplete)
    }

    /**
     * Cancels worker tasks by tag and triggers callback when done.
     *
     * @param context The application context.
     * @param tag The worker tag.
     * @param onComplete Callback when canceled.
     */
    private fun cancelWorkerByTag(context: Context, tag: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag(tag).result.get()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                error("cancelWorkerByTag", e)
            }
        }
    }

    /**
     * Cancels all worker tasks and triggers a callback when done.
     *
     * @param context The application context.
     * @param onComplete Callback when all tasks are canceled.
     */
    fun cancelCoroutineWorkersTask(context: Context, onComplete: () -> Unit) {
        val workManager = WorkManager.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jobs = listOf(
                    async { workManager.cancelAllWorkByTag(SUBSCRIBE_C_WORK_TAG).result.get() },
                    async { workManager.cancelAllWorkByTag(UPDATE_C_WORK_TAG).result.get() },
                    async { workManager.cancelAllWorkByTag(PUSH_EVENT_C_WORK_TAG).result.get() },
                    async { workManager.cancelAllWorkByTag(MOBILE_EVENT_C_WORK_TAG).result.get() }
                )
                jobs.awaitAll()
            } catch (e: Exception) {
                error("cancelCoroutineWorkersTask", e)
            }
            finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}