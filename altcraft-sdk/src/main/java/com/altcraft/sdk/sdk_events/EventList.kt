package com.altcraft.sdk.sdk_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

/**
 * Contains integer codes and string templates used for creating and categorizing SDK event messages.
 *
 * Groups:
 * - 2xx: informational / success events
 * - 4xx: non-retryable errors (operation cannot be repeated automatically)
 * - 5xx: retryable errors (SDK should retry automatically)
 */
@Suppress("SpellCheckingInspection")
internal object EventList {

    /** 200–206 — General SDK events */
    val configIsSet = 200 to "SDK configuration is installed"
    val pushProviderSet = 201 to "push provider set: "
    val notAltcraftPush = 202 to "received a notification unrelated to the Altcraft Platform"
    val altcraftPush = 203 to "received Altcraft push notification"
    val pushIsPosted = 204 to "push is posted"
    val sdkCleared = 205 to "SDK data has been cleared"
    val initAwait = 206 to "waiting for SDK initialization to complete"

    /**
     * 230–237 — Success codes for completed server requests:
     *  - 230 → push subscribe request succeeded
     *  - 231 → push suspend request succeeded
     *  - 232 → push unsubscribed request succeeded
     *  - 233 → push update request succeeded
     *  - 234 → push unsuspend request succeeded
     *  - 235 → profile status request succeeded
     *  - 236 → push event delivered successfully
     *  - 237 → mobile event delivered successfully
     */

    // 4xx — used for exceptions after which the operation cannot be repeated automatically.

    /** 401–409 — Common internal non-retryable errors */
    val configIsNotSet = 401 to "the configuration is not set"
    val userTagIsNullE = 402 to "userTag is null. It is impossible to identify the user"
    val sdkInitWaitingExpired = 404 to "SDK initialization timeout has expired"
    val mobileEventPartsIsNull = 405 to "mobile event parts is null"

    /**
     * 430–437 — SDK-to-server request errors without automatic retry
     * (4xx or non-retryable request group):
     *  - 430 → subscribe request failed
     *  - 431 → suspend request failed
     *  - 432 → unsubscribed request failed
     *  - 433 → update request failed
     *  - 434 → unsuspend request failed
     *  - 435 → status request failed
     *  - 436 → push event delivery failed
     *  - 437 → mobile event request failed
     */

    /** 450–459 — Notification-related errors */
    val pushDataIsNull = 450 to "push data is null"
    val uidIsNull = 451 to "uid in the push data is null or empty, it is impossible to send " +
            "a push event to the server."
    val errorImgLoad = 452 to "error uploading the notification image"
    val notificationErr = 453 to "couldn't create notification"
    val channelNotCreated = 454 to "the notification channel has not been created"

    /** 470–479 — Invalid values */
    val invalidPushProvider = 471 to "invalid provider. Available - android-firebase," +
            " android-huawei, android-rustore."
    val fieldsIsObjects = 472 to "invalid customFields: not all values are primitives"

    /**
     * 422–423 — Missing request payloads (non-retryable)
     * These errors indicate missing request data for which the SDK does not attempt to recollect
     * or retry.
     */
    val unSuspendRequestDataIsNull = 422 to "unsuspend request data is null"
    val profileRequestDataIsNull = 423 to "profile request data is null"

    /**
     * 480–485 — The request was removed from the database due to an excessive number of
     * repeated executions.
     *
     * 480 - subscribe retry limit
     * 484 - push event retry limit
     * 485 - mobile event retry limit
     */

    // 5xx — used for exceptions after which the operation should be retried automatically.

    /** 501–506 — Missing SDK state or environment issues */
    val configIsNull = 501 to "config data is null"
    val pushTokenIsNull = 502 to "current push token is null"
    val userTagIsNull = 503 to "userTag is null. It is impossible to identify the user"
    val permissionDenied = 504 to "no permission to send notifications"
    val noInternetConnect = 505 to "no internet connection, retry when connection is restored"
    val notUpdated = 506 to "failed to update the push token. The subscription request was rejected."

    /**
     * 520–529 — Missing or null request payloads (retryable)
     * These indicate that required data for a request is not available at the time of execution.
     * The SDK will attempt to collect and resend the data automatically.
     */
    val pushSubscribeRequestDataIsNull = 520 to "push subscribe request data is null"
    val tokenUpdateRequestDataIsNull = 523  to "token update request data is null"
    val pushEventRequestDataIsNull = 526 to "push event request data is null"
    val mobEventRequestDataIsNull = 527 to "mobile event request data is null"
    val profileUpdateRequestDataIsNull = 528 to "profile update request data is null"

    /**
     * 530–537 — SDK-to-server request errors with automatic retry by the SDK (5xx HTTP):
     *  - 530 → subscribe request failed (retryable)
     *  - 531 → suspend request failed (retryable)
     *  - 532 → unsubscribed request failed (retryable)
     *  - 533 → update request failed (retryable)
     *  - 536 → push event delivery failed (retryable)
     *  - 537 → mobile event delivery failed (retryable)
     */

    /** 540–545 — Authorization-related errors */
    val jwtIsNull = 540 to "JWT token is null"
    val matchingIsNull = 541 to "matching mode is null"
    val jwtTooLarge = 542 to "JWT payload exceeds allowed size (16 KB limit):" +
            " input rejected to prevent DoS."
    val payloadIsMissing = 543 to "JWT does not contain a payload"
    val authDataIsNull = 544 to "auth data is null"
    val matchingIdIsNull = 545 to "matching claim does not contain a matching ID"

    /** 560 — Response data issues */
    val responseDataIsNull = 560 to "response data is null"
}
