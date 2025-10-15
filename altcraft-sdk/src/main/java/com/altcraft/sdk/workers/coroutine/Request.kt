package com.altcraft.sdk.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.Constants.RETRY_TIME_C_WORK
import java.util.concurrent.TimeUnit

/**
 * Utility object for creating preconfigured [OneTimeWorkRequest] instances for WorkManager.
 *
 * Contains shared [constraints] and a factory method for instantiating typed workers.
 */
internal object Request {

    /**
     * Default constraints requiring network connectivity.
     */
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Builds a [OneTimeWorkRequest] for the given [ListenableWorker] with backoff and constraints.
     *
     * @param inputData Optional input [Data].
     * @param tag Work tag identifier.
     * @return Configured [OneTimeWorkRequest].
     */
    private inline fun <reified W : ListenableWorker> createWorkRequest(
        tag: String,
        inputData: Data? = null
    ): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<W>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_TIME_C_WORK, TimeUnit.SECONDS)
            .apply { inputData?.let { setInputData(it) } }
            .setConstraints(constraints)
            .addTag(tag)
            .build()
    }

    /**
     * Creates push event worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun pushEventRequest(): OneTimeWorkRequest =
        createWorkRequest<Worker.PushEventCoroutineWorker>(Constants.PUSH_EVENT_C_WORK_TAG)

    /**
     * Creates mobile event worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun mobileEventRequest(): OneTimeWorkRequest =
        createWorkRequest<Worker.MobileEventCoroutineWorker>(Constants.MOBILE_EVENT_C_WORK_TAG)

    /**
     *  Creates subscribe worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun subscribeRequest(): OneTimeWorkRequest =
        createWorkRequest<Worker.SubscribeCoroutineWorker>(Constants.SUBSCRIBE_C_WORK_TAG)

    /**
     * Creates update worker request.
     *
     * @return Configured [OneTimeWorkRequest] with the proper tag.
     */
    fun updateRequest(): OneTimeWorkRequest =
        createWorkRequest<Worker.UpdateCoroutineWorker>(Constants.UPDATE_C_WORK_TAG)
}