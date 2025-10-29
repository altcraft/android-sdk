package com.altcraft.sdk.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.PUSH_EVENT_P_WORK_NANE
import com.altcraft.sdk.data.Constants.SUB_P_WORK_NANE
import com.altcraft.sdk.data.Constants.UPDATE_P_WORK_NANE
import com.altcraft.sdk.sdk_events.Message.MOBILE_EVENT_WORK_START
import com.altcraft.sdk.sdk_events.Message.PUSH_EVENT_WORK_START
import com.altcraft.sdk.sdk_events.Message.SUB_WORK_START
import com.altcraft.sdk.sdk_events.Message.UPDATE_WORK_START
import com.altcraft.sdk.workers.periodical.CommonFunctions.createRequest
import com.altcraft.sdk.workers.periodical.CommonFunctions.createWorker

/**
 * Manages periodic background tasks for retrying push events, subscriptions, and token checks.
 *
 * Uses WorkManager to schedule and run periodic workers in the background.
 */
internal object LaunchFunctions {

    /**
     * Lazily initialized `PeriodicWorkRequest` for the **update** worker.
     *
     * Schedules periodic token update retries using `RetryUpdateWorker`.
     */
    private val updateRequest by lazy {
        createRequest(Workers.RetryUpdateWorker::class.java)
    }


    /**
     * Lazily initialized `PeriodicWorkRequest` for the **subscription** worker.
     *
     * Schedules periodic subscription retries using `RetrySubscribeWorker`.
     */
    private val subscribeRequest by lazy {
        createRequest(Workers.RetrySubscribeWorker::class.java)
    }

    /**
     * Lazily initialized `PeriodicWorkRequest` for the push event worker.
     *
     * Schedules periodic push event retries using `RetryPushEventWorker`.
     */
    private val pushEventRequest by lazy {
        createRequest(Workers.RetryPushEventWorker::class.java)
    }

    /**
     * Lazily initialized `PeriodicWorkRequest` for the mobile event worker.
     *
     * Schedules periodic mobile event retries using `RetryMobileEventWorker`.
     */
    private val mobileEventRequest by lazy {
        createRequest(Workers.RetryMobileEventWorker::class.java)
    }

    /**
     * Starts the periodic work manager for push event tasks.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalPushEventWorker(context: Context) {
        createWorker(context, PUSH_EVENT_P_WORK_NANE, pushEventRequest)
        logger(PUSH_EVENT_WORK_START)
    }

    /**
     * Starts the periodic work manager for mobile event tasks.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalMobileEventWorker(context: Context) {
        createWorker(context, MOBILE_EVENT_P_WORK_NANE, mobileEventRequest)
        logger(MOBILE_EVENT_WORK_START)
    }

    /**
     * Starts the periodic work manager for push update.
     *
     * @param context The context of the application, used to access `WorkManager`.
     */
    fun startPeriodicalUpdateWorker(context: Context) {
        createWorker(context, UPDATE_P_WORK_NANE, updateRequest)
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
}