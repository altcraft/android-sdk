package com.altcraft.sdk.inapp.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

/**
 * Provides utility functions used by the in-app module.
 */
internal object Utils {

    /**
     * Adds a short delay when the application is running in the background.
     *
     * The delay gives the application process time to stabilize before
     * pending in-app operations are processed.
     */
    suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(1.5.seconds)
    }

    /**
     * Converts milliseconds to whole minutes, rounding up.
     *
     * @return Duration in minutes.
     */
    internal fun Long.toMinutesCeil(): Long = ceil(this / 60000.0).toLong()
}
