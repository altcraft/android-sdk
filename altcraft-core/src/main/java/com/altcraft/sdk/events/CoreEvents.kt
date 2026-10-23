package com.altcraft.sdk.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Contains string templates used for creating and categorizing core SDK event messages.
 */
@Keep
object CoreEvents {

    /** SDK event implementation carrying the message text. */
    private data class Event(
        override val message: String
    ) : SDKEvent

    // General SDK events

    val configIsSet: SDKEvent =
        Event("SDK configuration is installed")

    val sdkCleared: SDKEvent =
        Event("SDK data has been cleared")

    val notAuthenticatedError: SDKEvent =
        Event("User is not authenticated")

    val userLogOut: SDKEvent =
        Event("User logged out. Anonymous session started.")

    // Internal non-retryable errors

    internal val apiUrlIsBlank: SDKEvent =
        Event("Invalid configuration: apiUrl must not be empty or blank.")

    internal val rTokenIsBlank: SDKEvent =
        Event("Invalid configuration: RToken must not be empty or blank.")

    internal val configIsNotSet: SDKEvent =
        Event("the configuration is not set")

    internal val sdkInitWaitingExpired: SDKEvent =
        Event("SDK initialization timeout has expired")

    val roomMigrationError: SDKEvent =
        Event("Room database migration failed. The local SDK database will be recreated.")

    // Missing request payloads (non-retryable)

    internal val authorizeRequestDataIsNull: SDKEvent =
        Event("authorize request data is null")

    // Retry limit events

    internal val authRequestRetryLimit: SDKEvent =
        Event("auth request retry limit")

    // Missing SDK state or environment issues

    internal val configIsNull: SDKEvent =
        Event("config data is null")

    internal val userIsNull: SDKEvent =
        Event("userTag is null. It is impossible to identify the user")

    @PublishedApi
    internal val noInternetConnect: SDKEvent =
        Event("no internet connection, retry when connection is restored")

    internal val responseDataIsNull: SDKEvent =
        Event("response data is null")

    internal val jwtTokenIsNull: SDKEvent =
        Event("JWT token is null")

    internal val jwtDecodeFailed: SDKEvent =
        Event("JWT payload decoding failed")

    internal val jwtTooLarge: SDKEvent =
        Event("JWT payload exceeds allowed size (16 KB limit): input rejected to prevent DoS")

    internal val payloadIsMissing: SDKEvent =
        Event("JWT does not contain a payload")

    internal val matchingIsNull: SDKEvent =
        Event("matching mode is null")

    internal val matchingIdIsNull: SDKEvent =
        Event("matching claim does not contain a matching ID")

    internal val internalAuthIsNull: SDKEvent =
        Event("internal auth data is null")

    internal val authHeaderIsNull: SDKEvent =
        Event("auth header is null")

    // Successful api requests

    val authenticateSuccessful: SDKEvent =
        Event("successful request: profile/authenticate")

    // Failed api requests

    val pushStatusRequestFailed: SDKEvent =
        Event("failed request: push/status")

    internal val authenticateFailed: SDKEvent =
        Event("failed request: profile/authenticate")
}
