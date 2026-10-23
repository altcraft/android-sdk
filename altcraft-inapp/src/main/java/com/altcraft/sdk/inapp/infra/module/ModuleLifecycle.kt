package com.altcraft.sdk.inapp.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.InitLauncher.launchInit
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.config.ConfigManager
import com.altcraft.sdk.inapp.data.Constants.IN_APP_P_WORK_TAG
import com.altcraft.sdk.inapp.data.Constants.MODULE_NAME
import com.altcraft.sdk.inapp.data.Constants.WORKER_TAGS
import com.altcraft.sdk.inapp.data.Preferenses
import com.altcraft.sdk.inapp.platform.event.EventProcess
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import com.altcraft.sdk.inapp.system.presentation.PresentationGate
import com.altcraft.sdk.inapp.worker.PeriodicWorker
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.ensureWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.supervisorScope

/**
 * Controls the lifecycle of the in-app module: initialization, cleanup,
 * and SDK RToken change handling.
 *
 * State cleanup operations are executed before the corresponding retry flows.
 */
internal object ModuleLifecycle {

    /**
     * Initializes in-app module operations.
     *
     * Independent initialization tasks are executed concurrently:
     * - ensures that required periodic workers are scheduled;
     * - processes pending subscription retries;
     * - processes pending in-app event retries;
     * - requests actual in-app placements.
     *
     * A failure in one task does not interrupt the remaining tasks.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context) {
        try {
            val previous = SubscriptionProcess.snapshotTaken
            if (!previous.isCompleted) previous.complete(Unit)
            SubscriptionProcess.snapshotTaken = CompletableDeferred()

            val env = Environment.create(context)

            supervisorScope {
                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = {
                        ensureWorker<PeriodicWorker>(
                            it, IN_APP_P_WORK_TAG
                        )
                    }
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    cleanup = env.room.dao.subscribe::deleteOldest,
                    operation = SubscriptionProcess::isRetry
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    cleanup = env.room.dao.event::deleteOldest,
                    operation = EventProcess::isRetry
                )

                launchInit(
                    context = context,
                    moduleName = MODULE_NAME,
                    operation = PlacementsProcess::requestPlacements
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
     * Clears in-app module data and stops its workers.
     *
     * Pending presentations (delayed tasks and presentations waiting on
     * the presentation mutex or loading content) are invalidated first,
     * so stale campaigns cannot be shown after the clear.
     *
     * The placements readiness gate is re-armed so triggers scheduled after
     * the clear wait for the next placements request instead of passing
     * with the just-wiped local data.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context) {
        try {
            PresentationGate.invalidate()
            Preferenses.clearPreferences(context)
            cancelWorkerTask(context, WORKER_TAGS)
            Environment.create(context).room.clear()
            ConfigManager.clearCache()
            InAppVariables.reset()
            PlacementsProcess.reset()
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
                .subscribe
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