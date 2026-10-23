package com.altcraft.sdk.profile.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.events.SDKEvent

/**
 * Events emitted by the Profile module.
 */
@Keep
object ModuleEvents {

    /** SDK event implementation carrying the message text. */
    private data class Event(
        override val message: String
    ) : SDKEvent

    val requestDataIsNull: SDKEvent =
        Event("profile update request data is null")

    val retryLimit: SDKEvent =
        Event("profile update retry limit")

    val requestSuccessful: SDKEvent =
        Event("successful request: profile/update")

    val requestFailed: SDKEvent =
        Event("failed request: profile/update")
}
