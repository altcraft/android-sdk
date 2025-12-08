package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.util.Log
import com.altcraft.sdk.data.Constants.LOG_NULL
import com.altcraft.sdk.data.Constants.LOG_TAG
import com.altcraft.sdk.sdk_events.Message.LOG_HINT
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized logger for Altcraft SDK that respects the user's logging preferences.
 *
 * Logging behavior:
 * - If logging status is `true`  -> all logs are printed.
 * - If logging status is `false` -> no logs are printed at all.
 * - If logging status is `null`  -> logging is not yet configured:
 *      • SDK emits a one-time integration hint to help developers
 *      • no actual logs are printed until logging is explicitly enabled
 */
internal object Logger {
    private val integrationHintLogged = AtomicBoolean(false)
    internal var loggingStatus: Boolean? = null

    /**
     * Logs an SDK message according to the current logging status.
     *
     * @param message The message to be logged.
     */
    fun log(message: String?) {
        when (loggingStatus) {
            false -> return
            true -> Log.d(LOG_TAG, message ?: LOG_NULL)
            null -> logIntegrationHintOnce()
        }
    }

    /**2
     * Emits a one-time integration hint explaining how to enable logging.
     *
     * AtomicBoolean ensures that the hint is logged only once, even if multiple
     * threads call this method concurrently.
     */
    private fun logIntegrationHintOnce() {
        if (integrationHintLogged.compareAndSet(false, true)) {
            Log.d(LOG_TAG, LOG_HINT)
        }
    }
}