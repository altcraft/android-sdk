package com.altcraft.sdk.target.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.target.infra.Environment
import com.altcraft.sdk.target.platform.target.TargetProcess
import com.altcraft.sdk.target.data.Constants.TARGET_P_WORK_TAG
import com.altcraft.sdk.target.data.Constants.WORKER_TAGS
import com.altcraft.sdk.target.worker.PeriodicWorker
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.ensureWorker
import com.altcraft.sdk.target.util.Log.error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Controls the lifecycle of the target module: initialization, cleanup,
 * and SDK rToken change handling.
 */
internal object ModuleLifecycle {

    /**
     * Performs target-module startup operations after core initialization.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context) = try {
        val previous = TargetProcess.snapshotTaken
        if (!previous.isCompleted) previous.complete(Unit)
        TargetProcess.snapshotTaken = CompletableDeferred()
        ensureWorker<PeriodicWorker>(context, TARGET_P_WORK_TAG)
        Environment.create(context).room.dao().deleteOldest()
        TargetProcess.isRetry(context)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        error(
            "onSdkInitialized",
            e
        )
    }

    /**
     * Stops target-module work and removes target-module persisted data.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context) {
        try {
            cancelWorkerTask(context, WORKER_TAGS)
            Environment.create(context).room.clear()
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
     * Handles an SDK rToken change.
     *
     * @param context Application context.
     */
    fun onSdkRTokenChanged(context: Context) = try {
        Environment.create(context).room.clear()
    } catch (e: Exception) {
        error(
            "onSdkRTokenChanged",
            e
        )
    }
}