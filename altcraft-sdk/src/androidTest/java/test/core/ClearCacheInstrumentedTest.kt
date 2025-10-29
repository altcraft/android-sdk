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
import com.altcraft.sdk.core.ClearCache
import com.altcraft.sdk.core.Retry.retryControl
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.Preferenses.getPreferences
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.push.token.TokenManager.tokens
import com.altcraft.sdk.sdk_events.EventList
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.services.manager.ServiceManager
import com.altcraft.sdk.workers.coroutine.CancelWork
import com.altcraft.sdk.workers.periodical.CommonFunctions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import test.events.EventProbe
import test.room.TestRoom
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ClearCacheInstrumentedTest
 *
 * Positive scenario:
 * - test_1: clear() wipes real Room tables, removes SDK SharedPreferences keys,
 * resets in-memory state, stops services, cancels workers, and emits sdkCleared to subscriber.
 */
@RunWith(AndroidJUnit4::class)
class ClearCacheInstrumentedTest {

    private companion object {
        const val MSG_DB_CONFIG_EMPTY   = "configurationTable must be empty"
        const val MSG_DB_SUB_EMPTY      = "subscribeTable must be empty"
        const val MSG_DB_EVENTS_EMPTY   = "pushEventTable must be empty"
        const val MSG_DB_M_EVENTS_EMPTY = "mobileEventTable must be empty"
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
        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()

        mockkObject(SDKdb)
        val sdkDBWrapper = mockk<SDKdb>(relaxed = true)
        every { SDKdb.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.request() } returns dao

        mockkObject(ServiceManager)
        every { ServiceManager.stopService(any(), any()) } just Runs

        mockkObject(CancelWork)
        every { CancelWork.cancelCoroutineWorkersTask(any(), any()) } answers {
            val cb = secondArg<() -> Unit>()
            cb()
        }

        mockkObject(CommonFunctions)
        every { CommonFunctions.cancelPeriodicalWorkersTask(any()) } just Runs

        prefs = getPreferences(appContext)
        prefs.edit(commit = true) {
            putString(Preferenses.TOKEN_KEY, "tkn")
            putString(Preferenses.MANUAL_TOKEN_KEY, "manual")
            putString(Preferenses.MESSAGE_ID_KEY, "msg")
        }

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
                    profileFields = JsonNull,
                    customFields = JsonNull,
                    cats = null,
                    replace = null,
                    skipTriggers = null,
                    uid = UUID.randomUUID().toString(),
                    time = System.currentTimeMillis() / 1000,
                    retryCount = 1,
                    maxRetryCount = 5
                )
            )
            dao.insertPushEvent(
                com.altcraft.sdk.data.room.PushEventEntity(
                    uid = UUID.randomUUID().toString(),
                    type = "test"
                )
            )
            dao.insertMobileEvent(
                MobileEventEntity(
                    id = 0L,
                    userTag = "tag",
                    timeZone = 0,
                    time = System.currentTimeMillis(),
                    sid = "sid-1",
                    altcraftClientID = "cid-1",
                    eventName = "evt",
                    payload = null,
                    matching = null,
                    profileFields = null,
                    subscription = null,
                    matchingType = null,
                    utmTags = null,
                    sendMessageId = null,
                    retryCount = 0,
                    maxRetryCount = 5
                )
            )
        }

        configuration = ConfigurationEntity(
            id = 999, icon = null, apiUrl = "https://cached", rToken = "cached",
            appInfo = null, usingService = false, serviceMessage = null,
            pushReceiverModules = null, providerPriorityList = null,
            pushChannelName = null, pushChannelDescription = null
        )
        retryControl = AtomicBoolean(true)
        tokens.add("123")

        Events.subscribe(probe)
        probe.clear()
    }

    @After
    fun tearDown() {
        Events.unsubscribe()
        unmockkAll()
        db.close()
    }

    /** - test_1: clear() wipes DB, clears prefs, resets flags, stops services, cancels workers,
     * and emits sdkCleared.
     * */
    @Test
    fun clear_wipesDbPrefsWorkersAndEmitsEvent() = runBlocking {
        val latch = CountDownLatch(1)
        ClearCache.clear(appContext) { latch.countDown() }
        assertTrue(MSG_CALLBACK_FIRED, latch.await(5, TimeUnit.SECONDS))

        assertNull(MSG_DB_CONFIG_EMPTY, dao.getConfig())
        assertTrue(MSG_DB_SUB_EMPTY, dao.allSubscriptionsByTag("tag").isEmpty())
        assertEquals(MSG_DB_EVENTS_EMPTY, 0, dao.getPushEventCount())
        assertEquals(MSG_DB_M_EVENTS_EMPTY, 0, dao.getMobileEventCount())

        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.TOKEN_KEY, null))
        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.MANUAL_TOKEN_KEY, null))
        assertNull(MSG_PREFS_CLEARED, prefs.getString(Preferenses.MESSAGE_ID_KEY, null))

        assertNull(MSG_FLAGS_RESET, configuration)
        assertFalse(MSG_FLAGS_RESET, retryControl.get())
        assertTrue(MSG_FLAGS_RESET, tokens.isEmpty())

        io.mockk.verify(exactly = 1) { ServiceManager.stopService(appContext, SUBSCRIBE_SERVICE) }
        io.mockk.verify(exactly = 1) { ServiceManager.stopService(appContext, UPDATE_SERVICE) }
        io.mockk.verify(exactly = 1) { CancelWork.cancelCoroutineWorkersTask(appContext, any()) }
        io.mockk.verify(exactly = 1) { CommonFunctions.cancelPeriodicalWorkersTask(appContext) }

        val events = probe.snapshot()
        assertTrue(MSG_EVENT_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals("clear", last.function)
        assertEquals(EventList.sdkCleared.first, last.eventCode)
        assertEquals(EventList.sdkCleared.second, last.eventMessage)
    }
}