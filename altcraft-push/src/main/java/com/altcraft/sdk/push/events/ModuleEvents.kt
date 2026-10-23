package com.altcraft.sdk.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.events.SDKEvent

/**
 * Events emitted by the Push module.
 */
@Keep
object ModuleEvents {

    /** SDK event implementation carrying the message text. */
    private data class Event(
        override val message: String
    ) : SDKEvent

    // Config

    val invalidProvider: SDKEvent =
        Event(
            "Invalid push configuration: unsupported push provider. " +
                    "Available providers: " +
                    "android-firebase, android-huawei, android-rustore."
        )

    val configIsNull: SDKEvent =
        Event("push configuration is null")

    val invalidCustomFields: SDKEvent =
        Event("invalid custom fields: not all values are primitives")

    // Push events

    val eventRetryLimit: SDKEvent =
        Event("push event retry limit")

    val eventRequestDataIsNull: SDKEvent =
        Event("push event request data is null")

    val eventRequestSuccessful: SDKEvent =
        Event("successful request: event/push")

    val eventRequestFailed: SDKEvent =
        Event("failed request: event/push")

    val pushUidIsNull: SDKEvent =
        Event("push uid is null")

    // Push subscription

    val subscribeRetryLimit: SDKEvent =
        Event("push subscribe retry limit")

    val subscribeRequestDataIsNull: SDKEvent =
        Event("push subscribe request data is null")

    val subscribeRequestSuccessful: SDKEvent =
        Event("successful request: push/subscribe")

    val subscribeRequestFailed: SDKEvent =
        Event("failed request: push/subscribe")

    val suspendRequestSuccessful: SDKEvent =
        Event("successful request: push/suspend")

    val suspendRequestFailed: SDKEvent =
        Event("failed request: push/suspend")

    val unsubscribeRequestSuccessful: SDKEvent =
        Event("successful request: push/unsubscribe")

    val unsubscribeRequestFailed: SDKEvent =
        Event("failed request: push/unsubscribe")

    // Token

    val tokenUpdateRequestDataIsNull: SDKEvent =
        Event("token update request data is null")

    val tokenUpdateRequestSuccessful: SDKEvent =
        Event("successful request: push/update")

    val tokenUpdateRequestFailed: SDKEvent =
        Event("failed request: push/update")

    val pushTokenIsNull: SDKEvent =
        Event("push token is null")

    val notUpdated: SDKEvent =
        Event("push token not updated")

    val pushProviderSet: SDKEvent =
        Event("push provider set -")

    val invalidPushProvider: SDKEvent =
        Event(
            "Invalid push provider. " + "Available providers: " +
                    "android-firebase, android-huawei, android-rustore."
        )

    // Subscription status

    val statusRequestDataIsNull: SDKEvent =
        Event("subscription status request data is null")

    val statusRequestSuccess: SDKEvent =
        Event("successful request: push/status")

    val statusRequestFailed: SDKEvent =
        Event("failed request: push/status")

    // UnSuspend

    val unSuspendRequestDataIsNull: SDKEvent =
        Event("unsuspend request data is null")

    val unSuspendRequestSuccessful: SDKEvent =
        Event("successful request: push/unsuspend")

    val unSuspendRequestFailed: SDKEvent =
        Event("failed request: push/unsuspend")

    // Push display
    val pushDataIsNull: SDKEvent =
        Event("push data is null")

    val notificationErr: SDKEvent =
        Event("notification error")

    val channelNotCreated: SDKEvent =
        Event("notification channel not created")

    val pushPermissionDenied: SDKEvent =
        Event("notification permission denied")

    val pushIsPosted: SDKEvent =
        Event("push notification is posted")

    val acPush: SDKEvent =
        Event("Altcraft push notification received")

    val notAcPush: SDKEvent =
        Event("received a notification unrelated to the Altcraft Platform")

    val duplicatePushMessage: SDKEvent =
        Event("duplicate push message: processing and display skipped")

    val receiverRedefined: SDKEvent =
        Event("push receiver redefined")

    val errorImgLoad: SDKEvent =
        Event("error loading push image")

    val successImgLoad: SDKEvent =
        Event("push image loaded successfully")
}
