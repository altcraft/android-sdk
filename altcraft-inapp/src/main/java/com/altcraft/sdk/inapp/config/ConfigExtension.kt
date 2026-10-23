package com.altcraft.sdk.inapp.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_MAX_PLACEMENTS
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_PLACEMENTS_REQ_INTERVAL_MS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adds in-app module configuration to the SDK builder.
 *
 * @param inAppAutoRequest Enables automatic requests of in-app placements.
 * @param placementsRequestInterval Interval between automatic placement requests.
 * @param maxPlacements Maximum number of placements kept in local storage.
 * @return Builder for chained calls.
 */
fun AltcraftConfiguration.Builder.inAppConfig(
    inAppAutoRequest: Boolean = true,
    placementsRequestInterval: Duration =
        DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
    maxPlacements: Int = DEFAULT_MAX_PLACEMENTS
): AltcraftConfiguration.Builder = addModuleConfiguration(
    InAppConfiguration(
        placementsRequestInterval = placementsRequestInterval,
        inAppAutoRequest = inAppAutoRequest,
        maxPlacements = maxPlacements
    )
)