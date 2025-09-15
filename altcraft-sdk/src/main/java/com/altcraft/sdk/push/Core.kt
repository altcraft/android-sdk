package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.concurrency.ForegroundGate.foregroundCallback
import com.altcraft.sdk.data.Preferenses.getManualToken
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager.fcmProvider
import com.altcraft.sdk.push.token.TokenManager.hmsProvider
import com.altcraft.sdk.push.token.TokenManager.rustoreProvider
import com.altcraft.sdk.push.token.TokenUpdate.tokenUpdate
import com.altcraft.sdk.workers.coroutine.Worker.retrySubscribe
import com.altcraft.sdk.workers.coroutine.Worker.retryUpdate
import com.altcraft.sdk.workers.periodical.CommonFunctions.periodicalWorkerControl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * It checks the device's push token change and initializes processing of requests related to
 * push notifications.
 */
internal object Core {
    private val supervisor = SupervisorJob()

    // Coroutine context for performPushModuleCheck() operations with centralized exception
    internal val coroutineHandler = Dispatchers.IO + CoroutineExceptionHandler { _, e ->
        Events.error("performPushModuleCheck", e)
    }

    // CoroutineScope with exception handling
    private val scope = CoroutineScope(supervisor + coroutineHandler)

    // Thread-safe flag to ensure systemControl is executed only once.
    internal var pushControl = AtomicBoolean(false)

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
     * Launches background tasks related to the push module:
     * - periodic worker supervision,
     * - retry flow for pending subscriptions,
     * - push-token update checks,
     * - retrying delivery/open push events.
     *
     * The start is gated by [pushControl] (one start per process) and is triggered
     * only when the app is not in foreground (via `foregroundCallback`).
     *
     * @param context Application context.
     */
    fun performPushModuleCheck(context: Context) {
        foregroundCallback { foreground ->

            if ( pushControl.compareAndSet(false, true)) {
                retryUpdate = 0
                retrySubscribe = 0

                scope.launch { periodicalWorkerControl(context) }
                scope.launch { PushSubscribe.isRetry(context) }
                scope.launch { PushEvent.isRetry(context) }
                scope.launch { tokenUpdate(context) }
            }
        }
    }
}
