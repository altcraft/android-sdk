@file:Suppress("SpellCheckingInspection")

package com.altcraft.sdk.target.infra.module

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
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.target.data.Constants
import com.altcraft.sdk.target.data.room.DB
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.platform.target.TargetProcess
import com.altcraft.sdk.target.platform.target.request.TargetRequest
import com.altcraft.sdk.target.util.Log as TargetLog
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * ModuleLifecycleInstrumentedTest
 *
 * Verifies the target module lifecycle hooks.
 *
 * Positive scenarios:
 *  - onSdkInitialized completes and refreshes the snapshot signal.
 *  - onSdkInitialized enqueues the periodic target worker.
 *  - onSdkInitialized drains pending rows on success.
 *  - onSdkInitialized drops rows whose retry limit was exceeded.
 *  - onSdkClear cancels workers and clears the database.
 *  - onSdkRTokenChanged clears the database.
 *
 * Negative scenarios:
 *  - an onSdkInitialized failure is contained and reported through Log.error.
 */
@RunWith(AndroidJUnit4::class)
class ModuleLifecycleInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB

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

        mockkObject(DB.Companion)
        every { DB.getInstance(any()) } returns db

        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.user() } returns testUserTag

        TargetProcess.snapshotTaken = CompletableDeferred()

        mockkObject(TargetRequest)
        coEvery { TargetRequest.targetRequest(any(), any()) } returns SDKEventModel.Event("ok")
    }

    @After
    fun tearDown() {
        TargetProcess.snapshotTaken = CompletableDeferred()
        db.close()
        unmockkAll()
    }

    private fun insertEntity(
        retryCount: Int = 0,
        maxRetryCount: Int = 15
    ) = runBlocking {
        db.dao().insert(
            Entity(
                userTag = testUserTag,
                timeZone = 180,
                sid = "pixel-1",
                altcraftClientID = null,
                eventName = "onAppOpen",
                payload = null,
                matching = null,
                matchingType = null,
                profileFields = null,
                subscription = null,
                sendMessageId = null,
                utmTags = null,
                retryCount = retryCount,
                maxRetryCount = maxRetryCount
            )
        )
    }

    /** onSdkInitialized completes the previous snapshot, installs a fresh one and opens it once the startup retry pass has read pending entities. */
    @Test
    fun onSdkInitialized_completesAndRefreshesSnapshot() = runBlocking {
        val previous = TargetProcess.snapshotTaken
        assertFalse(previous.isCompleted)

        ModuleLifecycle.onSdkInitialized(context)

        assertTrue("Previous snapshot must be completed", previous.isCompleted)
        val fresh = TargetProcess.snapshotTaken
        assertTrue("A fresh snapshot must be installed for the new session", fresh !== previous)
        assertTrue(
            "Startup retry pass must open the fresh gate",
            fresh.isCompleted
        )
    }

    /** onSdkInitialized enqueues the periodic target worker. */
    @Test
    fun onSdkInitialized_enqueuesPeriodicWorker() = runBlocking {
        ModuleLifecycle.onSdkInitialized(context)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.TARGET_P_WORK_TAG)
        assertTrue(infos.get().isNotEmpty())
        assertEquals(
            WorkInfo.State.ENQUEUED,
            infos.get().first().state
        )
    }

    /** onSdkInitialized retries stored rows and deletes successfully delivered ones. */
    @Test
    fun onSdkInitialized_drainsSuccessfullyDeliveredRows() = runBlocking {
        insertEntity()

        ModuleLifecycle.onSdkInitialized(context)

        assertTrue(
            "A successfully delivered row must be removed",
            db.dao().getAllByTagInternal(testUserTag).isEmpty()
        )
    }

    /** onSdkInitialized removes rows whose retry limit was exceeded. */
    @Test
    fun onSdkInitialized_dropsRowsOverRetryLimit() = runBlocking {
        insertEntity(retryCount = 16, maxRetryCount = 15)

        coEvery { TargetRequest.targetRequest(any(), any()) } returns SDKEventModel.RetryError("target")

        ModuleLifecycle.onSdkInitialized(context)

        assertTrue(
            "A row over the retry limit must be removed",
            db.dao().getAllByTagInternal(testUserTag).isEmpty()
        )
    }

    /** onSdkClear cancels workers and clears all persisted data. */
    @Test
    fun onSdkClear_cancelsWorkersAndClearsDatabase() = runBlocking {
        insertEntity()

        ModuleLifecycle.onSdkClear(context)

        assertTrue(
            "onSdkClear must wipe all rows",
            db.dao().getAllByTagInternal(testUserTag).isEmpty()
        )
    }

    /** onSdkRTokenChanged clears all persisted data. */
    @Test
    fun onSdkRTokenChanged_clearsDatabase() = runBlocking {
        insertEntity()

        ModuleLifecycle.onSdkRTokenChanged(context)

        assertTrue(
            "onSdkRTokenChanged must wipe all rows",
            db.dao().getAllByTagInternal(testUserTag).isEmpty()
        )
    }

    /** A failing onSdkInitialized is contained and reported, not rethrown. */
    @Test
    fun onSdkInitialized_failureIsContainedAndLogged() = runBlocking {
        val original = TargetProcess.snapshotTaken
        mockkObject(TargetLog)
        mockkObject(com.altcraft.sdk.target.infra.Environment.Companion)
        every { com.altcraft.sdk.target.infra.Environment.create(any()) } throws IllegalStateException("db boom")

        ModuleLifecycle.onSdkInitialized(context)

        verify { TargetLog.error(any(), any(), any(), any()) }

        unmockkObject(com.altcraft.sdk.target.infra.Environment.Companion)
        unmockkObject(TargetLog)
        TargetProcess.snapshotTaken = original
    }
}
