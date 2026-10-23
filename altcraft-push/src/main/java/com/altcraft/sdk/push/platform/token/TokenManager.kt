package com.altcraft.sdk.push.platform.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.SuspendLazy
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.util.Log.event
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.config.ConfigManager
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.push.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.push.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.push.data.Constants.validProviders
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.events.ModuleEvents.invalidProvider
import com.altcraft.sdk.push.platform.token.TokenUpdateProcess.pushTokenUpdate
import com.altcraft.sdk.push.platform.token.provider.FCMInterface
import com.altcraft.sdk.push.platform.token.provider.HMSInterface
import com.altcraft.sdk.push.platform.token.provider.RustoreInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Handles push token retrieval, selection, deletion, and update operations.
 *
 * Resolves tokens from available push providers according to the configured
 * provider priority list or uses a manually assigned token when no provider
 * integration is available.
 */
internal object TokenManager {
    /** Registered Firebase Cloud Messaging provider. */
    @Volatile
    internal var fcmProvider: FCMInterface? = null

    /** Registered Huawei Mobile Services provider. */
    @Volatile
    internal var hmsProvider: HMSInterface? = null

    /** Registered RuStore push provider. */
    @Volatile
    internal var rustoreProvider: RustoreInterface? = null

    /** Synchronizes push provider event logging. */
    private val tokenLogMutex = Mutex()

    /** Last token reported through the provider setup event. */
    private var lastReportedToken: TokenData? = null

    /** Maximum wait for a provider delete callback before giving up. */
    private val deleteTokenTimeout = 10.seconds

    /**
     * Validates that all provider identifiers are supported.
     *
     * @param providers Provider identifiers to validate.
     * @return `true` if every provider is supported.
     */
    fun providersValid(providers: List<String>) = providers.all {
        it in validProviders
    }

