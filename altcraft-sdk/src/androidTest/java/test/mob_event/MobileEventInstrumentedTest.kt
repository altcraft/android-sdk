package test.mob_event

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import kotlinx.serialization.json.Json

// SDK imports
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.device.DeviceInfo

/**
 * MobileEventInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: sendMobileEvent enqueues a OneTimeWorkRequest with MOBILE_EVENT_C_WORK_TAG and
 *  inserts a MobileEventEntity.
 *  - test_2: isRetry returns true when Request.mobileEventRequest ->
 *  Retry event and RoomRequest.isRetryLimit == false.
 *
 * Negative scenarios:
 *  - test_3: sendMobileEvent with invalid payload triggers Events.error and does NOT insert.
 *  - test_4: isRetry returns false when Request.mobileEventRequest ->
 *  Retry event and RoomRequest.isRetryLimit == true (entity is deleted by isRetryLimit).
 */
@RunWith(AndroidJUnit4::class)
class MobileEventInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: SDKdb
    private lateinit var dao: DAO
    private val testUserTag = "user-tag-123"
    private val testSid = "pixel-001"
    private val testEvent = "app_open"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)

        db = Room.inMemoryDatabaseBuilder(context, SDKdb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns db

        mockkObject(CommandQueue.MobileEventCommandQueue)
        every { CommandQueue.MobileEventCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns
                ConfigurationEntity(
                    id = 0,
                    icon = null,
                    apiUrl = "https://api.example.com",
                    rToken = testUserTag,
                    appInfo = null,
                    usingService = false,
                    serviceMessage = null,
                    pushReceiverModules = null,
                    providerPriorityList = null,
                    pushChannelName = null,
                    pushChannelDescription = null
                )

        mockkObject(AuthManager)
        every { AuthManager.getUserTag(any()) } returns testUserTag

        mockkObject(DeviceInfo)
        every { DeviceInfo.getTimeZoneForMobEvent() } returns 0

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every {
            Events.retry(
                any(),
                any()
            )
        } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { DataClasses.Event(function = firstArg()) }
        every {
            Events.error(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Error(function = firstArg()) }
        every {
            Events.retry(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.RetryError(function = firstArg()) }
        every {
            Events.event(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Event(function = firstArg()) }

        mockkObject(Request)
        coEvery { Request.mobileEventRequest(any(), any()) } returns DataClasses.Event("ok")

        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * test_1: Ensures sendMobileEvent creates a DB row and enqueues Work with MOBILE_EVENT_C_WORK_TAG.
     */
    @Test
    fun sendMobileEvent_enqueues_and_inserts() = runTest {
        // Act.
        MobileEvent.sendMobileEvent(
            context = context,
            sid = testSid,
            eventName = testEvent,
            sendMessageId = "smid-1",
            payloadFields = mapOf("a" to 1, "b" to "x"),
            matching = mapOf("k" to "v"),
            matchingType = "rtoken",
            profileFields = mapOf("p" to true),
            subscription = null,
            utmTags = DataClasses.UTM(source = "src"),
            altcraftClientID = "ac-id-1"
        )

        val events = dao.allMobileEventsByTag(testUserTag)
        assertThat(events.isNotEmpty(), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.MOBILE_EVENT_C_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_3: Invalid payload (nested object) → Events.error and no DB insert.
     */
    @Test
    fun sendMobileEvent_invalid_payload_triggers_error_no_insert() = runTest {
        // Act.
        MobileEvent.sendMobileEvent(
            context = context,
            sid = testSid,
            eventName = testEvent,
            payloadFields = mapOf("bad" to mapOf("nested" to 123))
        )

        verify { Events.error(eq("sendMobileEvent"), any()) }
        val events = dao.allMobileEventsByTag(testUserTag)
        assertThat(events.isEmpty(), `is`(true))
    }

    /**
     * test_2: isRetry returns true when network returns Retry and RoomRequest.isRetryLimit == false.
     */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        dao.insertMobileEvent(
            MobileEventEntity(
                userTag = testUserTag,
                timeZone = 0,
                sid = testSid,
                altcraftClientID = "ac-id-2",
                eventName = testEvent,
                payload = Json.parseToJsonElement("""{"a":1}""").toString(),
                matching = null,
                matchingType = "rtoken",
                profileFields = null,
                subscription = null,
                sendMessageId = "smid-2",
                utmTags = null
            )
        )

        coEvery { Request.mobileEventRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "mobileEventRequest"
        )

        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        val shouldRetry = MobileEvent.isRetry(context)

        assertThat(shouldRetry, `is`(true))

        val events = dao.allMobileEventsByTag(testUserTag)
        assertThat(events.isNotEmpty(), `is`(true))
    }

    /**
     * test_4: isRetry returns false when network returns Retry and RoomRequest.isRetryLimit == true.
     */
    @Test
    fun isRetry_over_limit_returns_false_and_entity_deleted() = runTest {
        dao.insertMobileEvent(
            MobileEventEntity(
                userTag = testUserTag,
                timeZone = 0,
                sid = testSid,
                altcraftClientID = "ac-id-3",
                eventName = testEvent,
                payload = Json.parseToJsonElement("""{"a":1}""").toString(),
                matching = null,
                matchingType = "rtoken",
                profileFields = null,
                subscription = null,
                sendMessageId = "smid-3",
                utmTags = null,
                retryCount = 6,
                maxRetryCount = 5
            )
        )

        coEvery { Request.mobileEventRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "mobileEventRequest"
        )

        unmockkObject(RoomRequest)
        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any()) } coAnswers { callOriginal() }

        val shouldRetry = MobileEvent.isRetry(context)

        assertThat(shouldRetry, `is`(false))
        val events = dao.allMobileEventsByTag(testUserTag)
        assertThat(events.isEmpty(), `is`(true))
    }
}