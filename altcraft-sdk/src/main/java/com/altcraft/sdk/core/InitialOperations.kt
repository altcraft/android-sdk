package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.ForegroundBarrier.foreground
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.profile.ProfileUpdate
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager.pushModuleIsActive
import com.altcraft.sdk.push.token.TokenUpdate.pushTokenUpdate
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.periodical.CommonFunctions.periodicalWorkerControl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Schedules retry processing for pending requests and token update checks.
 */
internal object InitialOperations {

    /** Ensures retry operations run only once per process */
    internal var initControl = AtomicBoolean(false)

    /** Centralized exception handler for init-related coroutines */
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Events.error(Constants.INIT_OPERATION_FUNC, e)
    }

    /** CoroutineScope with centralized exception handling */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + exceptionHandler
    )

    /** Signals when retry operations have been scheduled */
    val initialRequestRetryStarted = CompletableDeferred<Unit>()
    val mobileEventsDbSnapshotTaken = CompletableDeferred<Unit>()
    val pushSubscribeDbSnapshotTaken = CompletableDeferred<Unit>()
    val profileUpdatesDbSnapshotTaken = CompletableDeferred<Unit>()

    /**
     * Starts retry processing for mobile events and, if enabled, push-related tasks.
     *
     * - Runs periodic mobile-event checks.
     * - If push is active, schedules push retries and token updates.
     *
     * Conditions:
     * - Executes only once per process (`initControl`).
     * - Runs only when the app is in foreground.
     *
     * @param context Application context.
     */
    fun performInitOperations(context: Context) = foreground {
        if (it && initControl.compareAndSet(false, true)) {
            when (pushModuleIsActive(context)) {
                true -> withPushActions(context)
                false -> withoutPushActions(context)
            }
            initialRequestRetryStarted.complete(Unit)
        } else initialRequestRetryStarted.complete(Unit)
    }

    /**
     * Launches retry flow including push-related operations.
     *
     * @param context Application context .
     */
    fun withPushActions(context: Context) {
        scope.launch { pushTokenUpdate(context) }
        scope.launch { PushEvent.isRetry(context) }
        scope.launch { MobileEvent.isRetry(context) }
        scope.launch { PushSubscribe.isRetry(context) }
        scope.launch { ProfileUpdate.isRetry(context) }
        scope.launch { periodicalWorkerControl(context) }
    }

    /**
     * Launches retry flow without any push-related operations.
     *
     * @param context Application context.
     */
    fun withoutPushActions(context: Context) {
        scope.launch { periodicalWorkerControl(context) }
        scope.launch { ProfileUpdate.isRetry(context) }
        scope.launch { MobileEvent.isRetry(context) }
    }

    /**
     * Suspends until all retry operations have been scheduled.
     *
     * Used to ensure that retry-related coroutines are launched
     * before executing dependent logic.
     *
     */
    suspend fun awaitSubscribeRetryStarted() {
        fallbackExitSubRetryStartAwait()
        initialRequestRetryStarted.await()
        pushSubscribeDbSnapshotTaken.await()
    }

    /**
     * Fallback timeout for awaitSubscribeRetryStarted.
     * Unblocks the waiting coroutine after 3 seconds by
     * force-completing the retry start and push subscribe
     * snapshot signals.
     */
    private fun fallbackExitSubRetryStartAwait() {
        scope.launch {
            delay(3000)
            initialRequestRetryStarted.complete(Unit)
            pushSubscribeDbSnapshotTaken.complete(Unit)
        }
    }

    /**
     * Suspends until all retry operations have been scheduled.
     *
     * Used to ensure that retry-related coroutines are launched
     * before executing dependent logic.
     */
    suspend fun awaitMobileEventRetryStarted() {
        fallbackExitEventRetryStartAwait()
        initialRequestRetryStarted.await()
        mobileEventsDbSnapshotTaken.await()
    }

    /**
     * Fallback timeout for awaitMobileEventRetryStarted.
     * Unblocks the waiting coroutine after 3 seconds by
     * force-completing the retry start and mobile events
     * snapshot signals.
     */
    private fun fallbackExitEventRetryStartAwait() {
        scope.launch {
            delay(3000)
            initialRequestRetryStarted.complete(Unit)
            mobileEventsDbSnapshotTaken.complete(Unit)
        }
    }

    /**
     * Suspends until all retry operations have been scheduled.
     *
     * Used to ensure that retry-related coroutines are launched
     * before executing dependent logic.
     */
    suspend fun awaitProfileUpdateRetryStarted() {
        fallbackExitProfileUpdateRetryStartAwait()
        initialRequestRetryStarted.await()
        profileUpdatesDbSnapshotTaken.await()
    }

    /**
     * Fallback timeout for awaitProfileUpdateRetryStarted.
     * Unblocks the waiting coroutine after 3 seconds by
     * force-completing the retry start and profile updates
     * snapshot signals.
     */
    private fun fallbackExitProfileUpdateRetryStartAwait() {
        scope.launch {
            delay(3000)
            initialRequestRetryStarted.complete(Unit)
            profileUpdatesDbSnapshotTaken.complete(Unit)
        }
    }
}