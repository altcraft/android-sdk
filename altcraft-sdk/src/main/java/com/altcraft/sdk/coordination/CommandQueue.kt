package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import com.altcraft.sdk.sdk_events.Events.error

/**
 * CommandQueue — central entry point for sequential SDK task execution.
 *
 * Each nested queue:
 * - Accepts suspend tasks via submit
 * - Executes them sequentially (FIFO) on Dispatchers.IO
 * - Catches and logs errors without stopping the processing loop
 */
internal object CommandQueue {

    /**
     * Handles SDK initialization commands.
     * Ensures init operations are executed sequentially.
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

        /** Enqueues an initialization command for sequential execution. */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }

    /**
     * Handles subscription-related commands.
     * Prevents race conditions between subscription operations.
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

        /** Enqueues a subscription command for sequential execution. */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }

    /**
     * Handles mobile event commands.
     * Guarantees ordered delivery of mobile event tasks.
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

        /** Enqueues a mobile event command for sequential execution. */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }

    /**
     * Handles profile update commands.
     * Ensures profile mutations are processed strictly one-by-one.
     */
    object ProfileUpdateCommandQueue {
        private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private const val FUNC = "ProfileUpdateCommandQueue - init"

        init {
            CoroutineScope(Dispatchers.IO).launch {
                for (command in channel) {
                    runCatching { command() }.onFailure {
                        error(FUNC, it)
                    }
                }
            }
        }

        /** Enqueues a profile update command for sequential execution. */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(block)
        }
    }
}
