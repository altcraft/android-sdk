package com.altcraft.sdk.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Describes an event emitted by Altcraft SDK or an optional SDK module.
 */
interface SDKEvent {

    /**
     * event message.
     */
    val message: String
}