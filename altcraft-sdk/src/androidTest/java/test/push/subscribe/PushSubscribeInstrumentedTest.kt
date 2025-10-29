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
import com.altcraft.sdk.auth.AuthManager

/**
 * PushSubscribeInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: pushSubscribe enqueues OneTimeWork with SUBSCRIBE_C_WORK_TAG and inserts SubscribeEntity.
 *  - test_2: isRetry returns true when subscribeRequest → Retry and isRetryLimit == false.
 *
 * Negative scenarios:
 *  - test_3: pushSubscribe with invalid customFields triggers Events.error and does not insert.
 *  - test_4: isRetry returns false when subscribeRequest → Retry and isRetryLimit == true.
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

        mockkObject(CommandQueue.SubscribeCommandQueue)
        every { CommandQueue.SubscribeCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns ConfigurationEntity(
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
        coEvery { Request.pushSubscribeRequest(any(), any()) } returns DataClasses.Event("ok")

        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /** - test_1: pushSubscribe enqueues work and inserts a row. */
    @Test
    fun pushSubscribe_enqueues_and_inserts() = runTest {
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

        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.SUBSCRIBE_C_WORK_TAG).get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_2: isRetry returns true when Retry and isRetryLimit == false. */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
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
        coEvery { Request.pushSubscribeRequest(any(), any()) } returns
                com.altcraft.sdk.data.retry(function = "subscribeRequest")
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        val shouldRetry = PushSubscribe.isRetry(context)

        assertThat(shouldRetry, `is`(true))
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))
    }

    /** - test_3: invalid customFields triggers Events.error and no insert. */
    @Test
    fun pushSubscribe_invalid_custom_fields_triggers_error_no_insert() = runTest {
        PushSubscribe.pushSubscribe(
            context = context,
            customFields = mapOf("bad" to mapOf("nested" to 123))
        )

        verify { Events.error(eq("pushSubscribe"), any()) }
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))
    }

    /** - test_4: isRetry returns false when Retry and isRetryLimit == true. */
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
        coEvery { Request.pushSubscribeRequest(any(), any()) } returns
                com.altcraft.sdk.data.retry(function = "subscribeRequest")
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns true

        val shouldRetry = PushSubscribe.isRetry(context)

        assertThat(shouldRetry, `is`(false))
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))
    }
}