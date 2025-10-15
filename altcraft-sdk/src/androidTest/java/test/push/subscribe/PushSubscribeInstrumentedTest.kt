package test.push.subscribe

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
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.Executors

// SDK imports
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.push.subscribe.PushSubscribe

/**
 * PushSubscribeInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: pushSubscribe enqueues a OneTimeWorkRequest with SUBSCRIBE_C_WORK_TAG and inserts a SubscribeEntity.
 *  - test_2: isRetry returns true when Request.subscribeRequest -> Retry event and RoomRequest.isRetryLimit == false.
 *
 * Negative scenarios:
 *  - test_3: pushSubscribe with invalid customFields triggers Events.error and does NOT insert.
 *  - test_4: isRetry returns false when Request.subscribeRequest -> Retry event and RoomRequest.isRetryLimit == true.
 */
@RunWith(AndroidJUnit4::class)
class PushSubscribeInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: SDKdb
    private lateinit var dao: DAO
    private val testUserTag = "user-tag-123"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // WorkManager test environment.
        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)

        // REAL in-memory DB of type SDKdb.
        db = Room.inMemoryDatabaseBuilder(context, SDKdb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()

        MockKAnnotations.init(this, relaxUnitFun = true)

        // SDKdb.getDb(context) -> our in-memory instance
        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns db

        // CommandQueue: run submitted suspend blocks immediately
        mockkObject(CommandQueue.SubscribeCommandQueue)
        every { CommandQueue.SubscribeCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        // Config
        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns
                ConfigurationEntity(
                    id = 0,
                    icon = null,
                    apiUrl = "https://api.example.com",
                    rToken = testUserTag,
                    appInfo = null,
                    usingService = false, // force CoroutineWorker path, not Android Service
                    serviceMessage = null,
                    pushReceiverModules = null,
                    providerPriorityList = null,
                    pushChannelName = null,
                    pushChannelDescription = null
                )

        // Environment helpers
        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

        // Events stubs (return real objects, not Unit)
        mockkObject(Events)
        every { Events.error(any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { DataClasses.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { DataClasses.Event(function = firstArg()) }

        // Network default: success (non-retry)
        mockkObject(Request)
        coEvery { Request.subscribeRequest(any(), any()) } returns DataClasses.Event("ok")

        // IMPORTANT: Do NOT call the real isRetryLimit by default — it triggers increaseRetryCount
        // which uses suspend DAO updates and can blow up in coroutine machinery here.
        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * test_1: Ensures pushSubscribe creates a DB row and enqueues Work with SUBSCRIBE_C_WORK_TAG.
     */
    @Test
    fun pushSubscribe_enqueues_and_inserts() = runTest {
        // Act
        PushSubscribe.pushSubscribe(
            context = context,
            sync = 1,
            status = Constants.SUBSCRIBED,
            customFields = mapOf("a" to 1, "b" to "x"),
            profileFields = mapOf("p" to true),
            cats = listOf(DataClasses.CategoryData(name = "news", active = true)),
            replace = true,
            skipTriggers = false
        )

        // Assert DB insert happened for user tag
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        // Assert WorkManager enqueued a job with the subscribe tag
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.SUBSCRIBE_C_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_3: Invalid customFields (nested object) → Events.error and no DB insert.
     */
    @Test
    fun pushSubscribe_invalid_custom_fields_triggers_error_no_insert() = runTest {
        // Act
        PushSubscribe.pushSubscribe(
            context = context,
            customFields = mapOf("bad" to mapOf("nested" to 123))
        )

        // Assert: error signaled and no rows for user tag
        verify { Events.error(eq("pushSubscribe"), any()) }
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))
    }

    /**
     * test_2: isRetry returns true when network returns Retry and isRetryLimit == false.
     */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        // Arrange: insert real SubscribeEntity
        dao.insertSubscribe(
            SubscribeEntity(
                userTag = testUserTag,
                status = Constants.SUBSCRIBED,
                sync = 1,
                profileFields = Json.parseToJsonElement("""{"p":true}"""),
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = listOf(DataClasses.CategoryData(name = "news", active = true)),
                replace = null,
                skipTriggers = null
            )
        )
        // Network → Retry event
        coEvery { Request.subscribeRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "subscribeRequest"
        )
        // Under limit for this test
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        // Act
        val shouldRetry = PushSubscribe.isRetry(context)

        // Assert
        assertThat(shouldRetry, `is`(true))
    }

    /**
     * test_4: isRetry returns false when network returns Retry and isRetryLimit == true.
     */
    @Test
    fun isRetry_over_limit_returns_false() = runTest {
        dao.insertSubscribe(
            SubscribeEntity(
                userTag = testUserTag,
                status = Constants.SUBSCRIBED,
                sync = 1,
                profileFields = null,
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = null,
                replace = null,
                skipTriggers = null
            )
        )
        coEvery { Request.subscribeRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "subscribeRequest"
        )
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns true
        val shouldRetry = PushSubscribe.isRetry(context)
        assertThat(shouldRetry, `is`(false))
    }
}
