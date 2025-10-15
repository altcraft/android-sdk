package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.PairBuilder.createSetTokenEventPair
import com.altcraft.sdk.concurrency.SuspendLazy
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.Constants.validProviders
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses.getManualToken
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles push token operations — retrieving, validating, and deleting the device's push token.
 * Supports integration with push notification providers.
 */
internal object TokenManager {

    @Volatile
    internal var fcmProvider: FCMInterface? = null

    @Volatile
    internal var hmsProvider: HMSInterface? = null

    @Volatile
    internal var rustoreProvider: RustoreInterface? = null

    @Volatile
    internal var tokenLogShow = AtomicBoolean(false)
    private val tokenLogMutex = Mutex()

    /**
    List of tokens for which the "set token" event
    has already been logged/shown
     */
    internal val tokens = CopyOnWriteArrayList<String>()

    /**
     * Validates that all items in the given list are known push providers.
     *
     * @param providers A list of provider identifiers to check.
     * @return `true` if all values are valid, `false` otherwise.
     */
    fun allProvidersValid(providers: List<String>?): Boolean {
        return if (!providers.isNullOrEmpty()) providers.all {
            it in validProviders
        } else true
    }

    /**
     * Deletes the FCM (Firebase Cloud Messaging) token.
     *
     * @param result A callback that receives a `Boolean` indicating the success (`true`) or
     * failure (`false`) of the operation.
     */
    suspend fun deleteFCMToken(result: (Boolean) -> Unit) =
        fcmProvider?.deleteToken(result)

    /**
     * Deletes the HMS (Huawei Mobile Services) token.
     *
     * @param context The `Context` required to interact with HMS services.
     * @param result A callback that receives a `Boolean` indicating the success (`true`) or
     * failure (`false`) of the operation.
     */
    suspend fun deleteHMSToken(context: Context, result: (Boolean) -> Unit) =
        hmsProvider?.deleteToken(context, result)

    /**
     * Deletes the RuStore token.
     *
     * @param result A callback that receives a `Boolean` indicating the success (`true`) or
     * failure (`false`) of the operation.
     */
    suspend fun deleteRuStoreToken(result: (Boolean) -> Unit) =
        rustoreProvider?.deleteToken(result)

    /**
     * Lazily retrieves the FCM (Firebase Cloud Messaging) token.
     */
    private fun getFCMTokenData() = SuspendLazy {
        getNonEmptyToken(FCM_PROVIDER) { fcmProvider?.getToken() }
    }

    /**
     * Lazily retrieves the HMS (Huawei Mobile Services) token.
     *
     * @param context Android [Context] used for accessing HMS services.
     */
    private fun getHMSTokenData(context: Context) = SuspendLazy {
        getNonEmptyToken(HMS_PROVIDER) { hmsProvider?.getToken(context) }
    }

    /**
     * Lazily retrieves the RuStore token.
     */
    private fun getRUSTokenData() = SuspendLazy {
        getNonEmptyToken(RUS_PROVIDER) { rustoreProvider?.getToken() }
    }

    /**
     * Returns the active push token for the device.
     *
     * First checks if a manual token is set and returns it immediately.
     * If not, attempts to fetch a token based on the configured `providerPriorityList`.
     * Falls back to the default order: FCM → HMS → RuStore.
     * The first successfully retrieved token is logged once per session.
     *
     * @param context The application context.
     * @return The selected [DataClasses.TokenData], or `null` if no token could be retrieved.
     */
    suspend fun getCurrentToken(context: Context): DataClasses.TokenData? {
        return try {
            getManualToken(context)?.let { return it }

            val fcm = getFCMTokenData()
            val rus = getRUSTokenData()
            val hms = getHMSTokenData(context)

            val priority = getConfig(context)?.providerPriorityList

            getPriorityToken(priority, fcm::get, hms::get, rus::get).also {
                tokenEvent(it)
            }
        } catch (e: Exception) {
            error("getCurrentToken", e)
            null
        }
    }

    /**
     * Resolves the device token using the specified provider priority list.
     *
     * Iterates over the first three items in `providerPriorityList` and calls the matching
     * provider fetcher.
     * Returns the first non-null result. If no token is found via the list,
     * falls back to: FCM → HMS → RuStore.
     * Each provider fetch function is called lazily and only if needed.
     *
     * @return The first available [DataClasses.TokenData], or `null` if all attempts fail
     */
    private suspend fun getPriorityToken(
        providerPriorityList: List<String>?,
        fcm: suspend () -> DataClasses.TokenData?,
        hms: suspend () -> DataClasses.TokenData?,
        rus: suspend () -> DataClasses.TokenData?
    ): DataClasses.TokenData? {
        return if (providerPriorityList.isNullOrEmpty())
            fcm() ?: hms() ?: rus()
        else {
            providerPriorityList.take(3).firstNotNullOfOrNull {
                when (it) {
                    FCM_PROVIDER -> fcm()
                    HMS_PROVIDER -> hms()
                    RUS_PROVIDER -> rus()
                    else -> null
                }
            }
        }
    }

    /**
     * Creates and sends a token analytics event exactly once per session.
     *
     * @param token Current push token of the device to be logged.
     *              If `null`, the function returns immediately without any action.
     *              Non-null token will trigger an analytics event on first call only.
     */
    private suspend fun tokenEvent(token: DataClasses.TokenData?) {
        val func = "tokenEvent"
        try {
            tokenLogMutex.withLock {
                if (
                    token != null && tokenLogShow.compareAndSet(false, true) &&
                    tokens.lastOrNull() != token.token
                ) {
                    event(func, createSetTokenEventPair(token), token.toMap())
                    tokens.add(token.token)
                }
            }
        } catch (e: Exception) {
            error(func, e)
        }
    }

    /**
     * Waits up to 3 seconds until all push tokens (FCM, HMS, RuStore) are non-empty.
     *
     * Retries token checks every second. Empty strings are considered invalid.
     *
     * @param fetch function for fetch token.
     */
    private suspend fun getNonEmptyToken(
        provider: String,
        fetch: suspend () -> String?
    ): DataClasses.TokenData? {
        repeat(3) {
            try {
                fetch()?.let {
                    if (it.isNotEmpty()) return DataClasses.TokenData(provider, it) else delay(1000)
                }
            } catch (e: Exception) {
                error("getNonEmptyToken", e)
            }
        }
        return null
    }
}