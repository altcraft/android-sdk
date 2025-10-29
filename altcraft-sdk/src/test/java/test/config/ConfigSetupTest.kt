package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.additional.SubFunction
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
 *  - test_2: If configuration becomes available during retries, getConfig() returns the in-memory
 *  cached value before exhausting all attempts.
 *
 * Negative scenarios:
 *  - test_1: In foreground (isAppInForegrounded = true) and DB has a config but foreground
 *  precondition blocks returning it, getConfig() retries 3× with 1-second delays and returns null.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigSetupForegroundRetryTest {

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

        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns true

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

    /** - test_1: Foreground branch retries 3× × 1000ms and returns null. */
    @Test
    fun `getConfig foreground retries then returns null`() = scope.runTest {
        val job = async { ConfigSetup.getConfig(context) }
        advanceTimeBy(3000)
        val result = job.await()
        assertNull(MSG_NULL_AFTER_RETRIES, result)
    }

    /** - test_2: Foreground branch returns cached configuration when it becomes available mid-retry. */
    @Test
    fun `getConfig foreground returns cached when it becomes available mid-retry`() = scope.runTest {
        val expected = cfg(serviceMessage = "from-cache")
        val job = async { ConfigSetup.getConfig(context) }
        advanceTimeBy(1500)
        ConfigSetup.configuration = expected
        advanceUntilIdle()
        val result = job.await()
        assertEquals(MSG_RETURNED_CACHED, expected, result)
        assertEquals(MSG_RETURNED_CACHED, expected, ConfigSetup.configuration)
    }

    private companion object {
        private const val FCM_PROVIDER = "android-firebase"
        private const val HMS_PROVIDER = "android-huawei"
        private const val RUS_PROVIDER = "android-rustore"

        private const val MSG_NULL_AFTER_RETRIES = "Expected null after 3 retries in foreground branch"
        private const val MSG_RETURNED_CACHED = "Expected cached configuration to be returned"

        private const val API_URL = "https://api.example.com"
        private const val RTOKEN = "rt-NEW"
    }
}
