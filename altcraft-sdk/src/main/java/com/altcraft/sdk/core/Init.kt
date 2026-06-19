package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger.loggingStatus
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup.setConfig
import com.altcraft.sdk.coordination.InitBarrier.initBarrierComplete
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.core.InitialOperations.performInitOperations
import com.altcraft.sdk.data.room.RoomRequest.roomOverflowControl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Entry point responsible for initializing the SDK.
 */
internal object Init {

    /** Prevents concurrent SDK initialization. */
    private val initMutex = Mutex()

    /**
     * Initializes the SDK with the provided context and configuration.
     *
     * Thread-safe initialization that:
     * - applies SDK configuration;
     * - configures global logging behavior;
     * - performs database overflow control;
     * - executes initialization operations;
     * - releases the initialization barrier;
     * - invokes the optional completion callback with [Result].
     *
     * @param context Context used for SDK initialization.
     * @param configuration SDK configuration.
     * @param complete Optional callback with initialization result.
     */
    fun init(
        context: Context,
        configuration: AltcraftConfiguration,
        complete: ((Result<Unit>) -> Unit)? = null
    ) {
        val reservedGate = InitBarrier.reserve()
        val appContext = context.applicationContext
        loggingStatus = configuration.getEnableLogging()
        CommandQueue.InitCommandQueue.submit {
            initMutex.withLock {
                try {
                    setConfig(appContext, configuration)
                        ?: exception(
                            configIsNotSet
                        )

                    roomOverflowControl(
                        Environment.create(
                            appContext
                        ).room
                    )

                    performInitOperations(appContext)
                    initBarrierComplete(reservedGate)
                    initFunctionComplete(complete)
                } catch (e: Exception) {
                    error("init", e)
                    initBarrierComplete(reservedGate)
                    initFunctionComplete(complete, e)
                }
            }
        }
    }

    /**
     * Safely invokes the initialization callback.
     *
     * Any exception thrown by client callback code is caught and logged.
     */
    private fun initFunctionComplete(
        complete: ((Result<Unit>) -> Unit)?, e: Throwable? = null
    ) = try {
        complete?.invoke(if (e == null) Result.success(Unit) else Result.failure(e))
    } catch (e: Exception) {
        error("init callback", e)
    }
}