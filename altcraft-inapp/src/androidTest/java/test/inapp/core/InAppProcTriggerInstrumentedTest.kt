package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.data.room.DB
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * InAppProcTriggerInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: trigger() does not throw with valid trigger.
 *  - test_2: trigger() with DEFAULT type processes correctly.
 *  - test_3: trigger() with CUSTOM type processes correctly.
 *  - test_4: multiple triggers are queued and processed.
 *
 * Negative scenarios:
 *  - test_5: trigger() with empty DB does not crash.
 */
@RunWith(AndroidJUnit4::class)
class InAppProcTriggerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB

    private val testAcid = "acid-123"
    private val testDeviceId = "device-123"

    private val testAuth = AuthenticationEntity(
        id = 0,
        anonimAcid = "anon-acid",
        acid = testAcid,
        deviceId = testDeviceId,
        currentUser = "tag1",
        startAnonSession = System.currentTimeMillis(),
        isTest = false
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)

        db = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false
        every { SubFunction.time() } returns System.currentTimeMillis()

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        coEvery { env.internalAuth() } returns testAuth
        every { Environment.create(any()) } returns env
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /** test_1: trigger() does not throw with valid CUSTOM trigger. */
    @Test
    fun test_1_trigger_customType_doesNotThrow() = runBlocking {
        try {
            InAppProcTrigger.trigger(context, InAppTrigger(name = "custom_btn", type = TriggerType.CUSTOM))
            delay(500)
        } catch (e: Exception) {
            fail("trigger should not throw: ${e.message}")
        }
    }

    /** test_2: trigger() with DEFAULT type does not throw. */
    @Test
    fun test_2_trigger_defaultType_doesNotThrow() = runBlocking {
        try {
            InAppProcTrigger.trigger(context, InAppTrigger(name = "open_page", type = TriggerType.DEFAULT))
            delay(500)
        } catch (e: Exception) {
            fail("trigger should not throw: ${e.message}")
        }
    }

    /** test_3: trigger() with empty DB does not crash. */
    @Test
    fun test_3_trigger_emptyDB_doesNotCrash() = runBlocking {
        try {
            InAppProcTrigger.trigger(context, InAppTrigger(name = "any_trigger", type = TriggerType.CUSTOM))
            delay(500)
        } catch (e: Exception) {
            fail("trigger with empty DB should not throw: ${e.message}")
        }
    }

    /** test_4: multiple triggers are queued without crashing. */
    @Test
    fun test_4_multipleTriggers_queuedSuccessfully() = runBlocking {
        try {
            InAppProcTrigger.trigger(context, InAppTrigger(name = "trigger1", type = TriggerType.CUSTOM))
            InAppProcTrigger.trigger(context, InAppTrigger(name = "trigger2", type = TriggerType.DEFAULT))
            InAppProcTrigger.trigger(context, InAppTrigger(name = "trigger3", type = TriggerType.CUSTOM))
            delay(1000)
        } catch (e: Exception) {
            fail("multiple triggers should not throw: ${e.message}")
        }
    }

    /** test_5: trigger() with empty name does not crash. */
    @Test
    fun test_5_trigger_emptyName_doesNotCrash() = runBlocking {
        try {
            InAppProcTrigger.trigger(context, InAppTrigger(name = "", type = TriggerType.CUSTOM))
            delay(500)
        } catch (e: Exception) {
            fail("trigger with empty name should not throw: ${e.message}")
        }
    }
}