    /**
     * Returns the current device push token.
     *
     * When no provider is registered, the manually assigned token is returned.
     * Otherwise, providers are queried lazily according to the configured
     * priority list. If the list is empty, the default order is:
     * FCM, HMS, RuStore.
     *
     * @param context Application context.
     * @return Selected token data, or `null` if no token is available.
     */
    suspend fun getCurrentPushToken(
        context: Context
    ): TokenData? {
        return try {
            val env = Environment
                .create(
                    context
                )
            if (
                fcmProvider == null &&
                hmsProvider == null &&
                rustoreProvider == null
            ) {
                env.manualToken()
            } else {
                getTokens(context).let { tokens ->
                    getPriorityToken(
                        env.pushConfig()
                            .providerPriority,
                        fcm = tokens.fcm::get,
                        hms = tokens.hms::get,
                        rus = tokens.rus::get
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getCurrentToken", e)
            null
        }.also { token ->
            tokenEvent(token)
        }
    }

    /**
     * Creates lazy token loaders for all supported providers.
     *
     * A provider token is requested only when its corresponding [SuspendLazy]
     * instance is accessed. This prevents lower-priority providers from being
     * queried when a higher-priority token is already available.
     *
     * @param context Application context required by the HMS provider.
     * @return Lazy token loaders for FCM, HMS, and RuStore.
     */
    private fun getTokens(context: Context) = object {

        /**
         * Lazily loads the FCM token.
         */
        val fcm = SuspendLazy {
            getNonEmptyToken(FCM_PROVIDER) {
                fcmProvider?.getToken()
            }
        }

        /**
         * Lazily loads the HMS token.
         */
        val hms = SuspendLazy {
            getNonEmptyToken(HMS_PROVIDER) {
                hmsProvider?.getToken(context)
            }
        }

        /**
         * Lazily loads the RuStore token.
         */
        val rus = SuspendLazy {
            getNonEmptyToken(RUS_PROVIDER) {
                rustoreProvider?.getToken()
            }
        }
    }

    /**
     * Resolves a token according to the configured provider priority.
     *
     * Each provider loader is invoked only when needed. If the priority list
     * is empty, providers are checked in the default order: FCM, HMS, RuStore.
     *
     * @param providerPriority Configured provider priority.
     * @param fcm Lazy FCM token loader.
     * @param hms Lazy HMS token loader.
     * @param rus Lazy RuStore token loader.
     * @return First available token, or `null` if no provider returns one.
     */
    private suspend fun getPriorityToken(
        providerPriority: List<String>?,
        fcm: suspend () -> TokenData?,
        hms: suspend () -> TokenData?,
        rus: suspend () -> TokenData?
    ): TokenData? {
        if (providerPriority.isNullOrEmpty())
            return fcm() ?: hms() ?: rus()

        return providerPriority
            .take(3)
            .firstNotNullOfOrNull { provider ->
                when (provider) {
                    FCM_PROVIDER -> fcm()
                    HMS_PROVIDER -> hms()
                    RUS_PROVIDER -> rus()
                    else -> null
                }
            }
    }

    /**
     * Logs the push provider setup event when the selected token changes.
     *
     * @param token Current token data, or `null` if unavailable.
     */
    private suspend fun tokenEvent(token: TokenData?) {
        val func = "tokenEvent"
        val moduleEvent = ModuleEvents.pushProviderSet

        try {
            tokenLogMutex.withLock {
                if (token == null || lastReportedToken == token) {
                    return
                }

                val value = token.toMap()

                event(
                    func,
                    moduleEvent,
                    value,
                    value
                )

                lastReportedToken = token
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(func, e)
        }
    }

    /**
     * Requests a non-empty token from a provider.
     *
     * The provider is queried up to three times. A one-second delay is applied
     * after an empty token is returned.
     *
     * @param provider Provider identifier.
     * @param fetch Operation that requests the provider token.
     * @return Token data, or `null` if no valid token is received.
     */
    private suspend fun getNonEmptyToken(
        provider: String,
        fetch: suspend () -> String?
    ): TokenData? {
        repeat(3) {
            fetch()?.let { if (it.isNotEmpty()) return TokenData(provider, it) }
            delay(1.seconds)
        }
        return null
    }

    /**
     * Deletes the current token from the specified provider.
     *
     * Waits at most [deleteTokenTimeout] for the provider callback; provider
     * implementations are app-supplied, so a missing callback must not
     * suspend the caller forever. Returns `false` on timeout, and a late
     * callback is ignored because the continuation is no longer active.
     *
     * @param context Application context.
     * @param provider Provider identifier.
     * @return `true` if the token was deleted successfully.
     */
    suspend fun deleteDeviceToken(
        context: Context,
        provider: String
    ): Boolean = withTimeoutOrNull(deleteTokenTimeout) {
        suspendCancellableCoroutine { continuation ->
            val complete: (Boolean) -> Unit = { success ->
                if (continuation.isActive) continuation.resume(
                    success
                )
            }

            when (provider) {
                HMS_PROVIDER ->
                    hmsProvider?.deleteToken(context, complete)

                RUS_PROVIDER ->
                    rustoreProvider?.deleteToken(complete)

                FCM_PROVIDER ->
                    fcmProvider?.deleteToken(complete)

                else ->
                    continuation.resume(false)
            } ?: continuation.resume(false)
        }
    } ?: false

    /**
     * Deletes the current provider token and requests a token update.
     *
     * @param context Application context.
     */
    suspend fun forcedTokenUpdate(context: Context) {
        try {
            val provider = Environment.create(context).currentToken().provider
            if (deleteDeviceToken(context, provider)) pushTokenUpdate(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("forcedTokenUpdate", e)
        }
    }

    /**
     * Changes the push provider priority and updates the current token.
     *
     * The stored configuration is updated before the token update operation
     * starts, ensuring that token selection uses the new provider order.
     *
     * @param context Application context.
     * @param priorityList New provider priority list.
     */
    suspend fun changePriority(
        context: Context,
        priorityList: List<String>
    ) {
        try {
            val dao = Environment.create(context).room.dao.config
            if (!providersValid(priorityList)) exception(invalidProvider)
            val config = dao.get() ?: exception(ModuleEvents.configIsNull)
            dao.insert(config.copy(providerPriority = priorityList))
            ConfigManager.clearConfigCache()
            pushTokenUpdate(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("providerChange", e)
        }
    }
}
