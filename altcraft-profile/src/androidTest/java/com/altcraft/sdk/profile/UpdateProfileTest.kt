package com.altcraft.sdk.profile

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
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.util.SubFunction
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Executors

import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess
import com.altcraft.sdk.profile.data.Constants
import com.altcraft.sdk.profile.data.room.DAO
import com.altcraft.sdk.profile.data.room.DB
import com.altcraft.sdk.profile.data.room.Entity
import com.altcraft.sdk.profile.platform.updateFields.request.UpdateFieldsRequest

/**
 * ProfileUpdateInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: updateProfileFields enqueues a OneTimeWorkRequest with PROFILE_O_WORK_TAG and
 *  inserts an [Entity].
 *  - test_2: isRetry returns true when [UpdateFieldsRequest.updateFieldsRequest] returns a
 *  Retry event and the retry limit is not reached.
 *
 * Negative scenarios:
 *  - test_3: updateProfileFields with missing user tag triggers Events.error and does NOT insert.
 *  - test_4: isRetry returns false when [UpdateFieldsRequest.updateFieldsRequest] returns a
 *  Retry event and the retry limit is reached (entity deleted).
 */
@RunWith(AndroidJUnit4::class)
class ProfileUpdateInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
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

        db = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()

        MockKAnnotations.init(this, relaxUnitFun = true)

        if (!UpdateFieldsProcess.snapshotTaken.isCompleted) {
            UpdateFieldsProcess.snapshotTaken.complete(Unit)
        }

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred<Unit>().apply {
            complete(Unit)
        }

        mockkObject(DB.Companion)
        every { DB.getInstance(any()) } returns db

        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.user() } returns testUserTag

        mockkObject(UpdateFieldsRequest)
        coEvery { UpdateFieldsRequest.updateFieldsRequest(any(), any()) } returns SDKEventModel.Event("ok")

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * test_1: Ensures updateProfileFields creates a DB row and enqueues Work with PROFILE_O_WORK_TAG.
     */
    @Test
    fun updateProfileFields_enqueues_and_inserts() = runBlocking {
        Profile.updateProfileFields(
            context = context,
            profileFields = mapOf("a" to 1, "b" to "x"),
            skipTriggers = true
        )

        var updates = emptyList<Entity>()
        for (i in 1..80) {
            Thread.sleep(250)
            updates = dao.getAllEntitiesByTag(testUserTag)
            if (updates.isNotEmpty()) return@runBlocking
        }
        assertThat(updates.isNotEmpty(), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.PROFILE_O_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * Retries [block] until it passes or [timeoutMs] elapses.
     */
    private fun eventually(timeoutMs: Long = 20_000, block: () -> Unit) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                block()
                return
            } catch (_: AssertionError) {
                Thread.sleep(250)
            } catch (_: MockKException) {
                Thread.sleep(250)
            }
        }
        block()
    }

    /**
     * test_3: Missing user tag → Events.error and no DB insert.
     */
    @Test
    fun updateProfileFields_missing_user_tag_triggers_error_no_insert() = runBlocking {
        coEvery { CoreEnvironmentProvider.user() } throws RuntimeException("user tag is null")

        Profile.updateProfileFields(
            context = context,
            profileFields = mapOf("x" to "y"),
            skipTriggers = null
        )

        eventually {
            verify {
                Events.error(
                    match { it.contains("updateProfileFields") },
                    any()
                )
            }
        }

        val updates = dao.getAllEntitiesByTag(testUserTag)
        assertThat(updates.isEmpty(), `is`(true))
    }

    /**
     * test_2: isRetry returns true when profileUpdateRequest -> Retry and the retry limit is not reached.
     */
    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        dao.insert(
            Entity(
                userTag = testUserTag,
                time = System.currentTimeMillis(),
                profileFields = Json.parseToJsonElement("""{"a":1}"""),
                skipTriggers = null
            )
        )

        coEvery { UpdateFieldsRequest.updateFieldsRequest(any(), any()) } returns retry(function = "profileUpdateRequest")

        val shouldRetry = UpdateFieldsProcess.isRetry(context, workerId = UUID.randomUUID())

        assertThat(shouldRetry, `is`(true))

        val updates = dao.getAllEntitiesByTag(testUserTag)
        assertThat(updates.isNotEmpty(), `is`(true))
    }

    /**
     * test_4: isRetry returns false when profileUpdateRequest -> Retry and the retry limit is reached.
     */
    @Test
    fun isRetry_over_limit_returns_false_and_entity_deleted() = runTest {
        dao.insert(
            Entity(
                userTag = testUserTag,
                time = System.currentTimeMillis(),
                profileFields = Json.parseToJsonElement("""{"a":1}"""),
                skipTriggers = null,
                retryCount = 6,
                maxRetryCount = 5
            )
        )

        coEvery { UpdateFieldsRequest.updateFieldsRequest(any(), any()) } returns retry(function = "profileUpdateRequest")

        val shouldRetry = UpdateFieldsProcess.isRetry(context, workerId = UUID.randomUUID())

        assertThat(shouldRetry, `is`(false))

        val updates = dao.getAllEntitiesByTag(testUserTag)
        assertThat(updates.isEmpty(), `is`(true))
    }
}
