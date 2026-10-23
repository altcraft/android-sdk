@file:Suppress("SpellCheckingInspection")

package test.inapp.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.inapp.config.InAppConfiguration
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_MAX_PLACEMENTS
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_PLACEMENTS_REQ_INTERVAL_MS
import com.altcraft.sdk.inapp.data.Constants.MODULE_NAME
import com.altcraft.sdk.inapp.events.ModuleEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

/**
 * InAppConfigurationDefaultsTest
 *
 * Positive scenarios:
 *  - test_1: the default constructor uses automatic request, 30-minute interval and 500 max placements.
 *  - test_2: custom constructor values are preserved.
 *  - test_3: entity maps all configuration fields into a Room entity (interval as milliseconds).
 *  - test_5: valid is true for a default configuration.
 *  - test_6: valid is true for a custom configuration with valid values.
 *
 * Negative scenarios:
 *  - test_4: maxPlacements of 0 sets maxPlacementsIsBelowOne to true.
 *  - test_7: a negative placementsRequestInterval sets the flag.
 *  - test_10: zero placementsRequestInterval sets the flag.
 *  - test_11: error() with maxPlacements < 1 throws the correct event.
 *  - test_12: error() with interval <= 0 throws the correct event.
 */
@OptIn(ExperimentalTime::class)
class InAppConfigurationDefaultsTest {

    /** test_1: default configuration uses automatic request, 30-minute interval and 500 max placements. */
    @Test
    fun test_1_defaultConstructor_usesSensibleDefaults() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertTrue(config.inAppAutoRequest)
        assertEquals(MODULE_NAME, config.moduleId)
        assertEquals(30.minutes, config.placementsRequestInterval)
        assertEquals(500, config.maxPlacements)
        assertEquals(30, config.placementsRequestInterval.toLong(DurationUnit.MINUTES))
    }

    /** test_2: custom configuration values are preserved. */
    @Test
    fun test_2_customConstructor_preservesAllValues() {
        val config = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = 10.minutes,
            maxPlacements = 300
        )

        assertFalse(config.inAppAutoRequest)
        assertEquals(10.minutes, config.placementsRequestInterval)
        assertEquals(300, config.maxPlacements)
    }

    /** test_3: entity maps all configuration fields into a Room entity (interval as milliseconds). */
    @Test
    fun test_3_entity_mapsAllFields() {
        val entity = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = 15.minutes,
            maxPlacements = 250
        ).entity

        assertFalse(entity.inAppAutoRequest)
        assertEquals(15.minutes.toLong(DurationUnit.MILLISECONDS), entity.placementsRequestInterval)
        assertEquals(250, entity.maxPlacements)
        assertEquals(0, entity.id)
    }

    /** test_4: maxPlacements of 0 sets maxPlacementsIsBelowOne to true. */
    @Test
    fun test_4_zeroMaxPlacements_setsBelowOneFlag() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = 0
        )

        assertFalse(config.valid)
        assertTrue(config.maxPlacementsSizeInvalid)
        assertFalse(config.requestIntervalInvalid)
    }

    /** test_5: valid is true for a default configuration. */
    @Test
    fun test_5_validForDefaultConfig() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertTrue(config.valid)
    }

    /** test_6: valid is true for a custom configuration with valid values. */
    @Test
    fun test_6_validForCustomValidConfig() {
        val config = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = 10.minutes,
            maxPlacements = 300
        )

        assertTrue(config.valid)
    }

    /** test_7: a negative placementsRequestInterval sets the flag. */
    @Test
    fun test_7_negativeInterval_setsNonPositiveFlag() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = (-5).minutes,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertFalse(config.valid)
        assertTrue(config.requestIntervalInvalid)
        assertFalse(config.maxPlacementsSizeInvalid)
    }

    /** test_10: zero placementsRequestInterval sets the flag. */
    @Test
    fun test_10_zeroInterval_setsNonPositiveFlag() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = 0.minutes,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertFalse(config.valid)
        assertTrue(config.requestIntervalInvalid)
        assertFalse(config.maxPlacementsSizeInvalid)
    }

    /** test_11: error() with maxPlacements < 1 throws the maxPlacements event. */
    @Test
    fun test_11_error_maxPlacementsBelowOne_correctEvent() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = 0
        )

        try {
            config.error()
            fail("Expected SDKException")
        } catch (e: SDKException.SDKException) {
            assertEquals(ModuleEvents.maxPlacementsInvalid.message, e.message)
        }
    }

    /** test_12: error() with interval <= 0 throws the interval event. */
    @Test
    fun test_12_error_intervalNonPositive_correctEvent() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = (-5).minutes,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        try {
            config.error()
            fail("Expected SDKException")
        } catch (e: SDKException.SDKException) {
            assertEquals(ModuleEvents.requestIntervalInvalid.message, e.message)
        }
    }
}
