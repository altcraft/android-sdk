package com.altcraft.sdk.core.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.coordination.InitLauncher.launchInit
import com.altcraft.sdk.core.Init.initPhaseStarted
import com.altcraft.sdk.data.Constants.CORE_P_WORK_TAG
import com.altcraft.sdk.data.Constants.MODULE_NAME
import com.altcraft.sdk.data.Constants.WORKER_TAGS
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.ensureWorker
import com.altcraft.sdk.workers.periodical.Worker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.supervisorScope

/**
 * Controls the lifecycle of the core module: initialization, cleanup,
 * and SDK RToken change handling.
 */
internal object ModuleLifecycle {

    /**
     * Initializes core module operations.
     *
     * Independent initialization tasks are executed concurrently.
     * A failure in one task does not interrupt the remaining tasks.
     *
     * Old auth request records are removed before the corresponding
     * retry operation is started.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context) {
        try {
            supervisorScope {
                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = {
                        ensureWorker<Worker.PeriodicWorker>(
                            it, CORE_P_WORK_TAG
                        )
                    }
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = AuthenticateProcess::isRetry
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(
                "onSdkInitialized",
                e
            )
        }
    }

    /**
     * Clears core module data and stops active SDK background work.
     *
     * - Clears the cached SDK configuration.
     * - Resets the init control so the next init can run again.
     * - Cancels background workers and services.
     * - Removes data from all core SDK tables.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context) {
        try {
            initPhaseStarted.set(false)
            cancelWorkerTask(context, WORKER_TAGS)
            ConfigManager.clearConfigCache()
            DB.getDb(context).clear()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(
                "onSdkClear",
                e
            )
        }
    }

    /**
     * Removes all pending auth requests after an r-token change.
     *
     * @param context Application context.
     */
    suspend fun onSdkRTokenChanged(context: Context) {
        try {
            AuthenticateProcess.logOut(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(
                "onSdkRTokenChanged",
                e
            )
        }
    }
}