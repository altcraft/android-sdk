package test.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.events.Events
import com.altcraft.sdk.data.DataClasses
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * SuspendLazyTest
 *
 * Positive scenarios:
 *  - test_1: singleInitialization_and_cache — initializer runs exactly once and the value
 *            is cached across successive get() calls.
 *  - test_4: concurrent_calls_result_in_single_initialization — multiple concurrent get()
 *            calls serialize; initializer executes once; all callers receive the same result.
 *
 * Negative scenarios:
 *  - test_2: error_is_swallowed_and_state_resets_then_retry_succeeds — first initialization
 *            throws Exception → get() returns null; state resets; next call succeeds.
 *  - test_3: cancellation_is_swallowed_and_state_resets_then_retry_succeeds — first initialization
 *            throws CancellationException → get() returns null; state resets; next call succeeds.
 *
 * Notes:
 *  - android.util.Log and Events.error(...) are mocked to avoid side effects in JVM tests.
 *  - CountDownLatch is used to assert the initializer actually started; CompletableDeferred
 *    coordinates the completion in the concurrent test.
 */

// ---------- Assertion messages ----------
private const val MSG_INIT_CALLED_ONCE = "Initializer must be called exactly once"
private const val MSG_FIRST_NULL_ON_ERROR = "First call should return null on error"
private const val MSG_INIT_NOT_STARTED_IN_TIME = "Initializer did not start in time"
private const val MSG_FIRST_NULL_ON_CANCELLATION = "First call should return null on cancellation"
private const val MSG_INIT_AT_LEAST_TWICE = "Initializer should run at least twice"
private const val MSG_EXPECT_VALUE = "Expected cached value"
private const val MSG_EXPECT_DONE = "Expected 'done' after retry"
private const val MSG_EXPECT_42 = "Expected computed value 42"
private const val MSG_SINGLE_INIT_CONCURRENT = "Initializer must run exactly once"

/** Tests for SuspendLazy<T> */
class SuspendLazyTest {

    @Before
    fun setUp() {
        // Mute android.util.Log for JVM tests (Events may use Log inside)
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Mute Events.error and return a proper value (not Unit)
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk<DataClasses.Error>(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    /** Ensures initializer runs only once and value is cached across successive get() calls */
    @Test
    fun singleInitialization_and_cache() = runBlocking {
        val calls = AtomicInteger(0)
        val lazy = com.altcraft.sdk.concurrency.SuspendLazy {
            calls.incrementAndGet()
            "value"
        }

        val v1 = lazy.get()
        val v2 = lazy.get()

        assertEquals(MSG_EXPECT_VALUE, "value", v1)
        assertEquals(MSG_EXPECT_VALUE, "value", v2)
        assertEquals(MSG_INIT_CALLED_ONCE, 1, calls.get())
    }

    /** Ensures exceptions are swallowed, state is reset, and next call retries initializer */
    @Test
    fun error_is_swallowed_and_state_resets_then_retry_succeeds() = runBlocking {
        val calls = AtomicInteger(0)
        val lazy = com.altcraft.sdk.concurrency.SuspendLazy {
            when (calls.incrementAndGet()) {
                1 -> throw IllegalStateException("boom")
                else -> "ok"
            }
        }

        val first = lazy.get()
        val second = lazy.get()

        assertNull(MSG_FIRST_NULL_ON_ERROR, first)
        assertEquals(MSG_EXPECT_DONE, "ok", second)
        assertEquals(MSG_INIT_AT_LEAST_TWICE, 2, calls.get())
    }

    /** Ensures CancellationException is handled, state resets, and next get() succeeds */
    @Test
    fun cancellation_is_swallowed_and_state_resets_then_retry_succeeds() = runBlocking {
        val calls = AtomicInteger(0)
        val started = CountDownLatch(1)

        val lazy = com.altcraft.sdk.concurrency.SuspendLazy {
            when (calls.incrementAndGet()) {
                1 -> {
                    // First run: signal start and simulate cancellation from inside initializer
                    started.countDown()
                    throw CancellationException("simulated cancellation")
                }
                else -> "done"
            }
        }

        // 1) First get(): initializer starts and throws CancellationException -> get() returns null
        val first = lazy.get()
        assertTrue(MSG_INIT_NOT_STARTED_IN_TIME, started.await(500, TimeUnit.MILLISECONDS))
        assertNull(MSG_FIRST_NULL_ON_CANCELLATION, first)

        // 2) Second get(): state was reset, initializer runs again and succeeds
        val second = lazy.get()
        assertEquals(MSG_EXPECT_DONE, "done", second)
        assertTrue(MSG_INIT_AT_LEAST_TWICE, calls.get() >= 2)
    }

    /** Ensures concurrent callers effectively serialize via Mutex and initializer executes once */
    @Test
    fun concurrent_calls_result_in_single_initialization() = runBlocking {
        val calls = AtomicInteger(0)
        val blocker = CompletableDeferred<Unit>()

        val lazy = com.altcraft.sdk.concurrency.SuspendLazy {
            calls.incrementAndGet()
            // hold until all callers are queued
            blocker.await()
            42
        }

        // Launch three concurrent get() calls
        val d1 = async { lazy.get() }
        val d2 = async { lazy.get() }
        val d3 = async { lazy.get() }

        // Allow initializer to finish
        blocker.complete(Unit)

        val r1 = d1.await()
        val r2 = d2.await()
        val r3 = d3.await()

        assertEquals(MSG_EXPECT_42, 42, r1)
        assertEquals(MSG_EXPECT_42, 42, r2)
        assertEquals(MSG_EXPECT_42, 42, r3)
        assertEquals(MSG_SINGLE_INIT_CONCURRENT, 1, calls.get())
    }
}