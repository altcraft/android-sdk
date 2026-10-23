package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log.error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * CommandQueue — central entry point for sequential SDK task execution.
 *
 * Each queue processes its own commands sequentially and independently
 * of other SDK command queues.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CommandQueue {

    /**
     * A per-function command queue that runs submitted commands sequentially.
     */
    class SerialCommandQueue(
        private val func: String
    ) {
        private val channel = Channel<suspend () -> Unit>(
            Channel.UNLIMITED
        )

        init {
            SDKScope.io.launch {
                for (command in channel) {
                    try {
                        command()
                    } catch (e: CancellationException) {
                        if (!isActive) throw e
                        error(func, e)
                    } catch (e: Throwable) {
                        error(func, e)
                    }
                }
            }
        }

        /**
         * Enqueues a command for sequential execution.
         */
        fun submit(block: suspend () -> Unit) {
            channel.trySend(
                block
            )
        }
    }
}