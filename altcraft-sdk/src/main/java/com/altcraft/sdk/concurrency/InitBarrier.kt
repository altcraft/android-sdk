package com.altcraft.sdk.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.events.EventList.initAwait
import com.altcraft.sdk.events.EventList.sdkInitWaitingExpired
import com.altcraft.sdk.events.Events.event
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Single-process init barrier.
 *
 * Invariant:
 * - beginInit() MUST be called only inside `initMutex.withLock { ... }` in the init path.
 * - Other callers only read `current()` / call `await()`.
 */
internal object InitBarrier {

    /** Current init gate. @Volatile → visible across threads. */
    @Volatile
    private var gate: CompletableDeferred<Unit> = CompletableDeferred()

    /** Returns the current gate (may already be completed). */
    fun current(): CompletableDeferred<Unit> = gate

    /**
     * If the current gate is completed → publish a new one;
     * otherwise return the current gate.
     * cur = old gate, next = new gate.
     */
    fun reserve(): CompletableDeferred<Unit> {
        val cur = gate
        if (!cur.isCompleted) return cur
        val next = CompletableDeferred<Unit>()
        gate = next
        return next
    }

    /** Completes the given gate (g) successfully. */
    fun complete(g: CompletableDeferred<Unit>) = g.complete(Unit)

    /** Completes the given gate (g) exceptionally with error (e). */
    fun fail(g: CompletableDeferred<Unit>, e: Throwable) = g.completeExceptionally(e)
}

/**
 * Awaits completion of an initialization gate.
 *
 * If [gate] is already completed, returns immediately; otherwise waits up to [timeoutMs].
 * On timeout, emits a failure event and returns without throwing.
 *
 * @param function Logical source tag (e.g., caller name) used for event logging
 *                 when emitting `initAwait` / `initAwaitFailure`.
 * @param gate The gate to wait for (defaults to a snapshot of `InitBarrier.current()`).
 * @param timeoutMs Max wait in milliseconds; `null` = wait indefinitely.
 */
internal suspend fun awaitInit(
    function: String,
    gate: CompletableDeferred<Unit> = InitBarrier.current(),
    timeoutMs: Long? = 7_000
) {
    if (gate.isCompleted) return
    event(function, initAwait)
    try {
        if (timeoutMs == null) gate.await() else withTimeout(timeoutMs) {
            gate.await()
        }
    } catch (_: TimeoutCancellationException) {
        event(function, sdkInitWaitingExpired)
    }
}

/**
 * Runs [block] only after initialization is ready.
 *
 * Waits for [gate] (or the current gate if `null`) with an optional timeoutMs,
 * then executes and returns the result of [block].
 *
 * @param gate the gate to await; `null` uses `InitBarrier.current()`.
 * @return the result of [block].
 * @throws Throwable propagates the gate’s failure if it completed exceptionally.
 */
internal suspend fun <T> withInitReady(
    function: String,
    gate: CompletableDeferred<Unit>? = null,
    block: suspend () -> T
): T {
    awaitInit(function, gate ?: InitBarrier.current())
    return block()
}