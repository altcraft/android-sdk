package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * ConfigSetupGetConfigTest
 *
 * Positive scenarios:
 *  - test_1: When the DB holds a config and the cache is empty, getConfig() returns the DB value
 *  immediately (foreground does not block it) and refreshes the in-memory cache.
 *  - test_2: When the DB is empty, getConfig() retries with 1-second delays and returns the
 *  in-memory cached value as soon as it becomes available.
 *
 * Negative scenarios:
 *  - test_3: When neither the cache nor the DB ever contains a config, getConfig() returns null
 *  after 3 retries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigSetupGetConfigTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var context: Context
    private lateinit var dao: DAO
    private lateinit var db: SDKdb

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        context = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        db = mockk(relaxed = true)

        mockkObject(SDKdb)
        every { SDKdb.getDb(context) } returns db
        every { db.request() } returns dao

        mockkObject(Events)
        justRun { Events.event(any(), any()) }
        every { Events.error(any(), any()) } returns mockk(relaxed = true)

        coEvery { dao.getConfig() } returns cfg()

        ConfigSetup.configuration = null
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun cfg() = ConfigurationEntity(
        id = 0,
        icon = null,
        apiUrl = API_URL,
        rToken = RTOKEN,
        appInfo = null,
        pushReceiverModules = listOf("push-receiver-a"),
        providerPriorityList = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER),
        pushChannelName = "Altcraft",
        pushChannelDescription = "Altcraft notifications channel"
    )

    /** - test_1: DB value is returned immediately and the cache is refreshed. */
    @Test
    fun `getConfig returns db value immediately and refreshes cache`() = scope.runTest {
        val expected = cfg()
        val result = ConfigSetup.getConfig(context)
        assertEquals(MSG_RETURNED_DB_VALUE, expected, result)
        assertEquals(MSG_RETURNED_DB_VALUE, expected, ConfigSetup.configuration)
    }

    /** - test_2: Empty DB → retries; cached value set mid-retry is returned. */
    @Test
    fun `getConfig retries when db empty and returns cached once available`() = scope.runTest {
        coEvery { dao.getConfig() } returns null
        val expected = cfg()
        val job = async { ConfigSetup.getConfig(context) }
        advanceTimeBy(1500)
        ConfigSetup.configuration = expected
        advanceUntilIdle()
        val result = job.await()
        assertEquals(MSG_RETURNED_CACHED, expected, result)
        assertEquals(MSG_RETURNED_CACHED, expected, ConfigSetup.configuration)
    }

    /** - test_3: No config anywhere → null after 3 retries. */
    @Test
    fun `getConfig returns null after retries when no config anywhere`() = scope.runTest {
        coEvery { dao.getConfig() } returns null
        val job = async { ConfigSetup.getConfig(context) }
        advanceTimeBy(3500)
        val result = job.await()
        assertNull(MSG_NULL_AFTER_RETRIES, result)
    }

    private companion object {
        private const val FCM_PROVIDER = "android-firebase"
        private const val HMS_PROVIDER = "android-huawei"
        private const val RUS_PROVIDER = "android-rustore"

        private const val MSG_NULL_AFTER_RETRIES = "Expected null after 3 retries when no config anywhere"
        private const val MSG_RETURNED_CACHED = "Expected cached configuration to be returned"
        private const val MSG_RETURNED_DB_VALUE = "Expected DB configuration to be returned immediately"

        private const val API_URL = "https://api.example.com"
        private const val RTOKEN = "rt-NEW"
    }
}
