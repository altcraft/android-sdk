package com.altcraft.sdk.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import com.altcraft.sdk.sdk_events.Events.error

/**
 * CommandQueue — central mechanism for sequential execution of SDK tasks.
 *
 * Provides two independent queues:
 * - InitCommandQueue — handles SDK initialization commands.
 * - SubscribeCommandQueue — handles subscription commands.
 * - MobileEventCommandQueue - handles mobile event commands.
 *
 * Each queue runs on Dispatchers.IO, processing suspend functions in FIFO order.
 * Errors are caught, logged, and do not stop the loop.
 */
internal object CommandQueue {

    /**
     * Serial command queue for SDK init tasks.
     *
     * Executes submitted suspendable commands **sequentially** on `Dispatchers.IO`.
     * Failures are caught and logged via `error(FUNC, throwable)`; the loop continues.
     */
     object InitCommandQueue {
        private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private const val FUNC = "InitCommandQueue - init"

        init {
            CoroutineScope(Dispatchers.IO).launch {
                for (command in channel) {
                    runCatching { command() }.onFailure {
                        error(FUNC, it)
                    }
                }
            }
        }

        /**
         * Enqueues a suspendable init command to be executed on the main thread (FIFO).
         *
         * @param block The suspend function to run.
         */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }

    /**
     * Serializes execution of suspend commands in a single coroutine on the IO dispatcher.
     *
     * Tasks submitted via [submit] are executed one-by-one in the order of arrival.
     */
     object SubscribeCommandQueue {
        private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private const val FUNC = "SubscribeCommandQueue - init"

        init {
            CoroutineScope(Dispatchers.IO).launch {
                for (command in channel) {
                    runCatching { command() }.onFailure {
                        error(FUNC, it)
                    }
                }
            }
        }

        /**
         * Enqueues a suspendable init command to be executed on the main thread (FIFO).
         *
         * @param block The suspend function to run.
         */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }

    /**
     * Serializes execution of suspend commands in a single coroutine on the IO dispatcher.
     *
     * Tasks submitted via [submit] are executed one-by-one in the order of arrival.
     */
    object MobileEventCommandQueue {
        private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private const val FUNC = "MobileEventCommandQueue - init"

        init {
            CoroutineScope(Dispatchers.IO).launch {
                for (command in channel) {
                    runCatching { command() }.onFailure {
                        error(FUNC, it)
                    }
                }
            }
        }

        /**
         * Enqueues a suspendable init command to be executed on the main thread (FIFO).
         *
         * @param block The suspend function to run.
         */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }
}