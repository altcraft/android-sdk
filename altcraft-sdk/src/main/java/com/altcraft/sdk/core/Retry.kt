package com.altcraft.sdk.core

import android.content.Context
import com.altcraft.sdk.concurrency.ForegroundGate.foregroundCallback
import com.altcraft.sdk.data.Preferenses.getManualToken
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager.fcmProvider
import com.altcraft.sdk.push.token.TokenManager.hmsProvider
import com.altcraft.sdk.push.token.TokenManager.rustoreProvider
import com.altcraft.sdk.push.token.TokenUpdate.tokenUpdate
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.coroutine.Worker
import com.altcraft.sdk.workers.periodical.CommonFunctions.mobileEventPeriodicalWorkerControl
import com.altcraft.sdk.workers.periodical.CommonFunctions.pushPeriodicalWorkerControl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Checking and resending pending requests. Checking the push token update.
 */
internal object Retry {
    private val supervisor = SupervisorJob()

    // Coroutine context for performPushModuleCheck() operations with centralized exception
    internal val coroutineHandler = Dispatchers.IO + CoroutineExceptionHandler { _, e ->
        Events.error("performRetryOperations", e)
    }

    // CoroutineScope with exception handling
    private val scope = CoroutineScope(supervisor + coroutineHandler)

    // Thread-safe flag to ensure retry control is executed only once.
    internal var retryControl = AtomicBoolean(false)

    /**
     * Checks whether push token acquisition is possible.
     *
     * Returns `true` if push token can be acquired —
     * either via manual token or registered provider.
     *
     * @param context Application context used to access stored tokens and provider state.
     */
    fun pushModuleIsActive(context: Context) = getManualToken(context) != null ||
            fcmProvider != null || hmsProvider != null || rustoreProvider != null

    /**
     * Launches processing of mobile events and, if applicable, push-related requests.
     *
     * What it does:
     * - Checks the state of the periodic mobile-events worker and sends any pending mobile events.
     * - If a push-token source is active (see `pushModuleIsActive()`), executes pending requests
     * related to push, including push-token update checks.
     *
     * Start conditions:
     * - Controlled by `retryControl` to ensure a single launch per process.
     * - Triggered only when the app is in the foreground (see `foregroundCallback`;
     * early return on `!foreground`).
     *
     * @param context Application context.
     */
    fun performRetryOperations(context: Context) {
        foregroundCallback { foreground ->
            if (!foreground) return@foregroundCallback
            if (retryControl.compareAndSet(false, true)) {
                scope.launch { mobileEventPeriodicalWorkerControl(context) }
                scope.launch { MobileEvent.isRetry(context) }
                if (pushModuleIsActive(context)) {
                    Worker.retryUpdate = 0
                    Worker.retrySubscribe = 0

                    scope.launch { pushPeriodicalWorkerControl(context) }
                    scope.launch { PushSubscribe.isRetry(context) }
                    scope.launch { PushEvent.isRetry(context) }
                    scope.launch { tokenUpdate(context) }
                }
            }
        }
    }
}