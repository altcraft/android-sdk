package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.config.ConfigSetup.updateConfigCache
import com.altcraft.sdk.push.token.TokenManager.fcmProvider
import com.altcraft.sdk.push.token.TokenManager.hmsProvider
import com.altcraft.sdk.push.token.TokenManager.rustoreProvider
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.events.EventList.configIsNull
import com.altcraft.sdk.events.EventList.invalidPushProvider
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.interfaces.FCMInterface
import com.altcraft.sdk.interfaces.HMSInterface
import com.altcraft.sdk.interfaces.RustoreInterface
import com.altcraft.sdk.push.token.TokenManager.allProvidersValid
import com.altcraft.sdk.push.token.TokenUpdate.tokenUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Provides public access to the functions of the `TokenManager` object.
 *
 */
@Keep
@Suppress("MemberVisibilityCanBePrivate")
object PublicPushTokenFunctions {

    /**
     * Manually sets the push token for the given provider.
     *
     * This method is intended to be called from `onNewToken()` callbacks
     * inside push provider services (FCM, HMS, RuStore).
     * It stores the token and provider information in shared preferences
     * for later retrieval by the SDK.
     *
     * @param context The application context.
     * @param provider The push provider identifier (e.g., "android-firebase").
     * @param token The push token received from the provider.
     */
    @Suppress("unused")
    fun setPushToken(context: Context, provider: String, token: String) {
        Preferenses.setPushToken(context, provider, token)
    }

    /**
     * Retrieves the token data for the device.
     *
     * This function delegates the token retrieval operation to the `TokenManager`.
     *
     * @param context The `Context` required for token retrieval.
     * @return A nullable `DataClasses.TokenData` containing the token information,
     * or `null` if no token is available.
     */
    suspend fun getPushToken(context: Context): DataClasses.TokenData? {
        return TokenManager.getCurrentToken(context)
    }

    /**
     * Sets the Firebase Cloud Messaging (FCM) token provider.
     *
     * @param provider The `FCMInterface` implementation to be used as the FCM token provider,
     * or `null` to unset it.
     */
    fun setFCMTokenProvider(provider: FCMInterface?) {
        fcmProvider = provider
    }

    /**
     * Sets the Huawei Mobile Services (HMS) token provider.
     *
     * @param provider The `HMSInterface` implementation to be used as the HMS token provider,
     * or `null` to unset it.
     */
    fun setHMSTokenProvider(provider: HMSInterface?) {
        hmsProvider = provider
    }

    /**
     * Sets the RuStore token provider.
     *
     * @param provider The `RustoreInterface` implementation to be used as the RuStore token provider,
     * or `null` to unset it.
     */
    fun setRuStoreTokenProvider(provider: RustoreInterface?) {
        rustoreProvider = provider
    }

    /**
     * Deletes the device token for a specified provider.
     *
     * This function delegates the token deletion operation to the `TokenManager` based on
     * the specified provider.
     *
     * @param context The `Context` required for token deletion.
     * @param provider A `String` representing the token provider (`FCM_PROVIDER`, `HMS_PROVIDER`,
     * or `RUSTORE_PROVIDER`).
     * @param complete A callback function invoked after the token deletion operation is complete.
     */
    suspend fun deleteDeviceToken(
        context: Context,
        provider: String,
        complete: () -> Unit = {}
    ) {
        when (provider) {
            FCM_PROVIDER -> TokenManager.deleteFCMToken { complete() }
            RUS_PROVIDER -> TokenManager.deleteRuStoreToken { complete() }
            HMS_PROVIDER -> TokenManager.deleteHMSToken(context) { complete() }
        }
    }

    /**
     * Forces a token refresh by deleting the current device token and triggering an update flow.
     *
     * This function:
     * - Deletes the existing push token using its provider.
     * - Then performs a full token update via [TokenUpdate.tokenUpdate].
     * - Calls [complete] upon successful completion of the update flow.
     *
     * All operations run on [Dispatchers.IO].
     *
     * @param context Application context used for token and update operations.
     * @param complete Callback invoked after the update finishes.
     */
    fun forcedTokenUpdate(context: Context, complete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deleteDeviceToken(context, getPushToken(context)?.provider ?: return@launch) {
                    CoroutineScope(Dispatchers.IO).launch {
                        tokenUpdate(context)
                        complete()
                    }
                }
            } catch (e: Exception) {
                error("forcedTokenUpdate", e)
            }
        }
    }

    /**
     * Updates the provider priority list in the local configuration and triggers token update.
     *
     * @param context The application context.
     * @param priorityList A list of push provider identifiers (e.g., "android-firebase").
     *
     * @throws IllegalStateException If configuration is missing or contains invalid provider names.
     */
    suspend fun changePushProviderPriorityList(context: Context, priorityList: List<String>) {
        try {
            val room = SDKdb.getDb(context)

            if (room.request().getConfig() == null) exception(configIsNull)
            if (!allProvidersValid(priorityList)) exception(invalidPushProvider)

            room.request().updateProviderPriorityList(priorityList)

            updateConfigCache(context)

            tokenUpdate(context)
        } catch (e: Exception) {
            error("providerChange", e)
        }
    }
}
