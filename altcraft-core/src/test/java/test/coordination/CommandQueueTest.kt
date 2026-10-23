@file:Suppress("SpellCheckingInspection")

package test.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Error
import com.altcraft.sdk.events.Events
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CommandQueueTest
 *
 * Positive scenarios:
 *  - test_1: FIFO — executes tasks strictly in submission order (1,2,3).
 *  - test_2: Resilience — continues after failures; later tasks run.
 *  - test_3: Non-blocking — submit() returns immediately; completion observed via latch.
 */
class CommandQueueTest {

    @Before
    fun setUp() {
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk<Error>(relaxed = true)
        every { Events.error(any(), any(), any()) } returns mockk<Error>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Thread.sleep(20)
    }

    /** - test_1: FIFO — executes tasks strictly in submission order (1,2,3). */
    @Test
    fun serialQueue_sequential_fifo() {
        val queue = SerialCommandQueue("test-fifo")
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(3)

        queue.submit {
            delay(10)
            order.add(1)
            latch.countDown()
        }
        queue.submit {
            delay(5)
            order.add(2)
            latch.countDown()
        }
        queue.submit {
            order.add(3)
            latch.countDown()
        }

        assertTrue("Tasks did not finish in time", latch.await(1, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), order.toList())
    }

    /** - test_2: Resilience — continues after failures; later tasks run. */
    @Test
    fun serialQueue_continues_after_failure() {
        val queue = SerialCommandQueue("test-resilience")
        val order = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(2)

        queue.submit {
            order.add("before")
            throw RuntimeException("boom-1")
        }
        queue.submit {
            order.add("after")
            latch.countDown()
        }
        queue.submit {
            throw RuntimeException("boom-2")
        }
        queue.submit {
            order.add("tail")
            latch.countDown()
        }

        assertTrue("Tasks did not finish in time", latch.await(1, TimeUnit.SECONDS))
        assertTrue(order.contains("after"))
        assertTrue(order.contains("tail"))
    }

    /** - test_3: submit() is non-blocking — caller not blocked; completion via latch. */
    @Test
    fun serialQueue_submit_is_non_blocking() {
        val queue = SerialCommandQueue("test-nonblocking")
        var marker = 0
        val latch = CountDownLatch(1)

        queue.submit {
            delay(50)
            marker = 2
            latch.countDown()
        }

        assertEquals(0, marker)
        assertTrue("Task did not finish in time", latch.await(1, TimeUnit.SECONDS))
        assertEquals(2, marker)
    }

    /**
     * - test_4: Consumer survives a command throwing Error
     *   (e.g. NoClassDefFoundError from an optional module); later tasks run.
     */
    @Test
    fun serialQueue_continues_after_error() {
        val queue = SerialCommandQueue("test-error-resilience")
        val order = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(1)

        queue.submit {
            order.add("before")
            throw NoClassDefFoundError("optional-module")
        }
        queue.submit {
            order.add("after")
            latch.countDown()
        }

        assertTrue("Task did not finish in time", latch.await(1, TimeUnit.SECONDS))
        assertTrue(order.contains("after"))
    }
}
