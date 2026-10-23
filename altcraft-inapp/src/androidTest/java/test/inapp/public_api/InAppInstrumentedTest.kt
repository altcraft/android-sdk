package test.inapp.public_api

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
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.InApp
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.platform.subscription.status.request.StatusRequest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * InAppInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: setPlacement updates current placement.
 *  - test_2: setPlacement with null clears placement.
 *  - test_3: setAnimation stores listener.
 *  - test_4: inAppTrigger does not throw.
 *  - test_5: getInAppPlacements does not throw.
 *
 * Negative scenarios:
 *  - test_6: getInAppSubscriptionStatus not authenticated returns null.
 *  - test_7: getInAppSubscriptionStatus with exception returns null.
 */
@RunWith(AndroidJUnit4::class)
class InAppInstrumentedTest {

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

        InAppVariables.reset()
        InApp.onInAppShownListener = null

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
        InAppVariables.reset()
        InApp.onInAppShownListener = null
        db.close()
        unmockkAll()
    }

    /** test_1: setPlacement updates current placement. */
    @Test
    fun test_1_setPlacement_updatesScreen() {
        InApp.setScreen(context, "home_screen")
        assertThat(InAppVariables.getCurrentScreen(), `is`("home_screen"))
    }

    /** test_2: setPlacement with null clears placement. */
    @Test
    fun test_2_setPlacement_null_clearsScreen() {
        InApp.setScreen(context, "some_screen")
        InApp.setScreen(context, "")
        assertThat(InAppVariables.getCurrentScreen(), `is`(""))
    }

    /** test_3: setAnimation stores listener. */
    @Test
    fun test_3_animation_storesListener() {
        InApp.animation { }
        assertNotNull(InApp.onInAppShownListener)
    }

    /** test_4: inAppTrigger does not throw. */
    @Test
    fun test_4_trigger_doesNotThrow() = runBlocking {
        mockkObject(InAppProcTrigger)

        try {
            InApp.trigger(context, "test_trigger")
            delay(200)
        } catch (e: Exception) {
            fail("inAppTrigger should not throw: ${e.message}")
        }

        verify { InAppProcTrigger.trigger(any(), any()) }
    }

    /** test_5: getInAppPlacements does not throw. */
    @Test
    fun test_5_getPlacements_doesNotThrow() = runBlocking {
        mockkObject(PlacementsProcess)
        PlacementsProcess.placementsReady()

        try {
            InApp.getPlacements(context)
            delay(200)
        } catch (e: Exception) {
            fail("getInAppPlacements should not throw: ${e.message}")
        }
    }

    /** test_6: getInAppSubscriptionStatus not authenticated returns null. */
    @Test
    fun test_6_getSubscriptionStatus_notAuthenticated_returnsNull() = runTest {
        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.isAuth() } returns false

        val result = InApp.subscription.getSubscriptionStatus(context)
        assertThat(result == null, `is`(true))
    }

    /** test_7: getInAppSubscriptionStatus with exception returns null. */
    @Test
    fun test_7_getSubscriptionStatus_exception_returnsNull() = runTest {
        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.isAuth() } returns true

        mockkObject(StatusRequest)
        coEvery { StatusRequest.inAppStatusRequest(any()) } throws RuntimeException("network error")

        val result = InApp.subscription.getSubscriptionStatus(context)
        assertThat(result == null, `is`(true))
    }
}
