package com.altcraft.sdk.push.platform.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.push.data.Preferenses.setPushToken
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.platform.token.TokenManager.changePriority
import com.altcraft.sdk.push.platform.token.TokenManager.deleteDeviceToken
import com.altcraft.sdk.push.platform.token.TokenManager.getCurrentPushToken
import com.altcraft.sdk.push.platform.token.provider.FCMInterface
import com.altcraft.sdk.push.platform.token.provider.HMSInterface
import com.altcraft.sdk.push.platform.token.provider.RustoreInterface

/**
 * Public facade for configuring push token providers and managing device tokens.
 */
@Keep
object TokenAPI {

    /**
     * Sets the Firebase Cloud Messaging (FCM) token provider.
     */
    fun setFCMTokenProvider(provider: FCMInterface?) {
        TokenManager.fcmProvider = provider
    }

    /**
     * Sets the Huawei Mobile Services (HMS) token provider.
     */
    fun setHMSTokenProvider(provider: HMSInterface?) {
        TokenManager.hmsProvider = provider
    }

    /**
     * Sets the RuStore token provider.
     */
    fun setRuStoreTokenProvider(provider: RustoreInterface?) {
        TokenManager.rustoreProvider = provider
    }

    /**
     * Returns the current device token data, if available.
     */
    suspend fun getToken(context: Context): TokenData? = getCurrentPushToken(
        context
    )

    /**
     * Deletes the device token for the specified provider.
     */
    suspend fun deleteToken(context: Context, provider: String) = deleteDeviceToken(
        context, provider
    )

    /**
     * Triggers token refresh by deleting the current device token
     * and running the token update flow.
     */
    suspend fun forcedTokenUpdate(context: Context) = TokenManager.forcedTokenUpdate(
        context
    )

    /**
     * Manually sets the push token for the given provider.
     */
    suspend fun setToken(context: Context, provider: String?, token: String?) = setPushToken(
        context, provider, token
    )

    /**
     * Updates the provider priority list and triggers token update.
     */
    suspend fun changeProviderPriority(context: Context, priority: List<String>) = changePriority(
        context, priority
    )
}
