package com.altcraft.sdk.inapp.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.events.SDKEvent

/**
 * Events emitted by the In-App module.
 */
@Keep
object ModuleEvents {

    /** SDK event implementation carrying the message text. */
    private data class Event(
        override val message: String
    ) : SDKEvent

    // Configuration

    val maxPlacementsInvalid: SDKEvent =
        Event("Invalid in-app configuration: maxPlacements must be >= 1")

    val requestIntervalInvalid: SDKEvent =
        Event("Invalid in-app configuration: placementsRequestInterval must be > Duration.ZERO")

    // In-app subscription

    val subscribeRetryLimit: SDKEvent =
        Event("in-app subscribe retry limit")

    val subscribeRequestDataIsNull: SDKEvent =
        Event("in-app subscribe request data is null")

    val subscribeRequestSuccessful: SDKEvent =
        Event("successful request: inapp/subscribe")

    val subscribeRequestFailed: SDKEvent =
        Event("failed request: inapp/subscribe")

    val unsubscribeRequestSuccessful: SDKEvent =
        Event("successful request: inapp/unsubscribe")

    val unsubscribeRequestFailed: SDKEvent =
        Event("failed request: inapp/unsubscribe")

    val invalidCustomFields: SDKEvent =
        Event("invalid custom fields: not all values are primitives")

    // In-app subscription status

    val statusRequestDataIsNull: SDKEvent =
        Event("in-app status request data is null")

    val statusRequestSuccessful: SDKEvent =
        Event("successful request: inapp/status")

    val statusRequestFailed: SDKEvent =
        Event("failed request: inapp/status")

    // In-app event

    val eventRetryLimit: SDKEvent =
        Event("in-app event retry limit")

    val eventRequestDataIsNull: SDKEvent =
        Event("in-app event request data is null")

    val smidIsNull: SDKEvent =
        Event("send message ID is null")

    val eventRequestSuccessful: SDKEvent =
        Event("successful request: inapp/event")

    val eventRequestFailed: SDKEvent =
        Event("failed request: inapp/event")

    // In-app placements

    val placementsRequestDataIsNull: SDKEvent =
        Event("in-app placements request data is null")

    val placementsRequestSuccessful: SDKEvent =
        Event("successful request: inapp/placements")

    val placementsRequestFailed: SDKEvent =
        Event("failed request: inapp/placements")

    val placementsLimitExceeded: SDKEvent =
        Event("in-app placements limit exceeded")

    // In-app static and content

    val invalidStaticField: SDKEvent =
        Event("invalid in-app static field")

    val inAppStaticErrorLoad: SDKEvent =
        Event("error loading in-app notification static resources")

    val inAppContentErrorLoad: SDKEvent =
        Event("error loading in-app notification content")
}
