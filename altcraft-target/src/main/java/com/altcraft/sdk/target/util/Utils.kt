package com.altcraft.sdk.target.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Provides utility functions used by the Target module.
 */
internal object Utils {

    /**
     * Adds a short delay when the application is running in the background.
     *
     * The delay gives the application process time to stabilize before
     * pending target operations are processed.
     */
    suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(1.5.seconds)
    }

    /**
     * Returns mobile-event timezone offset in minutes as a signed integer.
     *
     * On error: returns 0.
     */
    fun getTimeZone(): Int {
        return try {
            val timeZone: TimeZone = TimeZone.getDefault()
            val offsetInMillis: Int = timeZone.rawOffset
            val hours: Int = offsetInMillis / (1000 * 60 * 60)
            val minutes: Int = (offsetInMillis / (1000 * 60)) % 60
            -(hours * 60 + minutes)
        } catch (e: Exception) {
            Log.error("getTimeZone", e)
            0
        }
    }
}
