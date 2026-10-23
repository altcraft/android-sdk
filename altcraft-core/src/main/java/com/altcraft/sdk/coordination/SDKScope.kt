package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Centralized coroutine scopes for the SDK.
 *
 * All SDK-launched coroutines should run on one of these scopes so that
 * uncaught exceptions are routed to [Error] and sibling coroutines are not
 * affected by a single failure ([SupervisorJob]).
 *
 * - [main]   — UI-bound work (in-app presentation).
 * - [io]     — blocking/network work (command queues, push handling).
 * - [default] — CPU-bound processing (in-app trigger processing, delays).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SDKScope {
    private val rootJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.error("AltcraftSDKScope", e)
    }

    val io = CoroutineScope(rootJob + Dispatchers.IO + exceptionHandler)
    val main = CoroutineScope(rootJob + Dispatchers.Main + exceptionHandler)
    val default = CoroutineScope(rootJob + Dispatchers.Default + exceptionHandler)
}