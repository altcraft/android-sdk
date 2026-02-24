package test.profile

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
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Executors

import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.coordination.withInitReady
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.core.InitialOperations
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.ProfileUpdateEntity
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.profile.ProfileUpdate
import com.altcraft.sdk.sdk_events.Events
import kotlinx.coroutines.CompletableDeferred

/**
 * ProfileUpdateInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: updateProfileFields enqueues a OneTimeWorkRequest with PR_UPDATE_C_WORK_TAG and
 *  inserts a ProfileUpdateEntity.
 *  - test_2: isRetry returns true when Request.request(ProfileUpdateEntity) ->
 *  Retry event and RoomRequest.isRetryLimit == false.
 *
 * Negative scenarios:
 *  - test_3: updateProfileFields with missing config triggers Events.error and does NOT insert.
 *  - test_4: isRetry returns false when Request.request(ProfileUpdateEntity) ->
 *  Retry event and RoomRequest.isRetryLimit == true (entity is deleted by isRetryLimit).
 */
@RunWith(AndroidJUnit4::class)
class ProfileUpdateInstrumentedTest {

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

        mockkObject(CommandQueue.ProfileUpdateCommandQueue)
        every { CommandQueue.ProfileUpdateCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        mockkObject(InitBarrier)

        every { InitBarrier.current() } returns CompletableDeferred<Unit>().apply {
            complete(Unit)
        }

        mockkObject(InitialOperations)
        coEvery { InitialOperations.awaitProfileUpdateRetryStarted() } just Runs

        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns
                ConfigurationEntity(
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

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { DataClasses.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { DataClasses.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { DataClasses.Event(function = firstArg()) }

        mockkObject(Request)
        coEvery { Request.request(any(), any<ProfileUpdateEntity>()) } returns DataClasses.Event("ok")

        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any<ProfileUpdateEntity>()) } returns false
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * test_1: Ensures updateProfileFields creates a DB row and enqueues Work with PR_UPDATE_C_WORK_TAG.
     */
    @Test
    fun updateProfileFields_enqueues_and_inserts() = runTest {
        ProfileUpdate.updateProfileFields(
            context = context,
            profileFields = mapOf("a" to 1, "b" to "x"),
            skipTriggers = true
        )

        val updates = dao.allProfileUpdatesByTag(testUserTag)
        assertThat(updates.isNotEmpty(), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.PR_UPDATE_C_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_3: Missing config → Events.error and no DB insert.
     */
    @Test
    fun updateProfileFields_missing_config_triggers_error_no_insert() = runTest {
        coEvery { ConfigSetup.getConfig(any()) } returns null

        ProfileUpdate.updateProfileFields(
            context = context,
            profileFields = mapOf("x" to "y"),
            skipTriggers = null
        )

        verify { Events.error(eq("updateProfile"), any()) }

        val updates = dao.allProfileUpdatesByTag(testUserTag)
        assertThat(updates.isEmpty(), `is`(true))
    }

    /**
     * test_2: isRetry returns true when Request.request(ProfileUpdateEntity) -> Retry and retry limit not reached.
     */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        dao.insertProfileUpdate(
            ProfileUpdateEntity(
                userTag = testUserTag,
                time = System.currentTimeMillis(),
                profileFields = Json.parseToJsonElement("""{"a":1}"""),
                skipTriggers = null
            )
        )

        coEvery { Request.request(any(), any<ProfileUpdateEntity>()) } returns retry(function = "request")
        coEvery { RoomRequest.isRetryLimit(any(), any<ProfileUpdateEntity>()) } returns false

        val shouldRetry = ProfileUpdate.isRetry(context, workerId = UUID.randomUUID())

        assertThat(shouldRetry, `is`(true))

        val updates = dao.allProfileUpdatesByTag(testUserTag)
        assertThat(updates.isNotEmpty(), `is`(true))
    }

    /**
     * test_4: isRetry returns false when Request.request(ProfileUpdateEntity) -> Retry and RoomRequest.isRetryLimit == true (entity deleted).
     */
    @Test
    fun isRetry_over_limit_returns_false_and_entity_deleted() = runTest {
        dao.insertProfileUpdate(
            ProfileUpdateEntity(
                userTag = testUserTag,
                time = System.currentTimeMillis(),
                profileFields = Json.parseToJsonElement("""{"a":1}"""),
                skipTriggers = null,
                retryCount = 6,
                maxRetryCount = 5
            )
        )

        coEvery { Request.request(any(), any<ProfileUpdateEntity>()) } returns retry(function = "request")

        unmockkObject(RoomRequest)
        mockkObject(RoomRequest)
        coEvery { RoomRequest.isRetryLimit(any(), any<ProfileUpdateEntity>()) } coAnswers { callOriginal() }

        val shouldRetry = ProfileUpdate.isRetry(context, workerId = UUID.randomUUID())

        assertThat(shouldRetry, `is`(false))

        val updates = dao.allProfileUpdatesByTag(testUserTag)
        assertThat(updates.isEmpty(), `is`(true))
    }
}
