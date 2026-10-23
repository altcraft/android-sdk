@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.LIFOQueue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * LIFOQueueTest
 *
 * Positive scenarios:
 *  - test_1: A single item is processed successfully.
 *  - test_2: Multiple queued items are all processed.
 *  - test_3: Items are processed sequentially, never concurrently.
 *  - test_4: Exceptions thrown during processing do not stop the worker.
 *  - test_5: Batch insertion processes every queued item.
 */
class LIFOQueueTest {

    private val context: Context = mockk(relaxed = true)
    private val appContext: Context = mockk(relaxed = true)

    init {
        every { context.applicationContext } returns appContext
    }

    /** test_1: A single item is processed successfully. */
    @Test
    fun test_1_singleItem_processed() {
        val processed = AtomicInteger(0)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)

        val queue = LIFOQueue<String>(
            scope = scope,
            initAwait = false,
            placementAwait = false
        ) { _, _ ->
            processed.incrementAndGet()
        }

        runBlocking {
            queue.add(context, "item1")
            delay(500)
        }

        assertEquals(1, processed.get())
        job.cancel()
    }

    /** test_2: Multiple queued items are all processed. */
    @Test
    fun test_2_multipleItems_allProcessed() {
        val processed = AtomicInteger(0)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)

        val queue = LIFOQueue<String>(
            scope = scope,
            initAwait = false,
            placementAwait = false
        ) { _, _ ->
            processed.incrementAndGet()
        }

        runBlocking {
            repeat(5) {
                queue.add(context, "item$it")
            }
            delay(500)
        }

        assertEquals(5, processed.get())
        job.cancel()
    }

    /** test_3: Items are processed sequentially, never concurrently. */
    @Test
    fun test_3_sequentialProcessing_oneAtATime() {
        val concurrentCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)

        val queue = LIFOQueue<Int>(
            scope = scope,
            initAwait = false,
            placementAwait = false
        ) { _, _ ->
            val current = concurrentCount.incrementAndGet()
            if (current > maxConcurrent.get()) {
                maxConcurrent.set(current)
            }
            delay(50)
            concurrentCount.decrementAndGet()
        }

        runBlocking {
            queue.add(context, 1)
            queue.add(context, 2)
            queue.add(context, 3)
            delay(500)
        }

        assertEquals("Items should be processed sequentially", 1, maxConcurrent.get())
        job.cancel()
    }

    /** test_4: Exceptions thrown during processing do not stop the worker. */
    @Test
    fun test_4_exceptionInProcess_doesNotCrash() {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)

        val queue = LIFOQueue<String>(
            scope = scope,
            initAwait = false,
            placementAwait = false
        ) { _, item ->
            if (item == "bad") throw RuntimeException("fail")
        }

        runBlocking {
            queue.add(context, "bad")
            delay(300)
        }

        job.cancel()
    }

    /** test_5: Batch insertion processes every queued item. */
    @Test
    fun test_5_batchAdd_allProcessed() {
        val processed = AtomicInteger(0)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)

        val queue = LIFOQueue<String>(
            scope = scope,
            initAwait = false,
            placementAwait = false
        ) { _, _ ->
            processed.incrementAndGet()
        }

        runBlocking {
            repeat(10) {
                queue.add(context, "item$it")
            }
            delay(500)
        }

        assertEquals(10, processed.get())
        job.cancel()
    }
}