package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.DataClasses
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
 * ConfigSetupForegroundRetryTest
 *
 * Positive scenarios:
 *  - test_2: If configuration becomes available during retries, getConfig()
 *            returns the in-memory cached value before exhausting all attempts.
 *
 * Negative scenarios:
 *  - test_1: In foreground (isAppInForegrounded = true), when DB has a config
 *            but foreground precondition blocks returning it, getConfig()
 *            retries 3× with 1-second delays and returns null.
 *
 * Notes:
 *  - Pure JVM unit tests using kotlinx-coroutines-test virtual time.
 *  - DAO/SDKdb/Events/SubFunction are mocked. Only suspend methods use coEvery/coVerify.
 *  - Assertions use JUnit4; messages are constants.
 */

// ---------- Provider constants ----------
private const val FCM_PROVIDER: String = "android-firebase"
private const val HMS_PROVIDER: String = "android-huawei"
private const val RUS_PROVIDER: String = "android-rustore"

// ---------- Messages ----------
private const val MSG_NULL_AFTER_RETRIES = "Expected null after 3 retries in foreground branch"
private const val MSG_RETURNED_CACHED = "Expected cached configuration to be returned"

// ---------- Test data ----------
private const val API_URL = "https://api.example.com"
private const val RTOKEN = "rt-NEW"

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigSetupTest {

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

        // SDKdb.getDb(context).request() → mocked DAO
        mockkObject(SDKdb)
        every { SDKdb.getDb(context) } returns db
        every { db.request() } returns dao

        // SubFunction.isAppInForegrounded() → true (foreground branch)
        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns true

        // Events: event() is Unit, error() RETURNS DataClasses.Error
        mockkObject(Events)
        justRun { Events.event(any(), any()) }
        val errorStub: DataClasses.Error = mockk(relaxed = true)
        every { Events.error(any(), any()) } returns errorStub

        // DAO suspend calls
        coEvery { dao.getConfig() } returns cfg() // DB has some config, but foreground path won't return it

        ConfigSetup.configuration = null
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ---------- Helpers ----------
    private fun cfg(serviceMessage: String? = null) = ConfigurationEntity(
        id = 0,
        icon = null,
        apiUrl = API_URL,
        rToken = RTOKEN,
        appInfo = null,
        usingService = false,
        serviceMessage = serviceMessage,
        pushReceiverModules = listOf("push-receiver-a"),
        providerPriorityList = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER),
        pushChannelName = "Altcraft",
        pushChannelDescription = "Altcraft notifications channel"
    )

    // ---------- Tests ----------

    /** Foreground branch: 3 retries × 1000ms → returns null. */
    @Test
    fun `getConfig foreground retries then returns null`() = scope.runTest {
        val job = async { ConfigSetup.getConfig(context) }

        // Simulate the 3 × 1s delays using virtual time
        advanceTimeBy(3000)

        val result = job.await()
        assertNull(MSG_NULL_AFTER_RETRIES, result)
    }

    /**
     * Foreground branch: while getConfig() is retrying, configuration becomes available;
     * it should return the cached value immediately on the next loop.
     */
    @Test
    fun `getConfig foreground returns cached when it becomes available mid-retry`() = scope.runTest {
        val expected = cfg(serviceMessage = "from-cache")

        val job = async { ConfigSetup.getConfig(context) }

        // Let the first delay tick (e.g., 1500ms to be between attempts)
        advanceTimeBy(1500)

        // Now some other part of the app sets the cached configuration
        ConfigSetup.configuration = expected

        // Run the rest of the scheduled work
        advanceUntilIdle()

        val result = job.await()
        assertEquals(MSG_RETURNED_CACHED, expected, result)

        // Once cache is set, DB is not needed anymore within that fast path.
        // (We can't strictly assert zero DB calls because the first iteration might already have called it
        //  before we set the cache, but at least result comes from cache.)
        assertEquals(MSG_RETURNED_CACHED, expected, ConfigSetup.configuration)
    }
}
