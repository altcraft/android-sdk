@file:Suppress("SpellCheckingInspection")

package test.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.core.modules.ModuleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ModuleRegistryRetrySyncTest
 *
 * Covers the retry-start synchronization mechanism:
 *  - `scheduleRetryStartFallback()` creates one protective timer per snapshot.
 *  - `awaitRetryStarted()` returns immediately when both signals are complete.
 *  - `awaitRetryStarted()` force-completes both signals after the fallback timeout.
 */
class ModuleRegistryRetrySyncTest {

    private companion object {
        const val MSG_ONE_TIMER = "Only one fallback timer per snapshot is expected"
        const val MSG_IMMEDIATE = "Waiting must finish immediately when signals are complete"
        const val MSG_FALLBACK = "Fallback must force-complete both signals"
        const val TIMEOUT_MS = 3000L
    }

    @Before
    fun setUp() {
        ModuleRegistry.retryStartReady = CompletableDeferred()
        ModuleRegistry.retryFallbacks.keys.forEach {
            ModuleRegistry.retryFallbacks[it]?.cancel()
        }
        ModuleRegistry.retryFallbacks.clear()
    }

    @After
    fun tearDown() {
        ModuleRegistry.retryFallbacks.keys.forEach {
            ModuleRegistry.retryFallbacks[it]?.cancel()
        }
        ModuleRegistry.retryFallbacks.clear()
    }

    /** Multiple schedulings for the same snapshot create exactly one timer. */
    @Test
    fun scheduleRetryStartFallback_isIdempotentPerSnapshot() {
        val snapshot = CompletableDeferred<Unit>()

        ModuleRegistry.scheduleRetryStartFallback(snapshot)
        ModuleRegistry.scheduleRetryStartFallback(snapshot)
        ModuleRegistry.scheduleRetryStartFallback(snapshot)

        assertEquals(MSG_ONE_TIMER, 1, ModuleRegistry.retryFallbacks.size)
    }

    /** Distinct snapshots get their own protective timers. */
    @Test
    fun scheduleRetryStartFallback_createsTimerPerSnapshot() {
        val snapshotA = CompletableDeferred<Unit>()
        val snapshotB = CompletableDeferred<Unit>()

        ModuleRegistry.scheduleRetryStartFallback(snapshotA)
        ModuleRegistry.scheduleRetryStartFallback(snapshotB)

        assertEquals(MSG_ONE_TIMER, 2, ModuleRegistry.retryFallbacks.size)
    }

    /** When the retry signal and the snapshot are complete, the wait returns immediately. */
    @Test
    fun awaitRetryStarted_returnsImmediately_whenBothSignalsComplete() = runBlocking {
        ModuleRegistry.retryStartReady = CompletableDeferred<Unit>().apply { complete(Unit) }
        val snapshot = CompletableDeferred<Unit>().apply { complete(Unit) }

        val start = System.currentTimeMillis()

        ModuleRegistry.awaitRetryStarted(snapshot)

        assertTrue(MSG_IMMEDIATE, System.currentTimeMillis() - start < TIMEOUT_MS)
    }

    /** A never-completing snapshot is force-released by the fallback timeout. */
    @Test
    fun awaitRetryStarted_forceCompletesSignalsOnFallbackTimeout() = runBlocking {
        val snapshot = CompletableDeferred<Unit>()

        val start = System.currentTimeMillis()

        ModuleRegistry.awaitRetryStarted(snapshot)

        assertTrue(
            MSG_FALLBACK,
            System.currentTimeMillis() - start >= TIMEOUT_MS
        )
        assertTrue(MSG_FALLBACK, snapshot.isCompleted)
        assertTrue(MSG_FALLBACK, ModuleRegistry.retryStartReady.isCompleted)
    }

    /** The retry signal starts uncompleted so fresh sessions actually wait. */
    @Test
    fun retryStartReady_startsUncompleted() {
        assertFalse(MSG_FALLBACK, ModuleRegistry.retryStartReady.isCompleted)
    }

    /** When the snapshot is already complete, the guard skips the retry-start wait entirely. */
    @Test
    fun awaitRetryStarted_returnsImmediately_whenSnapshotCompleted_retryStartPending() = runBlocking {
        ModuleRegistry.retryStartReady = CompletableDeferred()
        val snapshot = CompletableDeferred<Unit>().apply { complete(Unit) }

        val start = System.currentTimeMillis()

        ModuleRegistry.awaitRetryStarted(snapshot)

        assertTrue(MSG_IMMEDIATE, System.currentTimeMillis() - start < TIMEOUT_MS)
        assertFalse(MSG_ONE_TIMER, ModuleRegistry.retryStartReady.isCompleted)
        assertTrue(MSG_ONE_TIMER, snapshot.isCompleted)
    }
}
