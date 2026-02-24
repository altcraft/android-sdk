package com.altcraft.sdk

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.additional.SubFunction.altcraftPush
import com.altcraft.sdk.auth.JWTManager
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.push.token.PublicPushTokenFunctions
import com.altcraft.sdk.core.ClearCache
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.push.subscribe.PublicPushSubscriptionFunctions
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.interfaces.JWTInterface
import com.altcraft.sdk.core.InitialOperations.initControl
import com.altcraft.sdk.mob_events.PublicMobileEventFunctions
import com.altcraft.sdk.profile.PublicProfileFunctions
import com.altcraft.sdk.push.IncomingPushManager.handlePush
import com.altcraft.sdk.push.PushPresenter.showPush
import com.altcraft.sdk.push.events.PublicPushEventFunctions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central control point of the Altcraft SDK.
 * Provides public APIs for interacting with the Altcraft platform.
 */
@Keep
object AltcraftSDK {

    /** Push subscription API: subscribe/suspend/unsubscribe and status queries. */
    @Keep
    val pushSubscriptionFunctions = PublicPushSubscriptionFunctions

    /** Mobile event API: send mobile events. */
    @Keep
    val mobileEventFunctions = PublicMobileEventFunctions

    /** Push token API: set/get token, configure providers, manage token lifecycle. */
    @Keep
    val pushTokenFunctions = PublicPushTokenFunctions

    /** Push event API: report delivery/open events. */
    @Keep
    val pushEventFunctions = PublicPushEventFunctions

    /** Profile API: update profile fields. */
    @Keep
    val profileFunctions = PublicProfileFunctions

    /** SDK events API: subscribe/unsubscribe to SDK event stream. */
    @Keep
    val eventSDKFunctions = Events


    /**
     * Public entry point to initialize the Altcraft SDK.
     *
     * @param context Android application context used by SDK internal processes.
     * @param configuration Configuration object with required SDK parameters.
     */
    @Keep
    fun initialization(
        context: Context,
        configuration: AltcraftConfiguration,
        complete: ((Result<Unit>) -> Unit)? = null
    ) {
        Init.init(context, configuration, complete)
    }

    /**
     * Clears SDK data and stops active SDK background work.
     *
     * @param context Application context used for cleanup.
     * @param onComplete Optional callback invoked after cleanup completes.
     */
    @Keep
    fun clear(context: Context, onComplete: (() -> Unit)? = null) {
        ClearCache.clear(context) { onComplete?.invoke() }
    }

    /**
     * Public method to register a JWT token provider for SDK use.
     *
     * @param provider Implementation of [JWTInterface] supplying JWT tokens.
     */
    @Keep
    fun setJWTProvider(provider: JWTInterface?) {
        JWTManager.register(provider)
    }

    /** Allows re-running initial retry/scheduling/token-check
     *  logic in this process on next [initialization]. */
    @Keep
    fun unlockInitialOperationsInThisSession() {
        initControl = AtomicBoolean(false)
    }

    /**
     * Requests notification permission from the user (Android 13+).
     *
     * @param context Context for permission state check.
     * @param activity Activity to present the permission prompt from.
     */
    @Keep
    fun requestNotificationPermission(context: Context, activity: ComponentActivity) {
        SubFunction.requestNotificationPermission(context, activity)
    }

    /**
     * An open class responsible for receiving incoming push notifications.
     * Can be overridden to handle Altcraft notifications in other app modules.
     */
    @Keep
    open class PushReceiver {

        /**
         * Processes an incoming Altcraft push message.
         *
         * @param context Context for service and operations.
         * @param message Push data payload.
         */
        open suspend fun pushHandler(context: Context, message: Map<String, String>) {
            showPush(context, message)
        }

        companion object {

            /**
             * Checks whether a push notification belongs to Altcraft.
             *
             * @param message Push payload converted from `RemoteMessage.data`.
             * @return `true` if this is an Altcraft push; otherwise `false`.
             */
            fun isAltcraftPush(message: Map<String, String>) = altcraftPush(
                message
            )

            /**
             * Delivers a push message to the SDK for processing.
             *
             * @param context Application context.
             * @param message Push payload.
             */
            fun takePush(context: Context, message: Map<String, String>) {
                handlePush(context, message)
            }
        }
    }
}