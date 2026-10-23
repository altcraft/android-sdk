package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import com.altcraft.sdk.data.Constants.RETRY_TIME_P_WORK
import com.altcraft.sdk.util.log.Log.error
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.guava.await
import java.util.concurrent.TimeUnit

/**
 * Contains helpers for scheduling periodic background tasks with WorkManager.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CommonFunctions {

    /**
     * Network constraint: work only runs with active connection.
     */
    @PublishedApi
    internal val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Ensures that the specified periodic worker is scheduled.
     *
     * Starts the worker if no active work exists or the previous one was canceled.
     * The repeat interval and initial delay are specified in minutes.
     *
     * @param context Application context.
     * @param workTag Unique periodic work tag (used as both unique name and tag).
     * @param repeatInterval Repeat interval in minutes.
     * @param initialDelay Initial delay before the first run in minutes.
     */
    suspend inline fun <reified W : CoroutineWorker> ensureWorker(
        context: Context,
        workTag: String,
        repeatInterval: Long = RETRY_TIME_P_WORK,
        initialDelay: Long = 0
    ) {
        val state = try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(
                    workTag
                )
                .await()
                .firstOrNull()
                ?.state
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getWorkState", e)
            null
        }

        if (state == null || state == State.CANCELLED) {
            startPeriodicalWorker<W>(
                context = context,
                workTag = workTag,
                repeatInterval = repeatInterval,
                initialDelay = initialDelay
            )
        }
    }

    /**
     * Creates and enqueues a unique periodic worker of type [W].
     *
     * The repeat interval and initial delay are specified in minutes.
     * Replaces existing periodic work with the same tag if present.
     * Exceptions during scheduling are caught and logged.
     *
     * @param context Application context.
     * @param workTag Unique periodic work tag (used as both unique name and tag).
     * @param repeatInterval Repeat interval in minutes.
     * @param initialDelay Initial delay before the first run in minutes.
     */
    @PublishedApi
    internal inline fun <reified W : CoroutineWorker> startPeriodicalWorker(
        context: Context,
        workTag: String,
        repeatInterval: Long = RETRY_TIME_P_WORK,
        initialDelay: Long = 0
    ) {
        try {
            val request = PeriodicWorkRequest.Builder(
                W::class.java,
                repeatInterval,
                TimeUnit.MINUTES
            )
                .setInitialDelay(
                    initialDelay,
                    TimeUnit.MINUTES
                )
                .setConstraints(
                    constraints
                )
                .addTag(workTag)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workTag,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            error(workTag, e)
        }
    }
}