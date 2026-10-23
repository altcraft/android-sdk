package com.altcraft.sdk.profile.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Provides utility functions used by the Profile module.
 */
internal object Utils {

    /**
     * Adds a short delay when the application is running in the background.
     *
     * The delay gives the application process time to stabilize before
     * pending profile operations are processed.
     */
    suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(1.5.seconds)
    }
}
