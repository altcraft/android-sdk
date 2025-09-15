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
import com.altcraft.sdk.events.Events
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
 *
 */
@RunWith(AndroidJUnit4::class)
class PushSubscribeInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: SDKdb
    private lateinit var dao: DAO

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // WorkManager test environment.
        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)

        // REAL in-memory production DB (type SDKdb).
        db = Room.inMemoryDatabaseBuilder(context, SDKdb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()

        MockKAnnotations.init(this, relaxUnitFun = true)

        // --- SDKdb.getDb(context) -> our in-memory instance ---
        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns db

        // --- Command queue: submit takes a suspend lambda; run it synchronously ---
        mockkObject(CommandQueue.SubscribeCommandQueue)
        every { CommandQueue.SubscribeCommandQueue.submit(any()) } answers {
            val suspendBlock = firstArg<Any?>()
            runBlocking {
                @Suppress("UNCHECKED_CAST")
                (suspendBlock as (suspend () -> Unit)).invoke()
            }
        }

        // --- ConfigSetup as object (suspend getConfig) ---
        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns
                ConfigurationEntity(
                    id = 0,
                    icon = null,
                    apiUrl = "https://api.example.com",
                    rToken = "user-tag-123",
                    appInfo = null,
                    usingService = false, // force CoroutineWorker path, not Android Service
                    serviceMessage = null,
                    pushReceiverModules = null,
                    providerPriorityList = null,
                    pushChannelName = null,
                    pushChannelDescription = null
                )

        // --- SubFunction as object ---
        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

        // --- Events as object: RETURN proper objects, not Unit! ---
        mockkObject(Events)
        // 2-arg overloads
        coEvery { Events.error(any(), any()) } answers {
            DataClasses.Error(function = firstArg())
        }
        coEvery { Events.retry(any(), any()) } answers {
            DataClasses.RetryError(function = firstArg())
        }
        coEvery { Events.event(any(), any()) } answers {
            DataClasses.Event(function = firstArg())
        }
        coEvery { Events.error(any(), any(), any()) } answers {
            DataClasses.Error(function = firstArg())
        }
        coEvery { Events.retry(any(), any(), any()) } answers {
            DataClasses.RetryError(function = firstArg())
        }
        coEvery { Events.event(any(), any(), any()) } answers {
            DataClasses.Event(function = firstArg())
        }

        // --- Request as object (network) ---
        mockkObject(Request)
        // default: non-Retry (success path)
        coEvery { Request.subscribeRequest(any(), any()) } returns mockk(relaxed = true)

        // --- RoomRequest as object---
        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any()) } answers { callOriginal() }
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * test_1
     * Ensures pushSubscribe creates a DB row and enqueues a Work with SUBSCRIBE_C_WORK_TAG.
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

        // Assert DB insert happened (entity exists for tag)
        val exists = dao.subscriptionsExistsByTag("user-tag-123")
        assertThat(exists, `is`(true))

        // Assert WorkManager enqueued a job with the subscribe tag
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.SUBSCRIBE_C_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_3
     * Invalid customFields (nested object) must trigger Events.error and no DB insert.
     */
    @Test
    fun pushSubscribe_invalid_custom_fields_triggers_error_no_insert() = runTest {
        // Act: nested object inside customFields → fieldsIsObjects == true
        PushSubscribe.pushSubscribe(
            context = context,
            customFields = mapOf("bad" to mapOf("nested" to 123))
        )

        // Assert: error signaled and no rows for tag
        coVerify(timeout = 2_000) { Events.error(eq("pushSubscribe"), any()) }
        val exists = dao.subscriptionsExistsByTag("user-tag-123")
        assertThat(exists, `is`(false))
    }

    /**
     * test_2
     * isRetry returns true when network returns a Retry event and isRetryLimit == false.
     */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        // Arrange: insert real SubscribeEntity
        dao.insertSubscribe(
            SubscribeEntity(
                userTag = "user-tag-123",
                status = Constants.SUBSCRIBED,
                sync = 1,
                profileFields = Json.parseToJsonElement("""{"p":true}"""),
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = listOf(DataClasses.CategoryData(name = "news", active = true)),
                replace = null,
                skipTriggers = null
            )
        )

        // Network → Retry event (class), NOT boolean!
        coEvery { Request.subscribeRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "subscribeRequest"
        )

        // Limit not reached
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        // Act
        val shouldRetry = PushSubscribe.isRetry(context)

        // Assert
        assertThat(shouldRetry, `is`(true))
    }

    /**
     * test_4
     * isRetry returns false when network returns a Retry event and isRetryLimit == true.
     */
    @Test
    fun isRetry_over_limit_returns_false() = runTest {
        // Arrange: insert real SubscribeEntity
        dao.insertSubscribe(
            SubscribeEntity(
                userTag = "user-tag-123",
                status = Constants.SUBSCRIBED,
                sync = 1,
                profileFields = null,
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = null,
                replace = null,
                skipTriggers = null
            )
        )

        // Network → Retry event
        coEvery { Request.subscribeRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "subscribeRequest"
        )

        // Limit reached
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns true

        // Act
        val shouldRetry = PushSubscribe.isRetry(context)

        // Assert
        assertThat(shouldRetry, `is`(false))
    }
}
