@file:Suppress("SpellCheckingInspection")

package test.push.subscribe

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.coroutine.Request as WorkerRequest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

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
        every { Events.retry(any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { DataClasses.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { DataClasses.Event(function = firstArg()) }

        mockkObject(Request)
        coEvery { Request.request(any(), any()) } returns DataClasses.Event("ok")

        mockkObject(RoomRequest)
        coEvery { RoomRequest.allSubscriptionsByTag(any(), any()) } coAnswers {
            dao.allSubscriptionsByTag(secondArg())
        }
        coEvery { RoomRequest.entityDelete(any(), any()) } coAnswers {
            val entity = secondArg<SubscribeEntity>()
            dao.deleteSubscribeById(entity.requestID)
        }
        coEvery { RoomRequest.entityInsert(any(), any()) } coAnswers {
            val entity = secondArg<SubscribeEntity>()
            dao.insertSubscribe(entity)
        }
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        mockkObject(WorkerRequest)
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        mockkObject(Environment)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        coEvery { env.userTag() } returns testUserTag
        coEvery { env.token() } returns DataClasses.TokenData(Constants.FCM_PROVIDER, "tkn")
        every { Environment.create(any()) } returns env

        mockkObject(Preferenses)
        every { Preferenses.setCurrentToken(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

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

        coEvery { Request.request(any(), any()) } returns com.altcraft.sdk.data.retry(function = "subscribeRequest")
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        val shouldRetry = PushSubscribe.isRetry(context)

        assertThat(shouldRetry, `is`(true))
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

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

        coEvery { Request.request(any(), any()) } returns com.altcraft.sdk.data.retry(function = "subscribeRequest")
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns true

        val shouldRetry = PushSubscribe.isRetry(context)

        assertThat(shouldRetry, `is`(false))
        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    @Test
    fun isRetry_success_setsCurrentToken_and_deletes_entity() = runTest {
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

        coEvery { Request.request(any(), any()) } returns DataClasses.Event("ok")

        val shouldRetry = PushSubscribe.isRetry(context)

        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(any(), any()) }

        val subscriptions = dao.allSubscriptionsByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))
    }
}