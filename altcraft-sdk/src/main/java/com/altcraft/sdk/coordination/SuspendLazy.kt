package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.sdk_events.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe lazy initializer.
 */
internal class SuspendLazy<T>(
    private val initializer: suspend () -> T,
) {
    private var deferred: Deferred<T>? = null
    private val mutex = Mutex()

    /**
     * Gets value or fallback on error.
     *
     * If the caller's coroutine is cancelled — rethrows [CancellationException]
     * so cancellation propagates. If the initializer itself fails (including
     * throwing a CancellationException while the caller is still active) —
     * logs the error, resets state and yields `null`.
     */
    suspend fun get(): T? = mutex.withLock {
        try {
            deferred?.await() ?: run {
                CoroutineScope(Dispatchers.Default)
                    .async(start = CoroutineStart.LAZY) {
                        initializer()
                    }
                    .also { deferred = it }
                    .await()
            }
        } catch (e: CancellationException) {
            deferred = null
            if (currentCoroutineContext().isActive) {
                Events.error("SuspendLazy", e)
                null
            } else {
                throw e
            }
        } catch (e: Exception) {
            Events.error("SuspendLazy", e)
            deferred = null
            null
        }
    }
}