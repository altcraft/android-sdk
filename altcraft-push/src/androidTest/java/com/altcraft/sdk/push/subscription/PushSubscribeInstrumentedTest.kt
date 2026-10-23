@file:Suppress("SpellCheckingInspection")

package com.altcraft.sdk.push.subscription

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
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.data.models.CoreModels.ProfileModel
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants
import com.altcraft.sdk.push.data.Preferenses
import com.altcraft.sdk.push.data.models.PushModels
import com.altcraft.sdk.push.data.room.DB
import com.altcraft.sdk.push.data.room.dao.PushSubscribeDao
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess.pushSubscribe
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess.isRetry
import com.altcraft.sdk.push.platform.subscription.subscribe.request.SubscriptionRequest
import com.altcraft.sdk.workers.onetime.Request as WorkerRequest
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
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
    private lateinit var db: DB
    private lateinit var dao: PushSubscribeDao
    private lateinit var env: Environment
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
        dao = db.dao.subscription

        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

        completeInitSignals()
        SubscriptionProcess.snapshotTaken.complete(Unit)

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every { Events.error(any(), any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every { Events.retry(any(), any(), any()) } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }

        mockkObject(SubscriptionRequest)
        coEvery { SubscriptionRequest.pushSubscribeRequest(any(), any()) } returns SDKEventModel.Event("ok")

        mockkObject(WorkerRequest)
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        every { env.room } returns db
        coEvery { env.userTag() } returns testUserTag
        coEvery { env.currentToken() } returns PushModels.TokenData(Constants.FCM_PROVIDER, "tkn")
        every { env.savedToken() } returns null
        every { Environment.create(any()) } returns env

        mockkObject(Preferenses)
        every { Preferenses.setCurrentToken(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /**
     * Reflectively completes the internal init barrier and retry-start signals so that
     * [com.altcraft.sdk.coordination.submitSdkCommand] does not block on their timeouts.
     */
    private fun completeInitSignals() {
        try {
            val initBarrierClass = Class.forName("com.altcraft.sdk.coordination.InitBarrier")
            val instance = initBarrierClass.getDeclaredField("INSTANCE").let {
                it.isAccessible = true
                it.get(null)
            }
            val current = initBarrierClass.getDeclaredMethod("current").let {
                it.isAccessible = true
                it.invoke(instance)
            }
            (current as? CompletableDeferred<Unit>)?.complete(Unit)
            val reserve = initBarrierClass.getDeclaredMethod("reserve").let {
                it.isAccessible = true
                it.invoke(instance)
            }
            (reserve as? CompletableDeferred<Unit>)?.complete(Unit)
        } catch (_: Exception) {
            // Reflection is best-effort; submitSdkCommand falls back to timeouts.
        }
        try {
            val initialOpsClass =
                Class.forName("com.altcraft.sdk.core.modules.ModuleRegistry")
            val instance = initialOpsClass.getDeclaredField("INSTANCE").let {
                it.isAccessible = true
                it.get(null)
            }
            val started = initialOpsClass.getDeclaredField("retryStartReady").let {
                it.isAccessible = true
                it.get(instance)
            }
            (started as? CompletableDeferred<Unit>)?.complete(Unit)
        } catch (_: Exception) {
            // Reflection is best-effort; submitSdkCommand falls back to timeouts.
        }
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

    @Test
    fun pushSubscribe_enqueues_and_inserts() = runBlocking {
        pushSubscribe(
            context = context,
            sync = true,
            status = Constants.SUBSCRIBED,
            customFields = mapOf("a" to 1, "b" to "x"),
            profileFields = mapOf("p" to true),
            cats = listOf(ProfileModel.CategoryData(name = "news", active = true)),
            replace = true,
            skipTriggers = false
        )

        var subscriptions = emptyList<PushSubscribeEntity>()
        for (i in 1..80) {
            Thread.sleep(250)
            subscriptions = dao.getAllEntitiesByTag(testUserTag)
            if (subscriptions.isNotEmpty()) break
        }
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.PUSH_SUBSCRIBE_O_WORK_TAG).get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    @Test
    fun isRetry_under_limit_returns_true() = runTest {
        dao.insert(
            PushSubscribeEntity(
                userTag = testUserTag,
                status = Constants.SUBSCRIBED,
                sync = true,
                profileFields = Json.parseToJsonElement("""{"p":true}"""),
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = listOf(ProfileModel.CategoryData(name = "news", active = true)),
                replace = null,
                skipTriggers = null
            )
        )

        coEvery { SubscriptionRequest.pushSubscribeRequest(any(), any()) } returns com.altcraft.sdk.data.models.retry(function = "subscribeRequest")
        coEvery { WorkerRequest.hasNewRequest(any(), any(), any()) } returns false

        val shouldRetry = isRetry(context)

        assertThat(shouldRetry, `is`(true))
        val subscriptions = dao.getAllEntitiesByTag(testUserTag)
        assertThat(subscriptions.isNotEmpty(), `is`(true))

        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    @Test
    fun pushSubscribe_invalid_custom_fields_triggers_error_no_insert() = runTest {
        pushSubscribe(
            context = context,
            status = Constants.SUBSCRIBED,
            customFields = mapOf("bad" to mapOf("nested" to 123))
        )

        eventually {
            verify { Events.error(eq("[altcraft-core] pushSubscribe"), any()) }
        }
        val subscriptions = dao.getAllEntitiesByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))
    }

    @Test
    fun isRetry_over_limit_returns_false() = runTest {
        dao.insert(
            PushSubscribeEntity(
                userTag = testUserTag,
                status = Constants.SUBSCRIBED,
                sync = true,
                profileFields = null,
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = null,
                replace = null,
                skipTriggers = null,
                retryCount = 6,
                maxRetryCount = 5
            )
        )

        coEvery { SubscriptionRequest.pushSubscribeRequest(any(), any()) } returns com.altcraft.sdk.data.models.retry(function = "subscribeRequest")

        val shouldRetry = isRetry(context)

        assertThat(shouldRetry, `is`(false))
        val subscriptions = dao.getAllEntitiesByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))

        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    @Test
    fun isRetry_success_setsCurrentToken_and_deletes_entity() = runTest {
        dao.insert(
            PushSubscribeEntity(
                userTag = testUserTag,
                status = Constants.SUBSCRIBED,
                sync = true,
                profileFields = null,
                customFields = Json.parseToJsonElement("""{"a":1}"""),
                cats = null,
                replace = null,
                skipTriggers = null
            )
        )

        coEvery { SubscriptionRequest.pushSubscribeRequest(any(), any()) } returns SDKEventModel.Event("ok")

        val shouldRetry = isRetry(context)

        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(any(), any()) }

        val subscriptions = dao.getAllEntitiesByTag(testUserTag)
        assertThat(subscriptions.isEmpty(), `is`(true))
    }
}
