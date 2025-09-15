package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.config.ConfigSetup.configuration
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.Preferenses.getPreferences
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.events.EventList
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.Core.pushControl
import com.altcraft.sdk.push.token.TokenManager.tokens
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.CancelWork
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.core.ClearCache
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import test.events.EventProbe
import test.room.TestRoom
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.*

/**
 * ClearCacheInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: clear() wipes real in-memory Room tables (config/subscribe/pushEvents),
 *            removes SDK SharedPreferences keys, resets in-memory flags/state,
 *            calls stopService/cancelWorkers, and emits sdkCleared to the subscriber.
 *
 * Notes:
 *  - Uses ApplicationProvider context, real in-memory Room (TestRoom) with production entities.
 *  - SDKdb.getDb(context) is mocked to return a wrapper exposing the REAL DAO.
 *  - External functions stopService/cancel*Workers are mocked.
 *  - Events are NOT mocked; we subscribe using a probe to capture real emissions.
 */
@RunWith(AndroidJUnit4::class)
class ClearCacheInstrumentedTest {

    // ---- Messages ----
    private companion object {
        const val MSG_DB_CONFIG_EMPTY   = "configurationTable must be empty"
        const val MSG_DB_SUB_EMPTY      = "subscribeTable must be empty"
        const val MSG_DB_EVENTS_EMPTY   = "pushEventTable must be empty"
        const val MSG_PREFS_CLEARED     = "SDK-related SharedPreferences keys must be removed"
        const val MSG_FLAGS_RESET       = "In-memory flags/state must be reset"
        const val MSG_EVENT_CAPTURED    = "Expected to capture Events emission"
        const val MSG_CALLBACK_FIRED    = "onComplete must be invoked"
    }

    private lateinit var appContext: Context
    private lateinit var db: TestRoom
    private lateinit var dao: DAO
    private lateinit var prefs: SharedPreferences

    private val probe = EventProbe()

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()

        // Real in-memory Room
        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()

        // Mock SDKdb.getDb(context) -> wrapper.request() -> REAL DAO
        mockkObject(SDKdb)
        val sdkDBWrapper = mockk<SDKdb>(relaxed = true)
        every { SDKdb.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.request() } returns dao

        // Mock external side-effects
        mockkObject(ServiceManager)
        every { ServiceManager.stopService(any(), any()) } just Runs

        mockkObject(CancelWork)
        every { CancelWork.cancelCoroutineWorkersTask(any(), any()) } answers {
            // arg2 is callback: invoke immediately
            val cb = secondArg<() -> Unit>()
            cb()
        }

        mockkObject(CommonFunctions)
        every { CommonFunctions.cancelPeriodicalWorkersTask(any()) } just Runs

        // Real preferences
        prefs = getPreferences(appContext)
        prefs.edit(commit = true) {
            putString(Preferenses.TOKEN_KEY, "tkn")
            putString(Preferenses.MANUAL_TOKEN_KEY, "manual")
            putString(Preferenses.MESSAGE_ID_KEY, "msg")
        }

        // Preload DB with sample rows (suspend DAO -> runBlocking)
        runBlocking {
            dao.insertConfig(
                ConfigurationEntity(
                    id = 1,
                    icon = null,
                    apiUrl = "https://api",
                    rToken = "rt",
                    appInfo = null,
                    usingService = true,
                    serviceMessage = "svc",
                    pushReceiverModules = listOf("m"),
                    providerPriorityList = listOf("android-firebase"),
                    pushChannelName = "ch",
                    pushChannelDescription = "desc"
                )
            )
            dao.insertSubscribe(
                SubscribeEntity(
                    userTag = "tag",
                    status = "PENDING",
                    sync = null,
                    profileFields = JsonNull, customFields = JsonNull, cats = null,
                    replace = null, skipTriggers = null,
                    uid = UUID.randomUUID().toString(),
                    time = System.currentTimeMillis()/1000,
                    retryCount = 1, maxRetryCount = 5
                )
            )
            dao.insertPushEvent(
                com.altcraft.sdk.data.room.PushEventEntity(
                    uid = UUID.randomUUID().toString(),
                    type = "test"
                )
            )
        }

        // Preload in-memory state
        configuration = ConfigurationEntity(
            id = 999, icon = null, apiUrl = "https://cached", rToken = "cached",
            appInfo = null, usingService = false, serviceMessage = null,
            pushReceiverModules = null, providerPriorityList = null,
            pushChannelName = null, pushChannelDescription = null
        )
        pushControl = AtomicBoolean(true)
        tokens.add("123")

        // Subscribe to events
        Events.subscribe(probe)
        probe.clear()
    }

    @After
    fun tearDown() {
        Events.unsubscribe()
        unmockkAll()
        db.close()
    }

    @Test
    fun clear_wipesDbPrefsWorkersAndEmitsEvent() = runBlocking {
        // Arrange latch for async callback
        val latch = CountDownLatch(1)

        // Act
        ClearCache.clear(appContext) { latch.countDown() }

        // Wait for callback
        assertTrue(MSG_CALLBACK_FIRED, latch.await(5, TimeUnit.SECONDS))

        // DB assertions
        assertNull(MSG_DB_CONFIG_EMPTY, dao.getConfig())
        assertTrue(MSG_DB_SUB_EMPTY, dao.allSubscriptionsByTag("tag").isEmpty())
        assertEquals(MSG_DB_EVENTS_EMPTY, 0, dao.getPushEventCount())

        // Preferences cleared
        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.TOKEN_KEY, null))
        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.MANUAL_TOKEN_KEY, null))
        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.MESSAGE_ID_KEY, null))

        // In-memory flags/state
        assertNull(MSG_FLAGS_RESET, configuration)
        assertFalse(MSG_FLAGS_RESET, pushControl.get())
        assertTrue(MSG_FLAGS_RESET, tokens.isEmpty())

        // External calls
        verify(exactly = 1) { ServiceManager.stopService(appContext, SUBSCRIBE_SERVICE) }
        verify(exactly = 1) { ServiceManager.stopService(appContext, UPDATE_SERVICE) }
        verify(exactly = 1) { CancelWork.cancelCoroutineWorkersTask(appContext, any()) }
        verify(exactly = 1) { CommonFunctions.cancelPeriodicalWorkersTask(appContext) }

        // Event emitted
        val events = probe.snapshot()
        assertTrue(MSG_EVENT_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals("clear", last.function)
        assertEquals(EventList.sdkCleared.first, last.eventCode)
        assertEquals(EventList.sdkCleared.second, last.eventMessage)
    }
}
