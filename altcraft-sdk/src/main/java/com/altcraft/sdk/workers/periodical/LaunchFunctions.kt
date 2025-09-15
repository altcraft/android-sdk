package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.data.Constants.CHECK_P_WORK_NANE
import com.altcraft.sdk.data.Constants.EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.events.Message.EVENT_WORK_START
import com.altcraft.sdk.events.Message.SUB_WORK_START
import com.altcraft.sdk.events.Message.TOKEN_CHECK_WORK_START
import com.altcraft.sdk.events.Message.UPDATE_WORK_START
import com.altcraft.sdk.workers.periodical.CommonFunctions.createRequest
import com.altcraft.sdk.workers.periodical.CommonFunctions.createWorker

/**
 * Manages periodic background tasks for retrying push events, subscriptions, and token checks.
 *
 * Uses WorkManager to schedule and run periodic workers in the background.
 */
internal object LaunchFunctions {

    /**
     * Lazily initialized `PeriodicWorkRequest` for the subscription + update worker.
     *
     * This request schedules a periodic task using `RetryUpdateAndSubWorker`.
     */
    private val updateAndSubRequest by lazy {
        createRequest(Workers.RetryUpdateWorker::class.java)
    }

    /**
     * Lazily initialized `PeriodicWorkRequest` for the subscription worker.
     *
     * This request schedules a periodic task using `RetrySubscribeWorker`.
     */
    private val subscribeRequest by lazy {
        createRequest(Workers.RetrySubscribeWorker::class.java)
    }

    /**
     * Lazily initialized `PeriodicWorkRequest` for the token check worker.
     *
     * This request schedules a periodic task using `TokenCheckWorker`.
     */
    private val tokenCheckRequest by lazy {
        createRequest(Workers.TokenCheckWorker::class.java)
    }

    /**
     * Lazily initialized `PeriodicWorkRequest` for the push event worker.
     *
     * This request schedules a periodic task using `RetryPushEventWorker`.
     */
    private val eventRequest by lazy {
        createRequest(Workers.RetryPushEventWorker::class.java)
    }

    /**
     * Starts the periodic work manager for push event tasks.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalPushEventWorker(context: Context) {
        createWorker(context, EVENT_P_WORK_NANE, eventRequest)
        logger(EVENT_WORK_START)
    }

    /**
     * Starts the periodic work manager for push update.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalUpdateWorker(context: Context) {
        createWorker(context, UPDATE_P_WORK_NANE, updateAndSubRequest)
        logger(UPDATE_WORK_START)
    }

    /**
     * Starts the periodic work manager for push subscription.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalSubscribeWorker(context: Context) {
        createWorker(context, SUB_P_WORK_NANE, subscribeRequest)
        logger(SUB_WORK_START)
    }

    /**
     * Starts the Periodic operation manager to request the current fcm token.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalTokenCheckWorker(context: Context) {
        createWorker(context, CHECK_P_WORK_NANE, tokenCheckRequest)
        logger(TOKEN_CHECK_WORK_START)
    }
}