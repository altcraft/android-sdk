package com.altcraft.sdk.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

/**
 * Object containing string constants used for creating event messages.
 *
 * The `EventMessage` object provides predefined string constants that represent various event
 * messages.
 */
@Suppress("SpellCheckingInspection")
internal object EventList {

    /** 200 — General SDK events */
    val configIsSet = 200 to "SDK configuration is installed"
    val pushProviderSet = 201 to "push provider set: "
    val notAltcraftPush = 202 to "received a notification unrelated to the" +
            " Altcraft Platform"
    val altcraftPush = 203 to "received Altcraft push notification"
    val pushIsPosted = 204 to "push is posted"
    val sdkCleared = 205 to "SDK data has been cleared"
    val initAwait = 206 to "waiting for SDK initialization to complete"

    /** 220 — receiver override event */

    /**
     * 230–234 — Success codes for completed server requests:
     *  - 230 → subscribe request succeeded
     *  - 231 → token update request succeeded
     *  - 232 → push event delivered successfully
     *  - 233 -> unsuspend request succeeded
     *  - 234 → status request succeeded
     */

    //400 - used for exceptions, after which the operation cannot be repeated.

    /**common internal error */
    val configIsNotSet = 401 to "the configuration is not set"
    val userTagIsNullE = 402 to "userTag is null. It is impossible to identify the user"
    val unsupportedEntityType = 403 to "Unsupported entity type"
    val sdkInitWaitingExpired = 404 to "The SDK initialization timeout has expired"

    /**
     * 430–434 — SDK-to-server request errors without automatic retry
     *  - 430 → subscribe request failed
     *  - 431 → token update request failed
     *  - 432 → push event delivery failed
     *  - 433 → unsuspend request failed
     *  - 434 → profile request failed
     */

    /**notification error*/
    val pushDataIsNull = 450 to "push data is null"
    val uidIsNull = 451 to "uid in the push data is null or empty," +
            " it is impossible to send a push event to the server."
    val errorImgLoad = 452 to "error uploading the notification image"
    val notificationErr = 453 to "couldn't create notification"
    val foregroundInfoIsNull = 454 to "foreground info is null"
    val channelNotCreated = 455 to "the notification channel has not been created"

    /**invalid value*/
    //470 - invalid config
    val invalidPushProvider = 471 to "invalid provider. Available - " +
            "android-firebase, android-huawei, android-rustore."
    val fieldsIsObjects = 472 to "invalid customFields: not all values are primitives"

    /**
     * 423–424 — Missing request payloads (no automatic retry)
     * These errors indicate missing request data for which the SDK does not attempt to recollect
     * or retry.
     */
    val unSuspendRequestDataIsNull = 423 to "unsuspend request data is null"
    val profileRequestDataIsNull = 424 to "profile request data is null"

    //500 - used for exception, after which the operation should be retried automatically

    /** 501–506 — Missing SDK state or environment issues */
    val configIsNull = 501 to "config data is null"
    val currentTokenIsNull = 502 to "current token data is null"
    val userTagIsNull = 503 to "userTag is null. It is impossible to identify the user"
    val permissionDenied = 504 to "no permission to send notifications"
    val noInternetConnect = 505 to "no internet connection, retry when connection is " +
            "restored"
    val bodyIsNotJson = 506 to "The response body is probably not a json string."

    /**
     * 520–529 — Missing or null request payloads
     * These indicate that required data for a request is not available at the time of execution.
     * The SDK will attempt to collect and resend the data automatically.
     */
    val commonDataIsNull = 529 to "common data is null"
    val pushSubscribeRequestDataIsNull = 520 to "push subscribe request data is null"
    val tokenUpdateRequestDataIsNull = 521 to "token update request data is null"
    val pushEventRequestDataIsNull = 522 to "push event request data is null"

    /**
     * 530–532 — SDK-to-server request errors with automatic retry by the SDK
     *  - 530 → subscribe request failed
     *  - 531 → token update request failed
     *  - 532 → push event delivery failed
     */

    /** 540–544 — Authorization-related errors */
    val jwtIsNull = 540 to "JWT token is null"
    val matchingIsNull = 541 to "matching mode is null"
    val authDataIsNull = 543 to "auth data is null"
    val matchingIdIsNull = 544 to "matching claim does not contain a matching ID"

    /** Response data issues */
    val responseDataIsNull = 560 to "response data is null"
}