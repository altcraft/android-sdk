@file:Suppress("SpellCheckingInspection")

package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * ConfigSetupCacheTest
 *
 * Positive scenarios:
 *  - test_1: getConfig() returns cached in-memory configuration without hitting the database.
 *  - test_2: getConfig() reads from the database when the cache is empty and caches the result.
 *
 * Negative scenarios:
 *  - test_3: getConfig() returns null when neither the cache nor the database contains a config.
 *  - test_5: validateConfig() throws for an invalid configuration.
 *
 * Write scenarios:
 *  - test_4: setConfig() inserts into the database and updates the cache.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigSetupCacheTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var context: Context
    private lateinit var dao: ConfigurationDao
    private lateinit var db: DB

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        context = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        db = mockk(relaxed = true)

        mockkObject(DB)
        every { DB.getDb(any()) } returns db
        every { db.configurationDao() } returns dao
        every { db.dao.config } returns dao

        ConfigManager.conf = null
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun cfg() = ConfigurationEntity(
        id = 0,
        apiUrl = API_URL,
        rToken = RTOKEN,
        appInfo = null
    )

    /** - test_1: getConfig() returns cached configuration without touching the database. */
    @Test
    fun `getConfig returns cached configuration`() = scope.runTest {
        val expected = cfg()
        ConfigManager.conf = expected
        coEvery { dao.get() } returns null

        val result = ConfigManager.getConfig(context)

        assertEquals(MSG_CACHED, expected, result)
        coVerify(exactly = 0) { dao.get() }
    }

    /** - test_2: getConfig() reads the database when the cache is empty and caches the result. */
    @Test
    fun `getConfig reads from database and caches when cache is empty`() = scope.runTest {
        val fromDb = cfg()
        coEvery { dao.get() } returns fromDb

        val result = ConfigManager.getConfig(context)

        assertEquals(MSG_DB, fromDb, result)
        assertEquals(MSG_CACHED_AFTER_READ, fromDb, ConfigManager.conf)
        coVerify(exactly = 1) { dao.get() }
    }

    /** - test_3: getConfig() returns null when neither cache nor database contains a config. */
    @Test
    fun `getConfig returns null when nothing is stored`() = scope.runTest {
        coEvery { dao.get() } returns null

        val result = ConfigManager.getConfig(context)

        assertNull(MSG_NULL, result)
    }

    /** - test_4: setConfig() inserts into DB and updates the cache. */
    @Test
    fun `setConfig inserts and caches`() = scope.runTest {
        val entity = cfg()
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } returns entity

        coEvery { dao.get() } returns null

        ConfigManager.setConfig(context, newConfig)

        assertEquals(MSG_SET_CACHE, entity, ConfigManager.conf)
        coVerify(exactly = 1) { dao.insert(entity) }
    }

    /** - test_5: validateConfig() throws for an invalid (empty apiUrl) configuration. */
    @Test
    fun `validateConfig throws when apiUrl is blank`() = scope.runTest {
        val invalid = mockk<AltcraftConfiguration>()
        every { invalid.valid } returns false
        every { invalid.error() } throws IllegalStateException("invalid config")

        try {
            ConfigManager.validateConfig(context, invalid)
            fail(MSG_INVALID)
        } catch (expected: IllegalStateException) {
            assertEquals(MSG_INVALID_MESSAGE, "invalid config", expected.message)
        }
        coVerify(exactly = 1) { dao.delete() }
    }

    private companion object {
        private const val MSG_CACHED = "Expected cached configuration to be returned"
        private const val MSG_DB = "Expected configuration stored in DB to be returned"
        private const val MSG_CACHED_AFTER_READ = "Expected DB value to be cached after read"
        private const val MSG_NULL = "Expected null when nothing is stored"
        private const val MSG_SET_CACHE = "Expected setConfig to update the in-memory cache"
        private const val MSG_INVALID = "Expected validateConfig to throw for an invalid configuration"
        private const val MSG_INVALID_MESSAGE = "Expected validateConfig exception message to be preserved"

        private const val API_URL = "https://api.example.com"
        private const val RTOKEN = "rt-NEW"
    }
}
