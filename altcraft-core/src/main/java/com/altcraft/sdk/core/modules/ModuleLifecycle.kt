package com.altcraft.sdk.core.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.coordination.InitLauncher.launchInit
import com.altcraft.sdk.core.Init.initPhaseStarted
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.Constants.CORE_P_WORK_TAG
import com.altcraft.sdk.data.Constants.MODULE_NAME
import com.altcraft.sdk.data.Constants.ONE_TIME_WORKER_TAGS
import com.altcraft.sdk.data.Constants.WORKER_TAGS
import com.altcraft.sdk.data.Preferences.clearCurrentRToken
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
            clearCurrentRToken(context)
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
     * Handles an SDK RToken change in the core module.
     *
     * Cancels one-time workers, removes all pending auth requests,
     * and restores the anonymous session.
     *
     * Restores the anonymous session without entering the AuthenticateProcess
     * mutex: this handler is dispatched from the RToken change notification
     * chain, which can run while an authentication flow already holds the
     * mutex, and re-entering it would deadlock the notification.
     *
     * @param context Application context.
     */
    suspend fun onSdkRTokenChanged(context: Context) {
        try {
            cancelWorkerTask(
                context, ONE_TIME_WORKER_TAGS
            )
            Environment.create(context)
                .room.authRequestDao()
                .deleteAll()
            AuthenticateProcess.logOutUnlocked(
                context
            )
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