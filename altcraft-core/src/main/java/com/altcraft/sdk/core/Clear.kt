package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.util.log.Log.event
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles SDK cleanup operations.
 */
internal object Clear {

    /**
     * Clears SDK data, stops active background work, and notifies completion.
     *
     * Fire-and-forget: cleanup is submitted to the serial init queue, so it
     * never interleaves with an in-flight or subsequent initialization.
     * Without this serialization a concurrent init could resurrect the
     * configuration and periodic workers after the wipe.
     *
     * The init barrier is reset before the wipe, so commands submitted
     * during or after cleanup wait for the next initialization instead of
     * acting on cleared storage.
     *
     * @param context Application context used for cleanup.
     * @param onComplete Optional callback invoked on the main thread
     * after cleanup completes.
     */
    fun clear(
        context: Context,
        onComplete: (() -> Unit)? = null
    ) {
        val applicationContext = context.applicationContext

        Init.initQueue.submit {
            try {
                InitBarrier.reset()

                ModuleRegistry.notifyClear(applicationContext)
            } catch (e: CancellationException) {
                error("clear", e)
                throw e
            } catch (e: Exception) {
                error("clear", e)
            }

            withContext(Dispatchers.Main) {
                event("clear", CoreEvents.sdkCleared)
                onComplete?.invoke()
            }
        }
    }
}
