package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.sdk_events.EventList
import com.altcraft.sdk.sdk_events.Events
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import test.events.EventProbe
import test.room.TestRoom
import java.util.UUID

/**
 * ConfigSetupInstrumentedEventsTest
 *
 * Positive scenarios:
 * - test_1: setConfig writes into real in-memory Room DB and emits a real Events.event delivered
 * to the active subscriber.
 * - test_4: setConfig overwrites an existing configuration row when values are changed (persisted
 * record reflects the latest config).
 *
 * Negative scenarios:
 * - test_2: setConfig when AltcraftConfiguration.toEntity() throws → emits a real Events.error to
 * the subscriber and returns null.
 *
 * Behavioral scenarios:
 * - test_3: getConfig (background branch) returns the row from the real DB without emitting events.
 * - test_5: rToken change triggers DAO.deleteAllSubscriptions() and DAO.deleteAllMobileEvents();
 * both tables become empty and configIsSet event is emitted.
 */
@RunWith(AndroidJUnit4::class)
class ConfigSetupInstrumentedEventsTest {

    private lateinit var appContext: Context
    private lateinit var db: TestRoom
    private lateinit var realDao: DAO

    private val probe = EventProbe()

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        realDao = db.request()

        mockkObject(SDKdb)
        val sdkDBWrapper = mockk<SDKdb>(relaxed = true)
        every { SDKdb.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.request() } returns realDao

        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns false

        ConfigSetup.configuration = null

