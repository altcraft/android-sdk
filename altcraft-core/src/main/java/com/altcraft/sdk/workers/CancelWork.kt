package com.altcraft.sdk.workers

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import com.altcraft.sdk.util.log.Log.error
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Provides coroutine-backed utilities for canceling WorkManager tasks by tag.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CancelWork {


    /**
     * Cancels worker tasks by their tags.
     *
     * Each cancellation waits at most [CANCEL_TIMEOUT_SECONDS] for WorkManager
     * to apply it, so a stalled executor cannot block the calling thread
     * indefinitely or exhaust the IO pool during bulk cancels.
     *
     * @param context The application context.
     * @param tags Worker tags to cancel.
     */
    suspend fun cancelWorkerTask(
        context: Context, tags: List<String>
    ) {
        try {
            withContext(Dispatchers.IO) {
                tags.forEach { tag ->
                    try {
                        WorkManager
                            .getInstance(context)
                            .cancelAllWorkByTag(tag)
                            .result
                            .get(CANCEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    } catch (e: TimeoutException) {
                        error("cancelWorkerTask :: $tag", e)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("cancelWorkerTask", e)
        }
    }

    private const val CANCEL_TIMEOUT_SECONDS = 10L
}