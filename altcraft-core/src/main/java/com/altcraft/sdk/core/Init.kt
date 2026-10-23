package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.platform.authenticate.external.RTokenChange
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.util.log.Logger.loggingStatus
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.InitBarrier.initBarrierComplete
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.data.Constants.INIT_QUEUE
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.util.log.Log.event
import kotlin.coroutines.cancellation.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Entry point responsible for initializing the SDK.
 */
internal object Init {

    /**
     * Signals whether the initialization (retry) phase has already been
     * triggered in the current session.
     *
     * Reset via [AtomicBoolean.set] when SDK data is cleared
     * (see [ModuleRegistry.notifyInitialized]) so a fresh initialization
     * can start the retry phase again.
     */
    internal val initPhaseStarted = AtomicBoolean(
        false
    )

    /** Serial queue for SDK initialization tasks. */
    val initQueue = SerialCommandQueue(
        INIT_QUEUE
    )

    /**
     * Initializes the SDK with the provided context and configuration.
     *
     * Thread-safe initialization that:
     * - waits for the application to reach the foreground, then notifies
     *   core and registered modules that the SDK is initialized;
     * - validates and applies registered module configurations;
     * - first compares the previously stored RToken with the new one and
     *   notifies modules on change;
     * - applies the SDK configuration and configures global logging behavior;
     * - releases the initialization barrier;
     * - invokes the optional completion callback with [Result].
     *
     * Runs inside the serial [initQueue], so concurrent calls are processed
     * one after another. A module configuration error cancels neither core
     * initialization nor initialization of the remaining modules.
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

        loggingStatus = configuration.enableLogging

        initQueue.submit {
            try {
                ConfigManager.validateConfig(
                    context = appContext,
                    config = configuration
                )

                CoreEnvironmentProvider.initialize(
                    context = appContext
                )

                RTokenChange.notifyIfRTokenChanged(
                    context = appContext,
                    config = configuration
                )

                ConfigManager.setConfig(
                    context = appContext,
                    config = configuration
                )

                ModuleRegistry.configureModules(
                    context = appContext,
                    config = configuration
                )

                event(
                    "init",
                    CoreEvents.configIsSet
                )

                AuthenticateProcess.setAuth(
                    context = appContext
                )

                ModuleRegistry.notifyInitialized(
                    context = appContext
                )

                initFunctionComplete(
                    complete
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error("init", e)

                initFunctionComplete(complete, e)
            } finally {
                initBarrierComplete(reservedGate)
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
    } catch (e: Throwable) {
        error("init callback", e)
    }
}