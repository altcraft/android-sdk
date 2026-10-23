package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import com.altcraft.sdk.core.modules.ModuleRegistry.awaitRetryStarted
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.util.log.Log.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Enqueues an SDK command for sequential execution.
 *
 * If [initAwait] is `true`, waits until SDK initialization is ready before execution.
 * If [snapshotTaken] is not `null`, waits until retry startup is triggered.
 * Then acquires the provided mutex, verifies the connection,
 * and executes the given block.
 *
 * @param context Application context used to access a temporary core
 * environment for connection verification.
 * @param func Logical source tag used for command queue and error event logging.
 * @param mutex Serializes the block execution; the block runs while the mutex is held.
 * @param queue Queue on which the command is submitted for sequential processing.
 * @param snapshotTaken Optional database snapshot signal; when not `null`,
 * the command waits until the initial retry startup is triggered.
 * @param initAwait When `true`, waits until SDK initialization is ready
 * before executing the block.
 * @param block Operation that is executed after all the above preconditions are met.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun submitSdkCommand(
    context: Context,
    func: String,
    mutex: Mutex,
    queue: CommandQueue.SerialCommandQueue,
    snapshotTaken: CompletableDeferred<Unit>?,
    initAwait: Boolean = true,
    crossinline block: suspend () -> Unit
) {
    val initGate = InitBarrier.current()

    queue.submit {
        try {
            val action: suspend () -> Unit = {
                if (snapshotTaken != null) awaitRetryStarted(
                    snapshotTaken
                )

                mutex.withLock {
                    if (!SubFunction.isOnline(
                            context.applicationContext
                        )
                    ) error(
                        func,
                        CoreEvents.noInternetConnect
                    )
                    block()
                }
            }

            if (initAwait) withInitReady(
                func,
                gate = initGate
            ) {
                action()
            } else {
                action()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(func, e)
        }
    }
}