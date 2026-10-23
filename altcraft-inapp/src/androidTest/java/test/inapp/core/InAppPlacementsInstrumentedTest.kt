package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import com.altcraft.sdk.inapp.data.Constants
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppsDao
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.workers.onetime.Request as WorkerRequest
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class InAppPlacementsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
    private lateinit var inAppsDao: InAppsDao
    private val testAcid = "acid-123"
    private val testDeviceId = "device-123"

    private val testAuth = AuthenticationEntity(
        id = 0,
        anonimAcid = "anon-acid",
        acid = testAcid,
        deviceId = testDeviceId,
        currentUser = null,
        startAnonSession = System.currentTimeMillis(),
        isTest = false
    )

    /** Default in-app configuration with automatic requests enabled. */
    private val defaultConfig = InAppConfigEntity(
        inAppAutoRequest = true,
        placementsRequestInterval = 60_000L,
        maxPlacements = 10
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
        inAppsDao = db.dao.inApps

        MockKAnnotations.init(this, relaxUnitFun = true)

        PlacementsProcess.placementsReady()

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false
        every { SubFunction.time() } returns System.currentTimeMillis()

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred<Unit>().apply {
            complete(Unit)
        }

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }

        mockkObject(WorkerRequest)
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        coEvery { env.config() } returns defaultConfig
        coEvery { env.internalAuth() } returns testAuth
        every { Environment.create(any()) } returns env
    }

    @After
    fun tearDown() {
        db.close()
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    @Test
    fun requestPlacements_enqueues_worker() = runBlocking {
        inAppsDao.clearInAppsData()

        PlacementsProcess.requestPlacements(context)

        Thread.sleep(500)
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.IN_APP_PLACEMENTS_O_WORK_TAG).get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun requestPlacements_throttled_skips_worker() = runBlocking {
        val state = InAppsEntity(
            acid = testAcid,
            deviceId = testDeviceId,
            lastRequestTime = System.currentTimeMillis()
        )
        inAppsDao.insert(state)

        PlacementsProcess.requestPlacements(context)

        Thread.sleep(500)
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.IN_APP_PLACEMENTS_O_WORK_TAG).get()
        assertThat(infos.isEmpty(), `is`(true))
    }

    @Test
    fun requestPlacements_auth_change_clears_data() = runBlocking {
        val oldState = InAppsEntity(
            acid = "old-acid",
            deviceId = "old-device",
            lastRequestTime = 0L
        )
        inAppsDao.insert(oldState)

        PlacementsProcess.requestPlacements(context)

        Thread.sleep(500)
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.IN_APP_PLACEMENTS_O_WORK_TAG).get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun placementsReady_completes_deferred() = runBlocking {
        PlacementsProcess.placementsReady()

        val gate = PlacementsProcess.placementsReady
        assertThat(gate.isCompleted, `is`(true))
    }

    @Test
    fun requestPlacements_autoRequestDisabled_marksReady() = runBlocking {
        val config = InAppConfigEntity(
            inAppAutoRequest = false,
            placementsRequestInterval = 60_000L,
            maxPlacements = 10
        )
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        coEvery { env.config() } returns config
        coEvery { env.internalAuth() } returns testAuth
        every { Environment.create(any()) } returns env

        mockkObject(PlacementsProcess)
        every {
            PlacementsProcess.requestPlacements(any(), any(), any())
        } answers { callOriginal() }
        every { PlacementsProcess.placementsReady() } answers { callOriginal() }

        PlacementsProcess.requestPlacements(context)

        verify(timeout = 5_000, exactly = 1) {
            PlacementsProcess.placementsReady()
        }
    }
}
