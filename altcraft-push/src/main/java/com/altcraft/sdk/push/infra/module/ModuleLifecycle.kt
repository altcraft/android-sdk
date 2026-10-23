package com.altcraft.sdk.push.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.InitLauncher.launchInit
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.config.ConfigManager
import com.altcraft.sdk.push.data.Constants.MODULE_NAME
import com.altcraft.sdk.push.data.Constants.PUSH_P_WORK_TAG
import com.altcraft.sdk.push.data.Constants.WORKER_TAGS
import com.altcraft.sdk.push.data.Preferenses
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.platform.event.EventProcess
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.push.platform.token.TokenUpdateProcess
import com.altcraft.sdk.push.worker.PeriodicWorker
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.ensureWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.supervisorScope

/**
 * Controls the lifecycle of the Push module: initialization, cleanup,
 * and SDK RToken change handling.
 */
internal object ModuleLifecycle {

    /**
     * Initializes Push module operations.
     *
     * Independent initialization tasks are executed concurrently.
     * A failure in one task does not interrupt the remaining tasks.
     *
     * Old subscription and push event records are removed before
     * the corresponding retry operations are started.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context) {
        try {
            val previous = SubscriptionProcess.snapshotTaken
            if (!previous.isCompleted) previous.complete(Unit)
            SubscriptionProcess.snapshotTaken = CompletableDeferred()

            val environment = Environment.create(context)

            supervisorScope {
                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = {
                        ensureWorker<PeriodicWorker>(
                            it, PUSH_P_WORK_TAG
                        )
                    }
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = TokenUpdateProcess::pushTokenUpdate
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    cleanup = environment.room.dao.subscription::deleteOldest,
                    operation = SubscriptionProcess::isRetry
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    cleanup = environment.room.dao.event::deleteOldest,
                    operation = EventProcess::isRetry
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
     * Clears Push module data and stops its periodic worker.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context) {
        try {
            Preferenses.clearPreferences(context)
            cancelWorkerTask(context, WORKER_TAGS)
            Environment.create(context).room.clear()
            ConfigManager.clearConfigCache()
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
     * Clears pending subscriptions after an r-token change.
     *
     * @param context Application context.
     */
    suspend fun onSdkRTokenChanged(context: Context) {
        try {
            Environment.create(context)
                .room
                .dao
                .subscription
                .deleteAll()
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