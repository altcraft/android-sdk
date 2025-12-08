package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.network.Request.tokenUpdateRequest
import com.altcraft.sdk.data.Preferenses.setCurrentToken
import com.altcraft.sdk.data.error
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.services.manager.ServiceManager.startUpdateWorker
import com.altcraft.sdk.push.token.TokenManager.tokenLogShow
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Contains the functions necessary to control the updating of the device
 * token and transfer the new value to the server in case of a change.
 */
internal object TokenUpdate {

    private val tokenUpdateMutex = Mutex()
    private val updateProcessMutex = Mutex()

    /**
     * Initiates the token update process.
     *
     * This function determines whether the device token needs to be updated and initiates the
     * update process.
     *
     * @param context The application context used for retrieving configurations and managing
     * token storage.
     */
    suspend fun pushTokenUpdate(context: Context) {
        tokenUpdateMutex.withLock {
            try {
                val env = Environment.create(context)

                if (env.savedToken == env.token()) return

                tokenLogShow = AtomicBoolean(false)

                if (!isAppInForegrounded()) isRetry(context)
                else startUpdateWorker(context, env.config())
            } catch (e: Exception) {
                error("tokenUpdate", e)
            }
        }
    }

    /**
     * Runs the push token update logic and indicates whether a retry is required.
     *
     * @param context Application context used for environment and request execution.
     * @param workerId Optional identifier of the current work request.
     *
     * @return `true` if the push/update request should be retried, otherwise `false`.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null): Boolean {
        return try {
            updateProcessMutex.withLock { logic(context, workerId) }
        } catch (e: Exception) {
            retry("isRetry :: pushTokenUpdate", e); true
        }
    }

    /**
     * Runs push-token update logic and decides if a retry is needed.
     *
     * - Sends a push-token update request.
     * - On non-error response, saves the latest token.
     * - Returns `true` only if the response is `retry` and no newer work exists.
     *
     * @param context Application context for request and storage access.
     * @param id Optional work identifier.
     * @return `true` if the update should be retried; `false` otherwise.
     */
    private suspend fun logic(context: Context, id: UUID?): Boolean {
        val env = Environment.create(context)

        val response = tokenUpdateRequest(context, id.toString())

        if (response !is error) setCurrentToken(context, env.token())
        return response is retry && !hasNewRequest(
            context, UPDATE_C_WORK_TAG, id
        )
    }
}