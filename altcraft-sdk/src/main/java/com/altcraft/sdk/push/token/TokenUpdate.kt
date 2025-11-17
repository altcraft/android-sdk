package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.network.Request.tokenUpdateRequest
import com.altcraft.sdk.push.token.TokenManager.getCurrentPushToken
import com.altcraft.sdk.data.Preferenses.getSavedPushToken
import com.altcraft.sdk.data.Preferenses.setCurrentToken
import com.altcraft.sdk.data.error
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.sdk_events.EventList.configIsNull
import com.altcraft.sdk.sdk_events.EventList.pushTokenIsNull
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.services.manager.ServiceManager.startUpdateWorker
import com.altcraft.sdk.push.token.TokenManager.tokenLogShow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

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
    suspend fun tokenUpdate(context: Context) {
        tokenUpdateMutex.withLock {
            try {
                val config = getConfig(context) ?: exception(configIsNull)
                val currentToken = getCurrentPushToken(context) ?: exception(pushTokenIsNull)
                val savedToken = getSavedPushToken(context)

                if (savedToken == currentToken) return

                tokenLogShow = AtomicBoolean(false)

                when (isAppInForegrounded()) {
                    true -> startUpdateWorker(context, config)
                    else -> isRetry(context, UUID.randomUUID().toString())
                }
            } catch (e: Exception) {
                error("tokenUpdate", e)
            }
        }
    }

    /**
     * Checks whether the update process should be retried.
     *
     * - Extracts `requestId` from `inputData`.SubFunction.logger("update")
     * - Calls `updateProcess()` to process the update request.
     * - If `updateProcess()` throws an exception, it logs the error and returns `true`
     * - If `updateProcess()` returns `RetryError`, the function returns `true`.
     *
     * @return `true` if the update process should be retried, otherwise `false`.
     */
    suspend fun isRetry(
        context: Context,
        requestID: String
    ): Boolean {
        return try {
            updateProcessMutex.withLock {
                val token = getCurrentPushToken(context) ?: exception(pushTokenIsNull)

                val response = tokenUpdateRequest(context, requestID)

                if (response !is error) setCurrentToken(context, token)

                response is retry
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            retry("performTokenUpdate", e)
            true
        }
    }
}