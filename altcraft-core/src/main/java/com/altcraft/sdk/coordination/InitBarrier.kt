package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log.event
import com.altcraft.sdk.events.CoreEvents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Single-process init barrier.
 *
 * Invariant:
 * - `reserve()` is thread-safe and may be called from the init path before
 *   the init task is queued, so new commands immediately observe the fresh gate.
 * - `reset()` is called only inside the serial `Init.initQueue` (clear path)
 *   to keep gate re-arming ordered with the wipe.
 * - Other callers only read `current()` / call `await()`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InitBarrier {

    /** Current init gate. @Volatile → visible across threads. */
    @Volatile
    private var gate: CompletableDeferred<Unit> = CompletableDeferred()

    /** Returns the current gate (may already be completed). */
    fun current(): CompletableDeferred<Unit> = gate

    /**
     * If the current gate is completed → publish a new one;
     * otherwise return the current gate.
     * cur = old gate, next = new gate.
     *
     * Synchronized so the check-then-act is atomic: concurrent init calls
     * attach to the same pending gate instead of racing to publish
     * different ones. Equivalent to fully serialized reservation.
     */
    @Synchronized
    internal fun reserve(): CompletableDeferred<Unit> {
        val cur = gate
        if (!cur.isCompleted) return cur
        val next = CompletableDeferred<Unit>()
        gate = next
        return next
    }

    /** Completes the given gate (g) successfully. */
    internal fun initBarrierComplete(g: CompletableDeferred<Unit>) = g.complete(Unit)

    /**
     * Publishes a new uncompleted gate when the current gate is completed.
     *
     * Used by the clear path so that commands submitted during or after
     * cleanup await the next initialization instead of acting on cleared
     * storage. Like [reserve], it must only be called inside the serial
     * `Init.initQueue`.
     */
    internal fun reset() {
        reserve()
    }
}

/**
 * Waits for SDK initialization to complete.
 *
 * @param timeout Maximum wait duration, or `null` to wait indefinitely.
 */
internal suspend fun awaitInit(
    function: String? = null,
    gate: CompletableDeferred<Unit> = InitBarrier.current(),
    timeout: Duration? = 7.seconds
) {
    if (gate.isCompleted) return

    try {
        if (timeout == null) gate.await() else withTimeout(timeout) {
            gate.await()
        }

    } catch (_: TimeoutCancellationException) {
        if (function != null) event(
            function, CoreEvents.sdkInitWaitingExpired
        )
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun <T> withInitReady(
    function: String? = null,
    gate: CompletableDeferred<Unit>? = null,
    block: suspend () -> T
): T {
    awaitInit(function, gate ?: InitBarrier.current())
    return block()
}