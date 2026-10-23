@file:Suppress("SpellCheckingInspection")

package com.altcraft.sdk.target

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration as WMConfiguration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.target.data.Constants
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.EmailSubscription
import com.altcraft.sdk.target.data.models.TargetModels.UTM
import com.altcraft.sdk.target.data.room.DB
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.platform.target.TargetProcess
import com.altcraft.sdk.util.SubFunction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * TargetSendTargetInstrumentedTest
 *
 * Public-API safety coverage for [Target.sendTarget].
 *
 * Positive scenarios:
 *  - a valid payload is persisted atomically before the worker is enqueued.
 *
 * Negative scenarios:
 *  - an object-valued payload is rejected without any DB writes or work.
 */
@RunWith(AndroidJUnit4::class)
class TargetSendTargetInstrumentedTest {

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

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred<Unit>().apply { complete(Unit) }

        if (!TargetProcess.snapshotTaken.isCompleted) {
            TargetProcess.snapshotTaken.complete(Unit)
        }

        mockkObject(DB.Companion)
        every { DB.getInstance(any()) } returns db

        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.user() } returns testUserTag

        mockkObject(Events)
        every { Events.error(any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
    }

    @After
    fun tearDown() {
        TargetProcess.snapshotTaken = CompletableDeferred()
        db.close()
        unmockkAll()
    }

    private fun rows(): List<Entity> = runBlocking {
        db.dao().getAllByTagInternal(testUserTag)
    }

    /** Retries [block] until it passes or [timeoutMs] elapses. */
    private fun eventually(
        timeoutMs: Long = 10_000,
        block: () -> Unit
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                block()
                return
            } catch (t: Throwable) {
                last = t
                Thread.sleep(250)
            }
        }
        throw AssertionError(
            "condition not met within ${timeoutMs}ms",
            last
        )
    }

    /** A valid payload is persisted and a one-time worker is enqueued. */
    @Test
    fun sendTarget_validPayload_persistsAndEnqueues() {
        Target.sendTarget(
            context = context,
            sid = "pixel-1",
            eventName = "onAppOpen",
            sendMessageId = "mid-1",
            payload = mapOf("a" to 1),
            matching = mapOf("m" to "x"),
            matchingType = "1",
            profileFields = mapOf("p" to 2),
            subscription = EmailSubscription(
                resourceId = 1,
                email = "a@b.co",
                status = "active"
            ),
            utm = UTM(
                campaign = "campaign",
                source = "source"
            )
        )

        eventually {
            val stored = rows()
            assertThat(
                "A valid target request must be persisted before delivery",
                stored.isNotEmpty(),
                `is`(true)
            )
            val entity = stored.first()
            assertThat(entity.sid, `is`("pixel-1"))
            assertThat(entity.eventName, `is`("onAppOpen"))
            assertThat(entity.userTag, `is`(testUserTag))
        }

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.TARGET_O_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** An object-valued payload is rejected without any DB writes or work. */
    @Test
    fun sendTarget_invalidPayload_rejectedWithoutWrites() {
        Target.sendTarget(
            context = context,
            sid = "pixel-1",
            eventName = "onAppOpen",
            payload = mapOf("a" to mapOf("nested" to 1))
        )

        Thread.sleep(1_500)

        assertThat("An invalid payload must not be persisted", rows().isEmpty(), `is`(true))
    }
}
