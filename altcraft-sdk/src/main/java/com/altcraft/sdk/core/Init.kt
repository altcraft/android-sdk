package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger.loggingStatus
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup.setConfig
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.core.Retry.performRetryOperations
import com.altcraft.sdk.data.room.RoomRequest.roomOverflowControl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Entry point responsible for initializing the SDK.
 *
 */
internal object Init {
    private val initMutex = Mutex()

    /**
     * Initializes the SDK with the provided context and configuration.
     *
     * Thread-safe, run-once initialization. After the configuration is applied, this method
     * calls `performRetryOperations(appCtx)`, which starts verification and processing of all
     * pending operations:
     * - sending mobile events;
     * - sending push events;
     * - handling push subscriptions;
     * - verifying and updating the device push token.
     *
     * During initialization the global logging behavior is configured based on
     * the `enableLogging` flag from [AltcraftConfiguration].
     *
     * @param context The application context used for configuration and internal setup.
     * @param configuration The SDK configuration required for initialization.
     * @param complete Optional callback that receives the result of the initialization.
     */
    fun init(
        context: Context,
        configuration: AltcraftConfiguration,
        complete: ((Result<Unit>) -> Unit)? = null
    ) {
        val context = context.applicationContext
        val reservedGate = InitBarrier.reserve()
        loggingStatus = configuration.getEnableLogging()
        CommandQueue.InitCommandQueue.submit {
            initMutex.withLock {
                try {
                    setConfig(context, configuration) ?: exception(configIsNotSet)
                    roomOverflowControl(Environment.create(context).room)
                    performRetryOperations(context)
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