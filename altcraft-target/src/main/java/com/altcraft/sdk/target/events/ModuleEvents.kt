package com.altcraft.sdk.target.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.events.SDKEvent

/**
 * Events emitted by the Target module.
 */
@Keep
object ModuleEvents {
    /** SDK event implementation carrying the message text. */
    private data class Event(
        override val message: String
    ) : SDKEvent

    val partsIsNull: SDKEvent =
        Event("target request parts is null")

    val retryLimit: SDKEvent =
        Event("target request retry limit")

    val invalidPayload: SDKEvent =
        Event("invalid target payload: not all values are primitives")

    val requestDataIsNull: SDKEvent =
        Event("target request data is null")

    val requestSuccessful: SDKEvent =
        Event("successful target request: event/post")

    val requestFailed: SDKEvent =
        Event("failed target request: event/post")
}
