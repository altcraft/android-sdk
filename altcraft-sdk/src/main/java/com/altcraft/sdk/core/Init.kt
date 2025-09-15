package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup.setConfig
import com.altcraft.sdk.events.EventList.configIsNotSet
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.push.Core.performPushModuleCheck
import com.altcraft.sdk.push.Core.pushModuleIsActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Entry point responsible for initializing the SDK.
 *
 */
object Init {

    private val initMutex = Mutex()

    /**
     * Initializes the SDK with the provided context and configuration.
     *
     * Thread-safe, run-once initialization. After the configuration is applied, this method
     * calls `performPushModuleCheck(appCtx)`, which starts verification and processing of all
     * pending push-related operations:
     * - handling push subscriptions;
     * - verifying and updating the device push token;
     * - sending push events.
     *
     * @param context The application context used for configuration and internal setup.
     * @param configuration The SDK configuration required for initialization.
     * @param complete Optional callback that receives the result of the initialization.
     */
    internal fun init(
        context: Context,
        configuration: AltcraftConfiguration,
        complete: ((Result<Unit>) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext
        val reservedGate = InitBarrier.reserve()

        CommandQueue.InitCommandQueue.submit {
            initMutex.withLock {
                try {
                    setConfig(context, configuration) ?: exception(configIsNotSet)
                    if (pushModuleIsActive(appCtx)) performPushModuleCheck(appCtx)

                    InitBarrier.complete(reservedGate)
                    complete?.invoke(Result.success(Unit))
                } catch (e: Exception) {
                    InitBarrier.fail(reservedGate, e)
                    error("init", e)
                    complete?.invoke(Result.failure(e))
                }
            }
        }
    }
}

