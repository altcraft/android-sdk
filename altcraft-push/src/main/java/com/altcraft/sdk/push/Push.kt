package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.system.notification.PushPresenter.showPush
import com.altcraft.sdk.push.platform.event.EventAPI
import com.altcraft.sdk.push.system.receiver.IncomingPushManager.handlePush
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionAPI
import com.altcraft.sdk.push.platform.token.TokenAPI
import com.altcraft.sdk.push.util.Utils
import com.altcraft.sdk.push.util.Utils.altcraftPush

/**
 * Provides access to the Push module through [AltcraftSDK].
 */
@get:Keep
val AltcraftSDK.push: Push get() = Push


/**
 * Central control point for the push module.
 *
 * Provides access to push subscription, token, event,
 * receiver, and configuration APIs.
 */
@Keep
object Push {

    /**
     * Push token API.
     */
    @Keep
    val token = TokenAPI

    /**
     * Push event API.
     */
    @Keep
    val pushEvents = EventAPI

    /**
     * Push module events.
     */
    @Keep
    val moduleEvents = ModuleEvents

    /**
     * Push receiver API.
     */
    @Keep
    val receiver = PushReceiverApi

    /**
     * Push subscription API.
     */
    @Keep
    val subscription = SubscriptionAPI

    /**
     * Requests notification permission from the user.
     *
     * @param context Context used to check the permission.
     * @param activity Activity used to request the permission.
     */
    fun requestNotificationPermission(context: Context, activity: ComponentActivity) {
        Utils.requestNotificationPermission(
            context, activity
        )
    }
}

/**
 * Provides functions for processing incoming push notifications.
 */
@Keep
object PushReceiverApi {

    /**
     * Checks whether the specified push notification belongs to Altcraft.
     *
     * @param message Push notification payload.
     * @return `true` if the message is an Altcraft push; otherwise `false`.
     */
    @Keep
    fun isAltcraftPush(
        message: Map<String, String>
    ) = altcraftPush(
        message
    )

    /**
     * Delivers an incoming push notification to the SDK.
     *
     * @param context Application context used for push processing.
     * @param message Push notification payload.
     */
    @Keep
    fun takePush(
        context: Context,
        message: Map<String, String>
    ) = handlePush(
        context, message
    )
}

/**
 * Base receiver for incoming Altcraft push notifications.
 *
 * Extend this class to provide custom push notification handling.
 *
 * The SDK searches for implementations of this class using reflection
 * in the packages configured through [com.altcraft.sdk.push.config.PushConfiguration].
 *
 * If no custom receiver is found, the SDK creates the default
 * [PushReceiver] implementation and calls [pushHandler].
 */
@Keep
open class PushReceiver {

    /**
     * Processes an incoming Altcraft push notification.
     *
     * Override this method to provide custom push handling.
     *
     * The default implementation displays the notification using
     * the SDK push presenter.
     *
     * @param context Context used for push processing.
     * @param message Push notification payload.
     */
    @Keep
    open suspend fun pushHandler(
        context: Context,
        message: Map<String, String>
    ) {
        showPush(
            context, message
        )
    }
}