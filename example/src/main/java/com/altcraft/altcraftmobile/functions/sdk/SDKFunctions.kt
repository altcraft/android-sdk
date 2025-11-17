package com.altcraft.altcraftmobile.functions.sdk

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.altcraftmobile.data.AppConstants.APP_ID
import com.altcraft.altcraftmobile.data.AppConstants.APP_IID
import com.altcraft.altcraftmobile.data.AppConstants.APP_VER
import com.altcraft.altcraftmobile.data.AppConstants.PUSH_CHANNEL_DESCRIPTION
import com.altcraft.altcraftmobile.data.AppConstants.PUSH_CHANNEL_NAME
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.data.AppPreferenses
import com.altcraft.altcraftmobile.data.AppPreferenses.getConfig
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscribeSettings
import com.altcraft.altcraftmobile.data.AppPreferenses.setConfig
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.push.subscribe.PublicPushSubscriptionFunctions.unSuspendPushSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SDKFunctions {

    /**
     * Initializes Altcraft SDK using configuration from SharedPreferences.
     * If no profile status is recorded yet, triggers a background fetch of the
     * current subscription status.
     *
     * @param context Application context.
     */
    fun initAltcraft(context: Context) {
        getConfig(context)?.let {

            val config = AltcraftConfiguration.Builder(
                apiUrl = it.apiUrl,
                icon = it.icon,
                rToken = it.rToken,
                usingService = it.usingService,
                serviceMessage = it.serviceMessage,
                providerPriorityList = it.priorityProviders,
                pushChannelName = PUSH_CHANNEL_NAME,
                pushChannelDescription = PUSH_CHANNEL_DESCRIPTION,
                appInfo = DataClasses.AppInfo(appID = APP_ID, appIID = APP_IID, appVer = APP_VER)
            ).build()

            AltcraftSDK.initialization(context, config)
            if (getSubscriptionStatus(context) == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    AltcraftSDK.pushSubscriptionFunctions.getStatusForCurrentSubscription(
                        context
                    )
                }
            }
        }
    }

    /**
     * Helpers to subscribe/unsubscribe using saved settings from SharedPreferences.
     * Applies defaults: sync=true when unset; replace/skipTriggers only when explicitly true.
     */
    object Subscribe {

        /**
         * Subscribes to push using stored subscribe settings.
         *
         * - sync = true unless explicitly set to false
         * - replace/skipTriggers are applied only if explicitly true
         * - forwards custom/profile fields and categories
         *
         * @param context Application context.
         */
        internal fun subscribe(context: Context) {
            val subscribeSetting = getSubscribeSettings(context)

            AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(
                context,
                sync = subscribeSetting?.sync != false,
                replace = subscribeSetting?.replace == true,
                skipTriggers = subscribeSetting?.skipTriggers == true,
                customFields = subscribeSetting?.customFields,
                profileFields = subscribeSetting?.profileFields,
                cats = subscribeSetting?.cats
            )
        }

        /**
         * Unsubscribes from push using stored subscribe settings.
         *
         * - sync = true unless explicitly set to false
         * - replace/skipTriggers are applied only if explicitly true
         * - forwards custom/profile fields
         *
         * @param context Application context.
         */
        internal fun unSubscribe(context: Context) {
            val subscribeSetting = getSubscribeSettings(context)

            AltcraftSDK.pushSubscriptionFunctions.pushUnSubscribe(
                context,
                sync = subscribeSetting?.sync != false,
                replace = subscribeSetting?.replace == true,
                skipTriggers = subscribeSetting?.skipTriggers == true,
                customFields = subscribeSetting?.customFields,
                profileFields = subscribeSetting?.profileFields,
            )
        }
    }

    object Login {

        /**
         * Manages push subscription state during user login/logout transitions.
         *
         * Flow:
         * 1. Updates the authentication status (JWT) depending on [logIn].
         * 2. Calls unSuspendPushSubscription(context), which:
         *    - suspends subscriptions related to the previous profile,
         *    - reactivates subscriptions for the current profile if they exist.
         * 3. If the current JWT profile does not have a subscription
         *    (response.profile?.subscription == null),
         *    creates a new push subscription via pushSubscribe().
         *
         * @param context application context required by the SDK
         * @param logIn   flag indicating the transition type:
         *   true  → login, transfers the subscription to the registered user profile,
         *   false → logout, transfers the subscription back to the anonymous user profile
         */
        private suspend fun unSuspend(context: Context, logIn: Boolean) {
            val subscribeSetting = getSubscribeSettings(context)

            AppPreferenses.setAuthStatus(context, logIn)

            unSuspendPushSubscription(context).let {
                if (it?.httpCode == 200 && it.response?.profile?.subscription == null) {
                    AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(
                        context,
                        sync = true,
                        replace = subscribeSetting?.replace == false,
                        skipTriggers = subscribeSetting?.skipTriggers == false,
                        customFields = subscribeSetting?.customFields,
                        profileFields = subscribeSetting?.profileFields,
                    )
                }
            }
        }

        /**
         * Performs the login transition for push subscriptions.
         *
         * Launches a coroutine on Dispatchers.IO and calls unSuspend(context, true),
         * which suspends the anonymous profile subscription and attempts to
         * transfer/reactivate the subscription under the registered user profile.
         *
         * @param context application context required by the SDK
         */
        internal fun logIn(context: Context) = CoroutineScope(Dispatchers.IO).launch {
            unSuspend(context, true)
        }

        /**
         * Performs the logout transition for push subscriptions.
         *
         * Launches a coroutine on Dispatchers.IO and calls unSuspend(context, false),
         * which suspends the registered user profile subscription and reactivates
         * the subscription under the anonymous user profile.
         *
         * @param context application context required by the SDK
         */
        internal fun logOut(context: Context) = CoroutineScope(Dispatchers.IO).launch {
            unSuspend(context, false)
        }

    }

    internal fun updateProvider(
        context: Context,
        viewModel: MainViewModel,
        providerList: List<String>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            AltcraftSDK.pushTokenFunctions.changePushProviderPriorityList(context, providerList)

            val currentConfig = getConfig(context)

            if (currentConfig != null) {
                setConfig(
                    context, AppDataClasses.ConfigData(
                        apiUrl = currentConfig.apiUrl,
                        icon = currentConfig.icon,
                        rToken = currentConfig.rToken,
                        usingService = currentConfig.usingService,
                        serviceMessage = currentConfig.serviceMessage,
                        priorityProviders = providerList
                    )
                )
            }
            viewModel.updateTokenUI(context)
        }
    }
}