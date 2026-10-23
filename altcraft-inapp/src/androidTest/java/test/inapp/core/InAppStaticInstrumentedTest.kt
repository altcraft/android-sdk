package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.static.StaticProcess
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.entity.InAppStaticEntity
import com.altcraft.sdk.workers.onetime.Request as WorkerRequest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class InAppStaticInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
    private val testAcid = "acid-123"
    private val testDeviceId = "device-123"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)
        WorkManager.getInstance(context).cancelAllWork().result.get()

        db = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()

        MockKAnnotations.init(this, relaxUnitFun = true)

        StaticProcess.static = null

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.time() } returns System.currentTimeMillis()

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }

        mockkObject(WorkerRequest)
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false
        coEvery { WorkerRequest.hasRunningWorker(any(), any()) } returns false

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        every { Environment.create(any()) } returns env
    }

    @After
    fun tearDown() {
        StaticProcess.static = null
        db.close()
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    @Test
    fun loadStatic_cached_returns_immediately() = runTest {
        val freshBundle = InAppStaticEntity(
            id = 0,
            createdAt = System.currentTimeMillis(),
            blockJs = "console.log('block')",
            blockCss = "body{color:red}",
            inAppLibraryJs = "console.log('lib')"
        )
        db.dao.static.insert(freshBundle)

        val result = StaticProcess.loadStatic(context)

        assertThat(result, `is`(freshBundle))
    }

    @Test
    fun loadStatic_expired_bundle_returns_null() = runTest {
        val expiredBundle = InAppStaticEntity(
            id = 0,
            createdAt = System.currentTimeMillis() - 25 * 60 * 60 * 1000L,
            blockJs = "old",
            blockCss = "old",
            inAppLibraryJs = "old"
        )
        db.dao.static.insert(expiredBundle)

        val result = StaticProcess.loadStatic(context)

        assertThat(result == null, `is`(true))
    }

    @Test
    fun isRetry_retry_returns_true() = runTest {
        StaticProcess.static = null

        val workerId = UUID.randomUUID()
        val shouldRetry = StaticProcess.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(true))
    }

    @Test
    fun isRetry_static_cached_returns_false() = runTest {
        StaticProcess.static = InAppStaticEntity(
            id = 0,
            createdAt = System.currentTimeMillis(),
            blockJs = "js",
            blockCss = "css",
            inAppLibraryJs = "lib"
        )

        val workerId = UUID.randomUUID()
        val shouldRetry = StaticProcess.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(false))
    }
}
