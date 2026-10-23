package com.altcraft.sdk.push.platform.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import com.altcraft.sdk.data.models.error
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants.TOKEN_UPDATE_O_WORK_TAG
import com.altcraft.sdk.push.data.Preferenses.setCurrentToken
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.platform.token.request.UpdateRequest.tokenUpdateRequest
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.util.Log.retry
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.hasRunningWorker
import com.altcraft.sdk.workers.onetime.Request.startWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Contains the functions necessary to control the updating of the device
 * token and transfer the new value to the server in case of a change.
 */
internal object TokenUpdateProcess {

    @Volatile
    private var updateLock: Boolean = false
    private val tokenUpdateMutex = Mutex()
    private val updateProcessMutex = Mutex()

    /**
     * Ensures the push token is synchronized before processing subscriptions when
     * RToken authentication is used.
     *
     * If there are pending entities and the token cannot be updated, throws
     * an SDK exception with [ModuleEvents.notUpdated] and cancels the current
     * subscription flow.
     *
     * @param context Application context.
     * @param items Pending push-subscription entities.
     * @param env Current SDK environment.
     * @return `null` if processing can continue.
     */
    suspend fun checkUpdateFromRtoken(
        context: Context,
        items: List<PushSubscribeEntity>,
        env: Environment
    ): Nothing? {
        return if (
            items.isNotEmpty()
            && env.usesRTokenAuth()
            && !pushTokenUpdate(context)
        ) exception(ModuleEvents.notUpdated)
        else null
    }

    /**
     * Initiates the token update process.
     *
     * This function determines whether the device token needs to be updated and initiates the
     * update process.
     *
     * A stale lock (the retry worker exhausted its budget without a final
     * [isRetry] call) is detected by the absence of an active token-update
     * worker and cleared, so the update process is never blocked for the
     * rest of the foreground session.
     *
     * @param context The application context used for retrieving configurations and managing
     * token storage.
     * @return `true` if an update is not required or already completed; `false` otherwise.
     */
    suspend fun pushTokenUpdate(context: Context): Boolean = tokenUpdateMutex.withLock {
        return try {
            if (updateLock) {
                if (hasRunningWorker(context, TOKEN_UPDATE_O_WORK_TAG)) return false
                updateLock = false
            }
            val env = Environment.create(context)
            val currentToken = env.currentToken()
            val savedToken = env.savedToken()

            when {
                savedToken == null || savedToken == currentToken || !isRetry(context)
                    -> return true

                else -> if (isAppInForegrounded()) startWorker<UpdateWorker>(
                    context, TOKEN_UPDATE_O_WORK_TAG
                )
            }
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("tokenUpdate", e)
            false
        }
    }

    /**
     * Executes token update logic and determines whether a retry is needed.
     *
     * The lock is held while the update cycle is pending (retry response)
     * and released when the cycle completes or fails, so a later attempt
     * (worker backoff, next subscription cycle) can always recover.
     *
     * @param context Application context.
     * @param workerId Optional work identifier.
     * @return `true` if another retry is required; `false` otherwise.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        updateLock = true

        updateProcessMutex.withLock { logic(context, workerId) }
    } catch (e: CancellationException) {
        updateLock = false
        throw e
    } catch (e: Exception) {
        updateLock = false
        retry("isRetry :: pushTokenUpdate", e)
        true
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
        val tag = TOKEN_UPDATE_O_WORK_TAG
        val token = Environment.create(context).currentToken()

        val response = tokenUpdateRequest(context, id?.toString())

        if (response !is retry) updateLock = false
        if (response !is error) setCurrentToken(context, token)
        return response is retry && !hasNewRequest(context, tag, id)
    }
}
