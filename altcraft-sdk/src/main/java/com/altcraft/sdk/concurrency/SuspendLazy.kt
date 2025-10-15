package com.altcraft.sdk.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.sdk_events.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe lazy initializer that swallows errors
 * and resets state on failure.
 */
internal class SuspendLazy<T>(
    private val initializer: suspend () -> T,
) {
    private var deferred: Deferred<T>? = null
    private val mutex = Mutex()

    /**
     * Gets value or fallback on error.
     * Never throws - errors are logged internally.
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
        } catch (_: CancellationException) {
            deferred = null
            null
        } catch (e: Exception) {
            Events.error("SuspendLazy", e)
            deferred = null
            null
        }
    }
}