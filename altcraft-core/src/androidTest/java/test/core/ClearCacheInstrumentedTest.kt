package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.config.ConfigManager.conf
import com.altcraft.sdk.core.Init.initPhaseStarted
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.util.log.Log
import com.altcraft.sdk.workers.CancelWork
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.events.EventProbe
import test.room.TestRoom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ClearCacheInstrumentedTest
 *
 * Positive scenario:
 * - test_1: clear() wipes real Room tables, resets in-memory state, cancels workers,
 * and emits sdkCleared to subscriber.
 *
 * NOTE:
 * - This test intentionally DOES NOT verify DataStore cleanup.
 */
@RunWith(AndroidJUnit4::class)
class ClearCacheInstrumentedTest {

    private companion object {
        const val MSG_DB_CONFIG_EMPTY = "configurationTable must be empty"
        const val MSG_FLAGS_RESET = "In-memory flags/state must be reset"
        const val MSG_EVENT_CAPTURED = "Expected to capture Events emission"
        const val MSG_CALLBACK_FIRED = "onComplete must be invoked"
    }

    private lateinit var appContext: Context
    private lateinit var db: TestRoom
    private lateinit var testConfigDao: ConfigurationDao

    private val probe = EventProbe()

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        testConfigDao = db.configurationDao()

        mockkObject(DB.Companion)
        val sdkDBWrapper = mockk<DB>(relaxed = true)
        every { DB.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.configurationDao() } returns testConfigDao
        coEvery { sdkDBWrapper.clear() } answers {
            runBlocking { db.clearAllTables() }
            Unit
        }

        mockkObject(CancelWork)
        coEvery { CancelWork.cancelWorkerTask(any(), any<List<String>>()) } just Runs

        runBlocking {
            testConfigDao.insert(
                ConfigurationEntity(
                    id = 1,
                    apiUrl = "https://api",
                    rToken = "rt",
                    appInfo = null
                )
            )
        }

        conf = ConfigurationEntity(
            id = 999,
            apiUrl = "https://cached",
            rToken = "cached",
            appInfo = null
        )
        initPhaseStarted.set(true)

        Events.Subscription.subscribe(probe)
        probe.clear()
    }

    @After
    fun tearDown() {
        Events.Subscription.unsubscribe()
        unmockkAll()
        db.close()
    }

    /** - test_1: clear() wipes DB, resets flags, cancels workers, and emits sdkCleared. */
    @Test
    fun clear_wipesDbPrefsWorkersAndEmitsEvent() = runBlocking {
        val latch = CountDownLatch(1)
        AltcraftSDK.clear(appContext) { latch.countDown() }
        assertTrue(MSG_CALLBACK_FIRED, latch.await(5, TimeUnit.SECONDS))

        assertNull(MSG_DB_CONFIG_EMPTY, testConfigDao.get())

        assertNull(MSG_FLAGS_RESET, conf)
        assertFalse(MSG_FLAGS_RESET, initPhaseStarted.get())

        io.mockk.coVerify(exactly = 1) { CancelWork.cancelWorkerTask(appContext, any<List<String>>()) }

        val events = probe.snapshot()
        assertTrue(MSG_EVENT_CAPTURED, events.isNotEmpty())
        val last = events.last()
        assertEquals(Log.MODULE_PREFIX + "clear", last.function)
        assertEquals(CoreEvents.sdkCleared, last.event)
        assertEquals(CoreEvents.sdkCleared.message, last.eventMessage)
    }
}