@file:Suppress("SpellCheckingInspection")

package test.inapp.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.inapp.config.ConfigManager
import com.altcraft.sdk.inapp.config.InAppConfiguration
import com.altcraft.sdk.inapp.config.inAppConfig
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_MAX_PLACEMENTS
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_PLACEMENTS_REQ_INTERVAL_MS
import com.altcraft.sdk.inapp.data.Constants.MODULE_NAME
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppConfigDao
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.infra.Environment
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

/**
 * InAppConfigurationTest
 *
 * Positive scenarios:
 *  - test_1: default configuration uses automatic placements request and the module id.
 *  - test_2: custom configuration value is preserved.
 *  - test_3: entity maps configuration fields into a Room entity.
 *  - test_4: ConfigManager returns the default configuration when nothing is stored.
 *  - test_5: ConfigManager persists and returns a custom configuration.
 *  - test_6: inAppConfig() extension registers the module in the SDK builder.
 *  - test_8: validateConfig is a no-op for a valid configuration.
 *  - test_9: validateConfig clears the cache for an invalid configuration.
 *
 * Negative scenarios:
 *  - test_7: validateConfig removes the stored configuration and throws for an invalid configuration.
 */
@OptIn(ExperimentalTime::class)
class InAppConfigurationTest {

    private lateinit var context: Context
    private lateinit var env: Environment
    private lateinit var configDao: InAppConfigDao

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        configDao = mockk(relaxed = true)

        val db = mockk<DB>(relaxed = true)
        every { Environment.create(any()) } returns env
        every { env.room } returns db
        every { db.dao.config } returns configDao
    }

    @After
    fun tearDown() {
        unmockkAll()
        ConfigManager.clearCache()
    }

    /** test_1: default configuration enables automatic placements request. */
    @Test
    fun test_1_defaultConstructor_usesAutoRequestTrueAndModuleId() {
        val config = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertTrue(config.inAppAutoRequest)
        assertEquals(MODULE_NAME, config.moduleId)
        assertTrue(config.valid)
    }

    /** test_2: custom configuration value is preserved. */
    @Test
    fun test_2_customConstructor_preservesAutoRequestFalse() {
        val config = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        )

        assertFalse(config.inAppAutoRequest)
        assertEquals(MODULE_NAME, config.moduleId)
        assertTrue(config.valid)
    }

    /** test_3: entity maps configuration fields into a Room entity. */
    @Test
    fun test_3_entity_mapsConfigurationFields() {
        val entity = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        ).entity

        assertFalse(entity.inAppAutoRequest)
        assertEquals(0, entity.id)

        val defaultEntity = InAppConfiguration(
            inAppAutoRequest = true,
            placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
            maxPlacements = DEFAULT_MAX_PLACEMENTS
        ).entity

        assertTrue(defaultEntity.inAppAutoRequest)
    }

    /** test_4: ConfigManager returns the default configuration (all three fields) when nothing is stored. */
    @Test
    fun test_4_getConfig_returnsDefaultWhenNothingStored() = runTest {
        ConfigManager.clearCache()
        coEvery { configDao.get() } returns null

        val config = ConfigManager.getConfig(context)

        assertTrue(config.inAppAutoRequest)
        assertEquals(0, config.id)
        assertEquals(DEFAULT_PLACEMENTS_REQ_INTERVAL_MS, config.placementsRequestInterval)
        assertEquals(DEFAULT_MAX_PLACEMENTS, config.maxPlacements)
    }

    /** test_5: ConfigManager persists and returns a custom configuration (all three fields). */
    @Test
    fun test_5_setConfig_thenGetConfig_returnsStoredValue() = runTest {
        ConfigManager.clearCache()

        val config = InAppConfiguration(
            inAppAutoRequest = false,
            placementsRequestInterval = 10.minutes,
            maxPlacements = 300
        )
        ConfigManager.setConfig(context, config)

        coVerify(exactly = 1) { configDao.insert(
            InAppConfigEntity(
                id = 0,
                inAppAutoRequest = false,
                placementsRequestInterval = 10.minutes.toLong(DurationUnit.MILLISECONDS),
                maxPlacements = 300
            )
        ) }

        val loaded = ConfigManager.getConfig(context)

        assertFalse(loaded.inAppAutoRequest)
        assertEquals(10.minutes.toLong(DurationUnit.MILLISECONDS), loaded.placementsRequestInterval)
        assertEquals(300, loaded.maxPlacements)
    }

    /** test_6: inAppConfig() extension registers the module in the SDK builder. */
    @Test
    fun test_6_inAppConfigExtension_registerModuleInBuilder() {
        val builder = AltcraftConfiguration.Builder(apiUrl = "https://api.altcraft.test")

        val returned = builder.inAppConfig(inAppAutoRequest = false)

        assertTrue(returned === builder)
    }

    /** test_7: validateConfig removes the stored configuration, clears the cache, and throws for an invalid configuration. */
    @Test
    fun test_7_validateConfig_invalidConfig_throwsSDKException() = runTest {
        ConfigManager.clearCache()
        // Seed the cache with an invalid config so the test is not affected by prior state.
        ConfigManager.setConfig(
            context,
            InAppConfiguration(
                inAppAutoRequest = true,
                placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                maxPlacements = 0
            )
        )

        try {
            ConfigManager.validateConfig(
                context,
                InAppConfiguration(
                    inAppAutoRequest = true,
                    placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                    maxPlacements = 0
                )
            )
            fail("Expected SDKException for an invalid configuration")
        } catch (e: SDKException.SDKException) {
            assertEquals(
                ModuleEvents.maxPlacementsInvalid.message,
                e.message
            )
        }

        coVerify(exactly = 1) { configDao.delete() }
        // The seed setConfig() inserted exactly once; validateConfig does not re-insert.
        coVerify(exactly = 1) { configDao.insert(any()) }
    }

    /** test_8: validateConfig is a no-op for a valid configuration. */
    @Test
    fun test_8_validateConfig_validConfig_isNoop() = runTest {
        ConfigManager.clearCache()

        ConfigManager.validateConfig(
            context,
            InAppConfiguration(
                inAppAutoRequest = true,
                placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                maxPlacements = DEFAULT_MAX_PLACEMENTS
            )
        )

        coVerify(exactly = 0) { configDao.delete() }
        coVerify(exactly = 0) { configDao.insert(any()) }
    }

    /** test_9: validateConfig clears the cache for an invalid configuration (even when nothing was stored). */
    @Test
    fun test_9_validateConfig_invalidConfig_clearsCache() = runTest {
        ConfigManager.clearCache()
        ConfigManager.setConfig(
            context,
            InAppConfiguration(
                inAppAutoRequest = true,
                placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                maxPlacements = DEFAULT_MAX_PLACEMENTS
            )
        )

        // After a valid config, the cache is non-null. The next invalid
        // validateConfig must clear it.
        ConfigManager.clearCache()
        ConfigManager.setConfig(
            context,
            InAppConfiguration(
                inAppAutoRequest = true,
                placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                maxPlacements = 0
            )
        )

        try {
            ConfigManager.validateConfig(
                context,
                InAppConfiguration(
                    inAppAutoRequest = true,
                    placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS.milliseconds,
                    maxPlacements = 0
                )
            )
            fail("Expected SDKException for an invalid configuration")
        } catch (e: SDKException.SDKException) {
            assertNotNull(e)
        }
    }
}
