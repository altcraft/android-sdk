package test.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.concurrency.awaitInit
import com.altcraft.sdk.concurrency.withInitReady
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * InitBarrierTest
 *
 * Positive scenarios:
 *  - test_1: reserve() returns the same gate until completed; after completion returns a new gate and current() points to it.
 *  - test_2: awaitInit on a completed gate returns immediately.
 *  - test_3: awaitInit times out without throwing and the gate remains incomplete.
 *  - test_4: withInitReady waits for the gate, executes the block, and returns its result.
 *  - test_5: awaitInit with default args waits on current() and completes when the current gate completes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InitBarrierTest {

    private companion object {
        private const val MSG_FRESH_GATE_INCOMPLETE = "Fresh gate must be incomplete"
        private const val MSG_RESERVE_SAME = "reserve() should return the same gate if not completed"
        private const val MSG_NEW_GATE_INCOMPLETE = "New gate must be incomplete"
        private const val MSG_NEW_GATE_DIFFERENT = "New gate must be a different instance"
        private const val MSG_CURRENT_REF_NEW = "current() should reference the new gate"
        private const val MSG_AWAIT_COMPLETED_RET_IMMEDIATELY = "awaitInit should return immediately for completed gate"
        private const val MSG_TIMEOUT_COMPLETES_NO_THROW = "awaitInit should complete after timeout without throwing"
        private const val MSG_GATE_REMAINS_INCOMPLETE = "Gate remains incomplete (we didn't complete it)"
        private const val MSG_BLOCK_EXECUTES_AFTER_GATE = "Block must execute after gate completion"
        private const val MSG_DEFAULT_CURRENT_COMPLETES = "awaitInit should complete when current gate completes"
    }

    @Before
    fun setUp() {
        io.mockk.mockkStatic(android.util.Log::class)
        io.mockk.every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        io.mockk.every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        io.mockk.every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() = io.mockk.unmockkAll()

    private fun freshGate(): CompletableDeferred<Unit> {
        val cur = InitBarrier.current()
        if (!cur.isCompleted) {
            InitBarrier.complete(cur)
        }
        return InitBarrier.reserve()
    }

    /** - test_1: reserve() returns same gate until completed, then returns a new gate and updates current(). */
    @Test
    fun reserve_behaviour_same_then_new() = runTest {
        val g1 = freshGate()
        assertFalse(MSG_FRESH_GATE_INCOMPLETE, g1.isCompleted)

        val r1 = InitBarrier.reserve()
        assertTrue(MSG_RESERVE_SAME, r1 === g1)

        InitBarrier.complete(g1)
        assertTrue(g1.isCompleted)

        val g2 = InitBarrier.reserve()
        assertFalse(MSG_NEW_GATE_INCOMPLETE, g2.isCompleted)
        assertTrue(MSG_NEW_GATE_DIFFERENT, g2 !== g1)
        assertTrue(MSG_CURRENT_REF_NEW, InitBarrier.current() === g2)
    }

    /** - test_2: awaitInit on completed gate returns immediately. */
    @Test
    fun awaitInit_completed_gate_returns_immediately() = runTest {
        val g = freshGate()
        InitBarrier.complete(g)
        val job = async { awaitInit("test-func", gate = g, timeoutMs = 10) }
        job.await()
        assertTrue(MSG_AWAIT_COMPLETED_RET_IMMEDIATELY, job.isCompleted)
    }

    /** - test_3: awaitInit times out without throwing; gate remains incomplete. */
    @Test
    fun awaitInit_times_out_without_throw() = runTest {
        val g = freshGate()
        val job = launch { awaitInit("test-func", gate = g, timeoutMs = 50) }
        assertFalse(job.isCompleted)

        this.testScheduler.advanceTimeBy(51)
        this.testScheduler.runCurrent()

        assertTrue(MSG_TIMEOUT_COMPLETES_NO_THROW, job.isCompleted)
        assertFalse(MSG_GATE_REMAINS_INCOMPLETE, g.isCompleted)
    }

    /** - test_4: withInitReady executes after gate completion and returns block result. */
    @Test
    fun withInitReady_executes_after_gate_completion() = runTest {
        val g = freshGate()
        var executed = false

        val deferred = async {
            withInitReady(function = "caller", gate = g) {
                executed = true
                42
            }
        }

        assertFalse(executed)

        InitBarrier.complete(g)
        this.testScheduler.runCurrent()

        val result = deferred.await()
        assertTrue(MSG_BLOCK_EXECUTES_AFTER_GATE, executed)
        assertEquals(42, result)
    }

    /** - test_5: awaitInit with default gate waits on current() and completes when current completes. */
    @Test
    fun awaitInit_defaults_to_current_gate() = runTest {
        val g = freshGate()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { awaitInit("default-current") }
        assertFalse(job.isCompleted)
        InitBarrier.complete(g)
        this.testScheduler.runCurrent()
        assertTrue(MSG_DEFAULT_CURRENT_COMPLETES, job.isCompleted)
    }
}