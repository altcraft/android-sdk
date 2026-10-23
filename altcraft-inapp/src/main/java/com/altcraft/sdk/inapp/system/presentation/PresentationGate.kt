package com.altcraft.sdk.inapp.system.presentation

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Gate that invalidates pending In-App presentations when SDK data is cleared.
 *
 * Presentations capture the current epoch before they start waiting
 * (delayed tasks) or before display; if the epoch has moved by then,
 * the stale presentation is discarded instead of being shown.
 */
internal object PresentationGate {

    /** Current presentation epoch; increments invalidate all pending presentations. */
    @Volatile
    var epoch: Long = 0L
        private set

    /**
     * Invalidates all pending presentations.
     *
     * Delayed tasks and in-flight presentations from the previous
     * epoch are discarded instead of being shown.
     */
    fun invalidate() {
        epoch++
    }
}
