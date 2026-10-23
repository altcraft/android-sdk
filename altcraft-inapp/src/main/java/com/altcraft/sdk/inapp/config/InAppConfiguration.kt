package com.altcraft.sdk.inapp.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.Constants.MODULE_NAME
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Internal configuration for the in-app module.
 *
 * Stores in-app settings provided through the public
 * `AltcraftConfiguration.Builder.inAppConfig(...)` API.
 */
internal class InAppConfiguration(

    /**
     * Enables automatic requests of in-app placements.
     *
     * When `false`, placements are requested only manually
     * through `AltcraftSDK.inApp.getPlacements(...)`.
     */
    val inAppAutoRequest: Boolean,

    /**
     * Interval between automatic in-app placements requests.
     *
     * A new automatic request is not sent while this interval has
     * not yet elapsed since the last successful request.
     */
    val placementsRequestInterval: Duration,

    /**
     * Maximum number of in-app placements kept in local storage.
     *
     * When a response contains more placements than this limit,
     * only the newest [maxPlacements] entries are stored.
     */
    val maxPlacements: Int
) : ModuleConfiguration {

    /**
     * Interval in milliseconds derived from [placementsRequestInterval].
     */
    val placementsRequestIntervalMs = placementsRequestInterval.toLong(
        DurationUnit.MILLISECONDS
    )

    /**
     * `true` when [maxPlacements] is below the minimum allowed value.
     */
    val maxPlacementsSizeInvalid = maxPlacements < 1

    /**
     * `true` when [placementsRequestInterval] is non-positive.
     */
    val requestIntervalInvalid = placementsRequestIntervalMs <= 0L

    /**
     * Indicates whether the in-app configuration is valid.
     */
    val valid = !maxPlacementsSizeInvalid && !requestIntervalInvalid

    /**
     * Throws an SDK exception with the specific invalid reason.
     */
    fun error() {
        when {
            requestIntervalInvalid ->
                exception(ModuleEvents.requestIntervalInvalid)

            maxPlacementsSizeInvalid ->
                exception(ModuleEvents.maxPlacementsInvalid)
        }
    }

    /**
     * Unique in-app module identifier.
     */
    override val moduleId: String = MODULE_NAME

    /**
     * In-app configuration database entity.
     */
    val entity = InAppConfigEntity(
        placementsRequestInterval = placementsRequestIntervalMs,
        inAppAutoRequest = inAppAutoRequest,
        maxPlacements = maxPlacements
    )
}