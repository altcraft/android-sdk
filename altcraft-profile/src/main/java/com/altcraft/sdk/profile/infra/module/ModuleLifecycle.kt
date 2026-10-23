package com.altcraft.sdk.profile.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.profile.util.Log.error
import com.altcraft.sdk.profile.data.Constants.PROFILE_P_WORK_TAG
import com.altcraft.sdk.profile.data.Constants.WORKER_TAGS
import com.altcraft.sdk.profile.infra.Environment
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess
import com.altcraft.sdk.profile.worker.PeriodicWorker
import com.altcraft.sdk.workers.CancelWork.cancelWorkerTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.ensureWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Controls the lifecycle of the profile module: initialization, cleanup,
 * and SDK RToken change handling.
 */
internal object ModuleLifecycle {

    /**
     * Performs profile-module startup operations after core initialization.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context) = try {
        val previous = UpdateFieldsProcess.snapshotTaken
        if (!previous.isCompleted) previous.complete(Unit)
        UpdateFieldsProcess.snapshotTaken = CompletableDeferred()
        ensureWorker<PeriodicWorker>(context, PROFILE_P_WORK_TAG)
        Environment.create(context).room.dao().deleteOldest()
        UpdateFieldsProcess.isRetry(context)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        error(
            "onSdkInitialized",
            e
        )
    }

    /**
     * Stops profile-module work and removes profile-module persisted data.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context) {
        try {
            cancelWorkerTask(
                context, WORKER_TAGS
            )
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
     * Handles an SDK RToken change.
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
