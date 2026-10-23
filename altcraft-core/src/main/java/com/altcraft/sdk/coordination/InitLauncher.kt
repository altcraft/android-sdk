package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log.error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launches SDK-lifecycle initialization operations.
 *
 * This helper is public so that optional SDK modules can launch their
 * initialization operations from a single shared implementation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InitLauncher {

    /**
     * Launches an initialization operation.
     *
     * The optional cleanup operation completes before the main operation starts.
     *
     * @param context Application context.
     * @param moduleName Name of the module that owns the operation, used for logging.
     * @param cleanup Optional cleanup operation.
     * @param operation Initialization operation.
     */
    fun CoroutineScope.launchInit(
        context: Context,
        moduleName: String,
        cleanup: (suspend () -> Unit?)? = null,
        operation: suspend (Context) -> Any?
    ) = launch {
        try {
            cleanup?.invoke()
            operation(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(
                "onSdkInitialized: $moduleName",
                e
            )
        }
    }
}