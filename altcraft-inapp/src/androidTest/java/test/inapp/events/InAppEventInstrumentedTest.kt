package test.inapp.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.data.Constants
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppEventDao
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.platform.event.EventProcess
import com.altcraft.sdk.inapp.platform.event.EventWorker
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.InAppEventType
import com.altcraft.sdk.inapp.platform.event.request.EventRequest
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class InAppEventInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
    private lateinit var dao: InAppEventDao
    private val testAcid = "acid-123"
    private val testDeviceId = "device-123"
    private val testSmid = "smid-001"

    private val testAuth = AuthenticationEntity(
        id = 0,
        anonimAcid = "anon-acid",
        acid = testAcid,
        deviceId = testDeviceId,
        currentUser = null,
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
        dao = db.dao.event

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

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

        mockkObject(EventRequest)
        coEvery { EventRequest.inAppEventRequest(any(), any()) } returns SDKEventModel.Event("ok")

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        coEvery { env.internalAuth() } returns testAuth
        every { Environment.create(any()) } returns env

        // EventProcess.logic reads the production singleton directly,
        // so it must be pointed at the in-memory database under test.
        mockkObject(DB.Companion)
        every { DB.getInstance(any()) } returns db
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /** Polls [condition] until it holds or the timeout elapses. */
    private suspend fun awaitTrue(
        timeoutMs: Long = 3000,
        condition: suspend () -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) break
            delay(50)
        }
        assertThat(condition(), `is`(true))
    }

    @Test
    fun sendInAppEvent_stores_event_and_enqueues_worker() = runBlocking {
        coEvery { EventRequest.inAppEventRequest(any(), any()) } returns retry(
            function = "inAppEventRequest"
        )

        EventProcess.sendInAppEvent(
            context = context,
            type = InAppEventType.CLICK,
            smid = testSmid,
            data = InAppModel.InAppEventData(code = 200, message = "ok")
        )

        // The event is stored immediately, before any network processing.
        awaitTrue { dao.getAll().any { it.smid == testSmid } }

        val events = dao.getAll()
        assertThat(events.first().smid, `is`(testSmid))
        assertThat(events.first().type, `is`(InAppEventType.CLICK.value))

        // The worker is enqueued for background processing.
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.IN_APP_EVENT_O_WORK_TAG).get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))

        // Processing the worker keeps the event stored on a retryable result.
        val result = TestListenableWorkerBuilder<EventWorker>(context)
            .build()
            .doWork()

        assertThat(result, `is`(ListenableWorker.Result.retry()))
        coVerify(exactly = 1) { EventRequest.inAppEventRequest(any(), any()) }
        assertThat(dao.getAll().isNotEmpty(), `is`(true))
    }

    @Test
    fun sendInAppEvent_null_smid_no_insert() = runBlocking {
        EventProcess.sendInAppEvent(
            context = context,
            type = InAppEventType.CLICK,
            smid = null,
            data = null
        )

        Thread.sleep(500)
        val events = dao.getAll()
        assertThat(events.isEmpty(), `is`(true))
    }

    @Test
    fun sendInAppEvent_success_removes_stored_event() = runBlocking {
        coEvery { EventRequest.inAppEventRequest(any(), any()) } returns SDKEventModel.Event("ok")

        EventProcess.sendInAppEvent(
            context = context,
            type = InAppEventType.SHOW,
            smid = testSmid,
            data = null
        )

        // The event is stored before any network processing: the test observes
        // the stored row before driving the worker that performs the request.
        awaitTrue { dao.getAll().any { it.smid == testSmid } }
        assertThat(dao.getAll().first().smid, `is`(testSmid))

        // Processing the worker sends the stored event and removes it on success.
        val result = TestListenableWorkerBuilder<EventWorker>(context)
            .build()
            .doWork()

        assertThat(result, `is`(ListenableWorker.Result.success()))
        awaitTrue { dao.getAll().isEmpty() }
    }

    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        dao.insert(
            InAppEventEntity(
                type = InAppEventType.CLICK.value,
                smid = testSmid,
                acid = testAcid,
                data = null
            )
        )

        coEvery { EventRequest.inAppEventRequest(any(), any()) } returns retry(
            function = "inAppEventRequest"
        )

        val shouldRetry = EventProcess.isRetry(context)

        assertThat(shouldRetry, `is`(true))
        val events = dao.getAll()
        assertThat(events.isNotEmpty(), `is`(true))
    }

    @Test
    fun isRetry_over_limit_entity_deleted() = runTest {
        dao.insert(
            InAppEventEntity(
                type = InAppEventType.CLICK.value,
                smid = testSmid,
                acid = testAcid,
                data = null,
                retryCount = 6,
                maxRetryCount = 5
            )
        )

        coEvery { EventRequest.inAppEventRequest(any(), any()) } returns retry(
            function = "inAppEventRequest"
        )

        val shouldRetry = EventProcess.isRetry(context)

        assertThat(shouldRetry, `is`(false))
        val events = dao.getAll()
        assertThat(events.isEmpty(), `is`(true))
    }
}
