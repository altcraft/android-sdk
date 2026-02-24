package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.config.ConfigSetup.updateConfigCache
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.push.token.TokenManager.fcmProvider
import com.altcraft.sdk.push.token.TokenManager.hmsProvider
import com.altcraft.sdk.push.token.TokenManager.rustoreProvider
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.EventList.configIsNull
import com.altcraft.sdk.sdk_events.EventList.invalidPushProvider
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import com.altcraft.sdk.push.token.TokenManager.allProvidersValid
import com.altcraft.sdk.push.token.TokenUpdate.pushTokenUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Public facade for configuring push token providers and managing device tokens.
 */
@Keep
@Suppress("MemberVisibilityCanBePrivate")
object PublicPushTokenFunctions {

    /**
     * Manually sets the push token for the given provider.
     *
     * Persists the token in the SDK storage (DataStore).
     * Manual token has lower priority than provider tokens.
     *
     * @param context The application context.
     * @param provider The push provider identifier (e.g., "android-firebase").
     * @param token The push token received from the provider.
     */
    @Suppress("unused")
    fun setPushToken(context: Context, provider: String?, token: String?) {
        Preferenses.setPushToken(context, provider, token)
    }

    /**
     * Returns the current device token data, if available.
     *
     * @param context The [Context] required for token retrieval.
     * @return [DataClasses.TokenData] or `null` if no token is available.
     */
    suspend fun getPushToken(context: Context): DataClasses.TokenData? {
        return TokenManager.getCurrentPushToken(context)
    }

    /**
     * Sets the Firebase Cloud Messaging (FCM) token provider.
     *
     * @param provider The [FCMInterface] implementation to be used, or `null` to unset it.
     */
    fun setFCMTokenProvider(provider: FCMInterface?) {
        fcmProvider = provider
    }

    /**
     * Sets the Huawei Mobile Services (HMS) token provider.
     *
     * @param provider The [HMSInterface] implementation to be used, or `null` to unset it.
     */
    fun setHMSTokenProvider(provider: HMSInterface?) {
        hmsProvider = provider
    }

    /**
     * Sets the RuStore token provider.
     *
     * @param provider The [RustoreInterface] implementation to be used, or `null` to unset it.
     */
    fun setRuStoreTokenProvider(provider: RustoreInterface?) {
        rustoreProvider = provider
    }

    /**
     * Deletes the device token for the specified provider.
     *
     * @param context The [Context] required for token deletion.
     * @param provider Provider identifier: [FCM_PROVIDER], [HMS_PROVIDER], or [RUS_PROVIDER].
     * @param complete Callback invoked after the delete call is dispatched.
     */
    fun deleteDeviceToken(
        context: Context,
        provider: String,
        complete: () -> Unit = {}
    ) {
        when (provider) {
            FCM_PROVIDER -> fcmProvider?.deleteToken { complete() }
            RUS_PROVIDER -> rustoreProvider?.deleteToken { complete() }
            HMS_PROVIDER -> hmsProvider?.deleteToken(context) { complete() }
        }
    }

    /**
     * Triggers token refresh: deletes the current device token and runs token update flow.
     *
     * Steps:
     * - Requests token deletion via the current provider.
     * - Then calls [TokenUpdate.pushTokenUpdate].
     * - Invokes [complete] after the update call finishes.
     *
     * Runs on [Dispatchers.IO].
     *
     * @param context Application context used for token and update operations.
     * @param complete Callback invoked after the update call finishes.
     */
    fun forcedTokenUpdate(
        context: Context, complete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val func = "forcedTokenUpdate"
            try {
                val env = Environment.create(context)
                val provider = env.token().provider

                deleteDeviceToken(context, provider) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            pushTokenUpdate(context)
                            complete()
                        } catch (e: Exception) {
                            error(func, e)
                        }
                    }
                }
            } catch (e: Exception) {
                error(func, e)
            }
        }
    }

    /**
     * Updates provider priority list in local configuration and triggers token update.
     *
     * @param context The application context.
     * @param priorityList List of push provider identifiers (e.g., "android-firebase").
     */
    suspend fun changePushProviderPriorityList(context: Context, priorityList: List<String>) {
        try {
            val room = SDKdb.getDb(context)

            if (room.request().getConfig() == null) exception(configIsNull)
            if (!allProvidersValid(priorityList)) exception(invalidPushProvider)

            room.request().updateProviderPriorityList(priorityList)

            updateConfigCache(context)

            pushTokenUpdate(context)
        } catch (e: Exception) {
            error("providerChange", e)
        }
    }
}