package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.platform.authenticate.external.RTokenChange
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.room.TestRoom

/**
 * ConfigSetupInstrumentedEventsTest
 *
 * Positive scenarios:
 * - test_1: setConfig writes into real in-memory Room DB and updates the cache.
 * - test_4: setConfig overwrites an existing configuration row when values are changed (persisted
 * record reflects the latest config).
 *
 * Negative scenarios:
 * - test_2: setConfig when AltcraftConfiguration.toEntity() throws → exception propagates and
 * the cache is not updated.
 *
 * Behavioral scenarios:
 * - test_3: getConfig (background branch) returns the row from the real DB.
 * - test_5: rToken change notifies optional modules via ModuleRegistry.notifyRTokenChanged,
 * while a stable rToken does not.
 */
@RunWith(AndroidJUnit4::class)
class ConfigSetupInstrumentedEventsTest {

    private lateinit var appContext: Context
    private lateinit var db: TestRoom
    private lateinit var realConfigDao: ConfigurationDao

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        realConfigDao = db.configurationDao()

        mockkObject(DB.Companion)
        val sdkDBWrapper = mockk<DB>(relaxed = true)
        every { DB.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.configurationDao() } returns realConfigDao
        every { sdkDBWrapper.dao.config } returns realConfigDao

        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns false

        ConfigManager.conf = null
    }

    @After
    fun tearDown() {
        unmockkAll()
        db.close()
    }

    private fun cfg(
        rToken: String?,
        apiUrl: String = API_URL,
    ) = ConfigurationEntity(
        id = 0,
        apiUrl = apiUrl,
        rToken = rToken,
        appInfo = null
    )

    /** - test_1: setConfig writes to real DB and updates cache. */
    @Test
    fun setConfig_writesToDb_andUpdatesCache() = runBlocking {
        val newEntity = cfg(rToken = "rt-NEW")
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } returns newEntity

        ConfigManager.setConfig(appContext, newConfig)

        assertEquals(MSG_CACHE_UPDATED, newEntity, ConfigManager.conf)

        val fromDb = realConfigDao.get()
        assertNotNull(MSG_DB_MATCH, fromDb)
        assertEquals(MSG_DB_MATCH, newEntity.copy(id = fromDb!!.id), fromDb)
    }

    /** - test_2: setConfig propagates the exception when toEntity() throws. */
    @Test
    fun setConfig_propagatesException_whenToEntityThrows() = runBlocking {
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } throws IllegalStateException("boom")

        try {
            ConfigManager.setConfig(appContext, newConfig)
            fail("setConfig must propagate the toEntity() exception")
        } catch (expected: IllegalStateException) {
            assertEquals("boom", expected.message)
        }

        assertNull("Cache must remain empty on failure", ConfigManager.conf)
    }

    /** - test_3: getConfig (background) returns configuration from real DB. */
    @Test
    fun getConfig_returnsFromDb_inBackground() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns false

        val pre = cfg(rToken = "rt-NEW")
        realConfigDao.insert(pre)
        ConfigManager.conf = null

        val got = ConfigManager.getConfig(appContext)
        val fromDb = realConfigDao.get()

        assertNotNull(MSG_NOT_NULL, got)
        assertEquals(MSG_GET_CONFIG_MATCH, fromDb, got)
    }

    /** - test_4: setConfig overwrites existing configuration when values change (update path). */
    @Test
    fun setConfig_overwrites_whenChanged() = runBlocking {
        val old = cfg(rToken = "rt-NEW", apiUrl = "https://api.old.example.com")
        realConfigDao.insert(old)

        val updated = cfg(rToken = "rt-NEW", apiUrl = "https://api.new.example.com")
        val updateConfig = mockk<AltcraftConfiguration>()
        every { updateConfig.toEntity() } returns updated

        ConfigManager.setConfig(appContext, updateConfig)

        assertEquals(MSG_CACHE_UPDATED, updated, ConfigManager.conf)

        val fromDb = realConfigDao.get()
        assertNotNull(fromDb)
        assertEquals(MSG_OVERWRITE_OK, updated.copy(id = fromDb!!.id), fromDb)
    }

    /** - test_5: rToken change notifies optional modules; stable rToken does not. */
    @Test
    fun rTokenChange_notifiesModules_onlyWhenChanged() = runBlocking {
        mockkObject(ModuleRegistry)
        coVerify(exactly = 0) { ModuleRegistry.notifyRTokenChanged(any()) }

        val old = cfg(rToken = "rt-OLD")
        realConfigDao.insert(old)

        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.rToken } returns "rt-NEW"

        RTokenChange.notifyIfRTokenChanged(appContext, newConfig)

        coVerify(exactly = 1) { ModuleRegistry.notifyRTokenChanged(appContext) }
    }

    /** - test_6: stable rToken does not notify optional modules. */
    @Test
    fun rTokenChange_sameRToken_doesNotNotifyModules() = runBlocking {
        mockkObject(ModuleRegistry)

        val initial = cfg(rToken = "rt-SAME")
        realConfigDao.insert(initial)
        ConfigManager.conf = initial

        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.rToken } returns "rt-SAME"

        RTokenChange.notifyIfRTokenChanged(appContext, newConfig)

        coVerify(exactly = 0) { ModuleRegistry.notifyRTokenChanged(any()) }
    }

    companion object {
        private const val API_URL = "https://api.example.com"
        private const val MSG_CACHE_UPDATED = "In-memory cache must be updated"
        private const val MSG_DB_MATCH = "DB value must match the expected configuration"
        private const val MSG_GET_CONFIG_MATCH =
            "getConfig must return the configuration stored in DB"
        private const val MSG_NOT_NULL = "Value must not be null"
        private const val MSG_OVERWRITE_OK =
            "Persisted configuration must reflect the latest values"
    }
}
