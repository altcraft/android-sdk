package com.altcraft.sdk.inapp.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.coordination.withInitReady
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/**
 * Thread-safe LIFO queue that processes items sequentially in a coroutine worker.
 *
 * If [initAwait] is `true`, processing starts only after SDK initialization is ready.
 * If [placementAwait] is `true`, processing starts only after in-app placements are ready.
 */
internal class LIFOQueue<T>(
    private val scope: CoroutineScope,
    private val func: String = "LIFOQueue",
    private val initAwait: Boolean = true,
    private val placementAwait: Boolean = true,
    private val process: suspend (Context, T) -> Unit
) {

    /** Mutex protecting queue state and worker flags. */
    private val mutex = Mutex()

    /** Internal stack used for LIFO item processing. */
    private val stack = ArrayDeque<QueueItem<T>>(MAX_SIZE)

    /** Worker job responsible for sequential queue processing. */
    private var workerJob: Job? = null

    /** Indicates whether the worker is currently running. */
    private var workerRunning = false

    /**
     * Adds an item to the queue and starts the worker if needed.
     *
     * @param context Application context associated with the item.
     * @param item Item to enqueue.
     */
    suspend fun add(context: Context, item: T) {

        val shouldStartWorker = mutex.withLock {
            stack.addFirst(
                QueueItem(
                    context = context.applicationContext,
                    item = item
                )
            )

            if (stack.size > MAX_SIZE) {
                stack.removeLast()
            }

            if (!workerRunning) {
                workerRunning = true
                true
            } else {
                false
            }
        }

        if (shouldStartWorker) {
            startWorker()
        }
    }

    /**
     * Starts the queue worker if the processing scope is active.
     */
    private fun startWorker() {
        if (!scope.isActive) return

        val initGate = InitBarrier.current()

        workerJob = scope.launch {
            try {
                val action: suspend () -> Unit = {
                    if (placementAwait) {
                        PlacementsProcess.placementsReady.await()
                    }

                    while (isActive) {
                        val next: QueueItem<T>? = mutex.withLock {
                            if (stack.isEmpty()) null else stack.removeFirst()
                        }

                        if (next == null) break

                        try {
                            process(next.context, next.item)
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                           error(func, t)
                        }
                    }
                }

                if (initAwait) {
                    withInitReady(null, gate = initGate) {
                        action()
                    }
                } else {
                    action()
                }
            } finally {
                val shouldRestart = mutex.withLock {
                    workerRunning = false

                    if (stack.isNotEmpty() && scope.isActive) {
                        workerRunning = true
                        true
                    } else {
                        false
                    }
                }

                if (shouldRestart) {
                    startWorker()
                }
            }
        }
    }

    /**
     * Queue entry containing an item and its associated context.
     *
     * @param context Application context associated with the queued item.
     * @param item Queued item.
     */
    private data class QueueItem<T>(
        val context: Context,
        val item: T
    )

    private companion object {
        /** Maximum number of items stored in the queue. */
        private const val MAX_SIZE = 50
    }
}