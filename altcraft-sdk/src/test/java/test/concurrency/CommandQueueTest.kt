package test.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.sdk_events.Events
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
 *  - test_1: InitCommandQueue FIFO — executes tasks strictly in submission order (1,2,3).
 *  - test_2: InitCommandQueue resilience — continues after failures; later tasks run.
 *  - test_3: InitCommandQueue submit() is non-blocking — caller not blocked; completion observed via latch.
 *  - test_4: SubscribeCommandQueue FIFO — executes tasks in order using CountDownLatch.
 *  - test_5: SubscribeCommandQueue resilience — continues after a failure; later tasks run.
 *  - test_6: SubscribeCommandQueue submit() is non-blocking — caller not blocked; completion observed via latch.
 *  - test_7: MobileEventCommandQueue FIFO — executes tasks in submission order (1,2,3).
 *  - test_8: MobileEventCommandQueue resilience — continues after a failure; later tasks run.
 *  - test_9: MobileEventCommandQueue submit() is non-blocking — caller not blocked; completion observed via latch.
 */

// Assertion messages
private const val MSG_TASK_NOT_FINISH = "Task did not finish in time"
private const val MSG_TASKS_NOT_FINISH_IN_TIME = "Tasks did not finish in time"

// Labels
private const val LBL_BEFORE = "before"
private const val LBL_AFTER  = "after"
private const val LBL_TAIL   = "tail"

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
        every { Events.error(any(), any()) } returns mockk<DataClasses.Error>(relaxed = true)
        every { Events.error(any(), any(), any()) } returns mockk<DataClasses.Error>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Thread.sleep(20)
    }

    /** - test_1: InitCommandQueue FIFO — executes tasks strictly in submission order (1,2,3). */
    @Test
    fun initQueue_sequential_fifo() {
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(3)

        CommandQueue.InitCommandQueue.submit {
            delay(10)
            order.add(1)
            latch.countDown()
        }
        CommandQueue.InitCommandQueue.submit {
            delay(5)
            order.add(2)
            latch.countDown()
        }
        CommandQueue.InitCommandQueue.submit {
            order.add(3)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), order.toList())
    }

    /** - test_2: InitCommandQueue resilience — continues after failures; later tasks run. */
    @Test
    fun initQueue_continues_after_failure() {
        val order = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(2)

        CommandQueue.InitCommandQueue.submit {
            order.add(LBL_BEFORE)
            throw RuntimeException("boom-1")
        }
        CommandQueue.InitCommandQueue.submit {
            order.add(LBL_AFTER)
            latch.countDown()
        }
        CommandQueue.InitCommandQueue.submit {
            throw RuntimeException("boom-2")
        }
        CommandQueue.InitCommandQueue.submit {
            order.add(LBL_TAIL)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertTrue(order.contains(LBL_AFTER))
        assertTrue(order.contains(LBL_TAIL))
    }

    /** - test_3: InitCommandQueue submit() is non-blocking — caller not blocked; completion via latch. */
    @Test
    fun initQueue_submit_is_non_blocking() {
        var marker = 0
        val latch = CountDownLatch(1)

        CommandQueue.InitCommandQueue.submit {
            delay(50)
            marker = 2
            latch.countDown()
        }

        assertEquals(0, marker)
        assertTrue(MSG_TASK_NOT_FINISH, latch.await(1, TimeUnit.SECONDS))
        assertEquals(2, marker)
    }

    /** - test_4: SubscribeCommandQueue FIFO — executes tasks in order. */
    @Test
    fun subscribeQueue_sequential_fifo() {
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(3)

        CommandQueue.SubscribeCommandQueue.submit {
            delay(20)
            order.add(1)
            latch.countDown()
        }
        CommandQueue.SubscribeCommandQueue.submit {
            delay(10)
            order.add(2)
            latch.countDown()
        }
        CommandQueue.SubscribeCommandQueue.submit {
            order.add(3)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), order.toList())
    }

    /** - test_5: SubscribeCommandQueue resilience — continues after a failure; later tasks run. */
    @Test
    fun subscribeQueue_continues_after_failure() {
        val order = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(2)

        CommandQueue.SubscribeCommandQueue.submit {
            order.add(LBL_BEFORE)
            throw IllegalStateException("boom")
        }
        CommandQueue.SubscribeCommandQueue.submit {
            order.add(LBL_AFTER)
            latch.countDown()
        }
        CommandQueue.SubscribeCommandQueue.submit {
            order.add(LBL_TAIL)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertTrue(order.containsAll(listOf(LBL_AFTER, LBL_TAIL)))
    }

    /** - test_6: SubscribeCommandQueue submit() is non-blocking — caller not blocked; completion via latch. */
    @Test
    fun subscribeQueue_submit_is_non_blocking() {
        var marker = 0
        val latch = CountDownLatch(1)

        CommandQueue.SubscribeCommandQueue.submit {
            delay(50)
            marker = 2
            latch.countDown()
        }

        assertEquals(0, marker)
        assertTrue(MSG_TASK_NOT_FINISH, latch.await(1, TimeUnit.SECONDS))
        assertEquals(2, marker)
    }

    /** - test_7: MobileEventCommandQueue FIFO — executes tasks in submission order (1,2,3). */
    @Test
    fun mobileEventQueue_sequential_fifo() {
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(3)

        CommandQueue.MobileEventCommandQueue.submit {
            delay(15)
            order.add(1)
            latch.countDown()
        }
        CommandQueue.MobileEventCommandQueue.submit {
            delay(5)
            order.add(2)
            latch.countDown()
        }
        CommandQueue.MobileEventCommandQueue.submit {
            order.add(3)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), order.toList())
    }

    /** - test_8: MobileEventCommandQueue resilience — continues after a failure; later tasks run. */
    @Test
    fun mobileEventQueue_continues_after_failure() {
        val order = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(2)

        CommandQueue.MobileEventCommandQueue.submit {
            order.add(LBL_BEFORE)
            throw RuntimeException("boom-mobile")
        }
        CommandQueue.MobileEventCommandQueue.submit {
            order.add(LBL_AFTER)
            latch.countDown()
        }
        CommandQueue.MobileEventCommandQueue.submit {
            order.add(LBL_TAIL)
            latch.countDown()
        }

        assertTrue(MSG_TASKS_NOT_FINISH_IN_TIME, latch.await(1, TimeUnit.SECONDS))
        assertTrue(order.containsAll(listOf(LBL_AFTER, LBL_TAIL)))
    }

    /** - test_9: MobileEventCommandQueue submit() is non-blocking — caller not blocked; completion via latch. */
    @Test
    fun mobileEventQueue_submit_is_non_blocking() {
        var marker = 0
        val latch = CountDownLatch(1)

        CommandQueue.MobileEventCommandQueue.submit {
            delay(50)
            marker = 2
            latch.countDown()
        }

        assertEquals(0, marker)
        assertTrue(MSG_TASK_NOT_FINISH, latch.await(1, TimeUnit.SECONDS))
        assertEquals(2, marker)
    }
}