        Events.subscribe(probe)
        probe.clear()
    }

    @After
    fun tearDown() {
        Events.unsubscribe()
        unmockkAll()
        db.close()
    }

    private fun cfg(
        rToken: String?,
        apiUrl: String = API_URL,
        serviceMessage: String? = null
    ) = ConfigurationEntity(
        id = 1,
        icon = null,
        apiUrl = apiUrl,
        rToken = rToken,
        appInfo = null,
        usingService = false,
        serviceMessage = serviceMessage,
        pushReceiverModules = listOf("push-receiver-a"),
        providerPriorityList = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER),
        pushChannelName = PUSH_CHANNEL_NAME,
        pushChannelDescription = PUSH_CHANNEL_DESCRIPTION
    )

    private fun sub(
        tag: String,
        uid: String = UUID.randomUUID().toString(),
        retryCount: Int = 1
    ) = SubscribeEntity(
        userTag = tag,
        status = SUBSCRIBED,
        sync = null,
        profileFields = null,
        customFields = null,
        cats = null,
        replace = null,
        skipTriggers = null,
        uid = uid,
        time = System.currentTimeMillis() / 1000,
        retryCount = retryCount,
        maxRetryCount = 5
    )

    private fun me(
        userTag: String,
        eventName: String = "event_${System.nanoTime()}",
        sid: String = "sid_${UUID.randomUUID()}",
        altcraftClientID: String = "client_${UUID.randomUUID()}",
        tz: Int = 0,
        retryCount: Int = 0,
        maxRetryCount: Int = 5
    ) = MobileEventEntity(
        id = 0L,
        userTag = userTag,
        timeZone = tz,
        time = System.currentTimeMillis(),
        sid = sid,
        altcraftClientID = altcraftClientID,
        eventName = eventName,
        payload = null,
        matching = null,
        profileFields = null,
        subscription = null,
        sendMessageId = null,
        matchingType = null,
        utmTags = null,
        retryCount = retryCount,
        maxRetryCount = maxRetryCount
    )

    /** - test_1: setConfig writes to real DB, updates cache, and emits Events.event to subscriber. */
    @Test
    fun setConfig_emitsRealEvent_andWritesToDb() = runBlocking {
        val newEntity = cfg(rToken = "rt-NEW")
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } returns newEntity

        val result = ConfigSetup.setConfig(appContext, newConfig)

        assertEquals(MSG_RESULT_NOT_NULL, newEntity, result)
        assertEquals(MSG_CACHE_UPDATED, newEntity, ConfigSetup.configuration)

        val fromDb = realDao.getConfig()
        assertNotNull(MSG_DB_MATCH, fromDb)
        assertEquals(MSG_DB_MATCH, newEntity.copy(id = fromDb!!.id), fromDb)

        val events: List<DataClasses.Event> = probe.snapshot()
        assertTrue(MSG_EVENT_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals("setConfig", last.function)
        assertEquals(EventList.configIsSet.first, last.eventCode)
        assertEquals(EventList.configIsSet.second, last.eventMessage)
    }

    /** - test_2: setConfig emits a real Events.error and returns null when toEntity() throws. */
    @Test
    fun setConfig_emitsRealError_onException() = runBlocking {
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } throws IllegalStateException("boom")

        val result = ConfigSetup.setConfig(appContext, newConfig)
        assertNull("Result must be null on exception", result)

        val events: List<DataClasses.Event> = probe.snapshot()
        assertTrue(MSG_ERROR_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals("setConfig", last.function)
        assertEquals(400, last.eventCode)
        assertTrue(last.eventMessage?.contains("boom") == true)
    }

    /** - test_3: getConfig (background) returns configuration from real DB without emitting events. */
    @Test
    fun getConfig_returnsFromDb_inBackground() = runBlocking {
        every { SubFunction.isAppInForegrounded() } returns false

        val pre = cfg(rToken = "rt-NEW")
        realDao.insertConfig(pre)
        ConfigSetup.configuration = null

        val got = ConfigSetup.getConfig(appContext)
        val fromDb = realDao.getConfig()

        assertNotNull(MSG_NOT_NULL, got)
        assertEquals(MSG_GET_CONFIG_MATCH, fromDb, got)
    }

    /** - test_4: setConfig overwrites existing configuration when values change (update path). */
    @Test
    fun setConfig_overwritesExistingConfig_whenChanged() = runBlocking {
        val old = cfg(rToken = "rt-NEW", serviceMessage = "old")
        realDao.insertConfig(old)

        val updated = cfg(rToken = "rt-NEW", serviceMessage = "new")
        val updateConfig = mockk<AltcraftConfiguration>()
        every { updateConfig.toEntity() } returns updated

        val result = ConfigSetup.setConfig(appContext, updateConfig)

        assertEquals(MSG_RESULT_NOT_NULL, updated, result)
        assertEquals(MSG_CACHE_UPDATED, updated, ConfigSetup.configuration)

        val fromDb = realDao.getConfig()
        assertNotNull(fromDb)
        assertEquals(MSG_OVERWRITE_OK, updated.copy(id = fromDb!!.id), fromDb)
    }

    /** - test_5: rToken change clears subscriptions and mobile events via real DAO and emits configIsSet event. */
    @Test
    fun setConfig_rTokenChange_clearsSubscriptionsAndMobileEvents_andEmitsEvent() = runBlocking {
        val tag = "test-tag"

        realDao.insertSubscribe(sub(tag))
        realDao.insertSubscribe(sub(tag))
        realDao.insertSubscribe(sub(tag))
        val subsBefore = realDao.allSubscriptionsByTag(tag).size
        assertTrue(MSG_SUB_ROWS_INSERTED, subsBefore >= 1)

        realDao.insertMobileEvent(me(tag))
        realDao.insertMobileEvent(me(tag))
        val meBefore = realDao.getMobileEventCount()
        assertTrue(MSG_ME_ROWS_INSERTED, meBefore >= 1)

        val old = cfg(rToken = "rt-OLD")
        realDao.insertConfig(old)

        val fresh = cfg(rToken = "rt-NEW")
        val newConfig = mockk<AltcraftConfiguration>()
        every { newConfig.toEntity() } returns fresh

        probe.clear()

        val result = ConfigSetup.setConfig(appContext, newConfig)

        val subsAfter = realDao.allSubscriptionsByTag(tag).size
        assertEquals(MSG_SUB_ROWS_CLEARED, 0, subsAfter)

        val meAfter = realDao.getMobileEventCount()
        assertEquals(MSG_ME_ROWS_CLEARED, 0, meAfter)

        assertEquals(MSG_RESULT_NOT_NULL, fresh, result)
        assertEquals(MSG_CACHE_UPDATED, fresh, ConfigSetup.configuration)

        val events = probe.snapshot()
        assertTrue(MSG_EVENT_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals("setConfig", last.function)
        assertEquals(EventList.configIsSet.first, last.eventCode)
        assertEquals(EventList.configIsSet.second, last.eventMessage)
    }

    companion object {
        private const val FCM_PROVIDER: String = "android-firebase"
        private const val HMS_PROVIDER: String = "android-huawei"
        private const val RUS_PROVIDER: String = "android-rustore"
        private const val API_URL = "https://api.example.com"
        private const val PUSH_CHANNEL_NAME = "Altcraft"
        private const val PUSH_CHANNEL_DESCRIPTION = "Altcraft notifications channel"
        private const val MSG_RESULT_NOT_NULL = "Resulting configuration must not be null"
        private const val MSG_CACHE_UPDATED = "In-memory cache must be updated"
        private const val MSG_DB_MATCH = "DB value must match the expected configuration"
        private const val MSG_EVENT_CAPTURED = "Expected to capture Events.event"
        private const val MSG_ERROR_CAPTURED = "Expected to capture Events.error"
        private const val MSG_GET_CONFIG_MATCH =
            "getConfig must return the configuration stored in DB"
        private const val MSG_NOT_NULL = "Value must not be null"
        private const val MSG_SUB_ROWS_INSERTED =
            "Rows must be inserted into subscribeTable for test setup"
        private const val MSG_SUB_ROWS_CLEARED = "subscribeTable must be empty after rToken change"
        private const val MSG_ME_ROWS_INSERTED =
            "Rows must be inserted into mobileEventTable for test setup"
        private const val MSG_ME_ROWS_CLEARED = "mobileEventTable must be empty after rToken change"
        private const val MSG_OVERWRITE_OK =
            "Persisted configuration must reflect the latest values"
    }
}