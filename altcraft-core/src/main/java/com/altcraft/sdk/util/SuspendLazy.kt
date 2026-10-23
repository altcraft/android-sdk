package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe lazy initializer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SuspendLazy<T>(
    private val initializer: suspend () -> T,
) {

    private val mutex = Mutex()

    private var value: T? = null

    private var initialized = false

    /**
     * Gets value or fallback on error.
     * Propagates cancellation; errors are logged internally.
     */
    suspend fun get(): T? = mutex.withLock {
        if (initialized) return@withLock value


        try {
            initializer().also {
                value = it
                initialized = true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("SuspendLazy", e)
            null
        }
    }
}