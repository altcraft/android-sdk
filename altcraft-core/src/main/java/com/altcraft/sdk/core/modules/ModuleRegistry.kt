package com.altcraft.sdk.core.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.coordination.ForegroundBarrier.foreground
import com.altcraft.sdk.coordination.SDKScope
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.data.Constants.RETRY_START_TIMEOUT
import com.altcraft.sdk.util.log.Log.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Volatile
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores optional Altcraft SDK modules and dispatches SDK lifecycle events.
 *
 * Core SDK does not depend on concrete module implementations.
 * Each optional module registers itself during application startup.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ModuleRegistry {

    /**
     * Registered modules indexed by their unique identifiers.
     */
    private val modules = ConcurrentHashMap<String, AltcraftModule>()

    /**
     * Module identifiers that passed configuration validation
     * and received their configuration during SDK initialization.
     *
     * Only these modules receive the [AltcraftModule.onSdkInitialized]
     * callback. An error in one module does not cancel initialization
     * of the remaining registered modules.
     */
    private val readyModules = ConcurrentHashMap.newKeySet<String>()

    /**
     * Signals that the retry phase is allowed to start for the current
     * initialization session. Recreated on every SDK re-initialization.
     */
    @Volatile
    internal var retryStartReady = CompletableDeferred<Unit>()

    /**
     * Active protective timeout tasks keyed by their module snapshot signal.
     * One timer per snapshot so concurrent commands do not spawn duplicates.
     */
    internal val retryFallbacks = ConcurrentHashMap<CompletableDeferred<Unit>, Job>()

    /**
     * Registers an optional SDK module.
     *
     * If a module with the same identifier is already registered,
     * its implementation is replaced.
     *
     * @param module Module implementation to register.
     */
    fun register(module: AltcraftModule) {
        modules[module.moduleId] = module
    }

    /**
     * Removes an optional SDK module from the registry.
     *
     * @param moduleId Unique identifier of the module.
     */
    internal fun unregister(moduleId: String) {
        modules.remove(moduleId)
        readyModules.remove(moduleId)
    }

    /**
     * Validates and persists the configurations of all registered modules.
     *
     * For every registered module:
     * - its configuration is resolved from the provided SDK configuration;
     * - the module validates the resolved configuration;
     * - the module persists and applies the configuration.
     *
     * Modules are processed independently. An error in one module is logged
     * and does not prevent configuration of the remaining modules.
     * Modules that failed are not included in the ready set and therefore
     * do not receive the [AltcraftModule.onSdkInitialized] callback.
     *
     * @param context Application context.
     * @param config SDK configuration containing module configurations.
     */
    internal suspend fun configureModules(
        context: Context,
        config: AltcraftConfiguration
    ) {
        readyModules.clear()

        for (module in modules.values) {
            try {
                val moduleConfiguration =
                    config.getModuleConfiguration(
                        module.moduleId
                    )

                moduleConfiguration?.let {
                    module.validate(
                        context, it
                    )

                    module.configure(
                        context, it
                    )
                }

                readyModules.add(module.moduleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(
                    "configureModule: ${module.moduleId}",
                    e
                )
            }
        }
    }

    /**
     * Notifies Altcraft SDK that core has been initialized successfully.
     *
     * Runs only when the app is in foreground and executes initialization
     * of core and ready modules only once per process. On a fresh init
     * (init gate passed) a new retry-start signal is created so awaiting
     * request queues of the new session wait for the actual retry phase.
     * If the init gate has already been triggered, the retry-start signal
     * is still completed so that awaiting request queues do not hang.
     *
     * @param context Application context.
     */
    internal fun notifyInitialized(context: Context) = foreground {
        if (it && Init.initPhaseStarted.compareAndSet(
                false, true
            )
        ) {
            val previous = retryStartReady
            if (!previous.isCompleted) previous.complete(Unit)

            retryFallbacks.keys.forEach { snapshot ->
                if (!snapshot.isCompleted) snapshot.complete(Unit)
                retryFallbacks[snapshot]?.cancel()
            }
            retryFallbacks.clear()
            retryStartReady = CompletableDeferred()
            notifyInitializedInternal(context)
        }
        retryStartReady.complete(Unit)
    }

    /**
     * Executes core and module initialization without the foreground gate.
     *
     * Notifies core lifecycle and all modules whose configuration was
     * successfully applied that Altcraft SDK core has been initialized.
     * An error in one module does not prevent initialization
     * of the remaining ready modules.
     *
     * @param context Application context.
     */
    internal suspend fun notifyInitializedInternal(context: Context) {
        ModuleLifecycle.onSdkInitialized(context)

        for (moduleId in readyModules) {
            try {
                modules[moduleId]?.onSdkInitialized(
                    context
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(
                    "onSdkInitialized: $moduleId",
                    e
                )
            }
        }
    }

    /**
     * Suspends until retry scheduling has started and
     * the initial database snapshot has been taken.
     *
     * A fallback timeout force-completes both signals after
     * [RETRY_START_TIMEOUT] to prevent indefinite waiting.
     *
     * @param snapshotTaken snapshot completion signal for a specific entity type.
     */
    @PublishedApi
    internal suspend fun awaitRetryStarted(snapshotTaken: CompletableDeferred<Unit>) {
        if (snapshotTaken.isCompleted) return

        scheduleRetryStartFallback(snapshotTaken)

        retryStartReady.run {
            if (!isCompleted) try {
                withTimeout(RETRY_START_TIMEOUT) {
                    await()
                }
            } catch (_: TimeoutCancellationException) {
                complete(Unit)
            }
        }

        snapshotTaken.run {
            if (!isCompleted) try {
                withTimeout(RETRY_START_TIMEOUT) {
                    await()
                }
            } catch (_: TimeoutCancellationException) {
                complete(Unit)
            }
        }
    }

    /**
     * Starts a single protective timeout for retry-start awaiting per snapshot.
     *
     * If a timer for the given snapshot is already active, does nothing.
     * On firing, force-completes both the common retry-start signal and
     * the provided snapshot signal so that awaiting queues never hang.
     *
     * @param snapshotTaken snapshot completion signal for a specific entity type.
     */
    internal fun scheduleRetryStartFallback(snapshotTaken: CompletableDeferred<Unit>) {
        if (retryFallbacks.containsKey(snapshotTaken)) return

        val job = SDKScope.default.launch {
            delay(RETRY_START_TIMEOUT)
            retryStartReady.complete(Unit)
            snapshotTaken.complete(Unit)
            retryFallbacks.remove(snapshotTaken)
        }

        retryFallbacks[snapshotTaken] = job
    }

    /**
     * Notifies the core and all registered modules that Altcraft SDK data
     * is being cleared.
     *
     * An error in one module does not prevent cleanup
     * of the remaining registered modules.
     *
     * @param context Application context.
     */
    internal suspend fun notifyClear(context: Context) {
        try {
            ModuleLifecycle.onSdkClear(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(
                "onSdkClear: core",
                e
            )
        }

        for (module in modules.values) {
            try {
                module.onSdkClear(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(
                    "onSdkClear: ${module.moduleId}",
                    e
                )
            }
        }
    }

    /**
     * Notifies the core and all registered modules that the SDK RToken
     * has changed.
     *
     * An error in one module does not prevent notification
     * of the remaining registered modules.
     *
     * @param context Application context.
     */
    internal suspend fun notifyRTokenChanged(context: Context) {
        ModuleLifecycle.onSdkRTokenChanged(context)

        for (module in modules.values) {
            try {
                module.onSdkRTokenChanged(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error(
                    "onSdkRTokenChanged: ${module.moduleId}",
                    e
                )
            }
        }
    }
}