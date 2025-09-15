package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.util.Log
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// SDK imports
import com.altcraft.sdk.data.Constants.PUSH_EVENT_C_WORK_TAG
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.Constants.UPDATE_C_WORK_TAG
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.workers.coroutine.CancelWork
import com.altcraft.sdk.workers.coroutine.Request

/**
 * CancelWorkInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: cancelSubscribeWorkerTask() cancels jobs with SUBSCRIBE_C_WORK_TAG and calls onComplete.
 *  - test_2: cancelUpdateWorkerTask() cancels jobs with UPDATE_C_WORK_TAG and calls onComplete.
 *  - test_3: cancelPushEventWorkerTask() cancels jobs with PUSH_EVENT_C_WORK_TAG and calls onComplete.
 *  - test_4: cancelCoroutineWorkersTask() cancels all three tags and calls onComplete.
 *
 */
@RunWith(AndroidJUnit4::class)
class CancelWorkInstrumentedTest {

    private lateinit var context: Context
    private lateinit var wm: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        wm = WorkManager.getInstance(context)
        wm.cancelAllWork().result.get()

        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(Events)
        every { Events.error(any(), any()) } returns DataClasses.Error("err")
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
        unmockkAll()
        runCatching { unmockkStatic(WorkManager::class) }
    }

    // ---------------------------
    // helpers
    // ---------------------------

    private fun enqueueSubscribe() { wm.enqueue(Request.subscribeRequest()).result.get() }
    private fun enqueueUpdate() { wm.enqueue(Request.updateRequest()).result.get() }
    private fun enqueuePushEvent() { wm.enqueue(Request.pushEventRequest()).result.get() }

    private fun assertAllCancelled(tag: String) {
        val infos = wm.getWorkInfosByTag(tag).get()
        if (infos.isNotEmpty()) {
            assertThat(infos.all { it.state == WorkInfo.State.CANCELLED }, `is`(true))
        }
    }

    // ---------------------------
    // Positive tests
    // ---------------------------

    /**
     * cancelSubscribeWorkerTask() cancels jobs with SUBSCRIBE_C_WORK_TAG and calls onComplete
     */
    @Test
    fun cancelSubscribeWorkerTask_cancels_and_calls_onComplete() {
        enqueueSubscribe()

        val latch = CountDownLatch(1)
        CancelWork.cancelSubscribeWorkerTask(context) { latch.countDown() }

        val completed = latch.await(2, TimeUnit.SECONDS)
        assertThat(completed, `is`(true))
        assertAllCancelled(SUBSCRIBE_C_WORK_TAG)
    }

    /**
     * cancelUpdateWorkerTask() cancels jobs with UPDATE_C_WORK_TAG and calls onComplete
     */
    @Test
    fun cancelUpdateWorkerTask_cancels_and_calls_onComplete() {
        enqueueUpdate()

        val latch = CountDownLatch(1)
        CancelWork.cancelUpdateWorkerTask(context) { latch.countDown() }

        val completed = latch.await(2, TimeUnit.SECONDS)
        assertThat(completed, `is`(true))
        assertAllCancelled(UPDATE_C_WORK_TAG)
    }

    /**
     * cancelPushEventWorkerTask() cancels jobs with PUSH_EVENT_C_WORK_TAG and calls onComplete
     */
    @Test
    fun cancelPushEventWorkerTask_cancels_and_calls_onComplete() {
        enqueuePushEvent()

        val latch = CountDownLatch(1)
        CancelWork.cancelPushEventWorkerTask(context) { latch.countDown() }

        val completed = latch.await(2, TimeUnit.SECONDS)
        assertThat(completed, `is`(true))
        assertAllCancelled(PUSH_EVENT_C_WORK_TAG)
    }

    /**
     * cancelCoroutineWorkersTask() cancels all three tags and calls onComplete
     */
    @Test
    fun cancelCoroutineWorkersTask_cancels_all_and_calls_onComplete() {
        enqueueSubscribe()
        enqueueUpdate()
        enqueuePushEvent()

        val latch = CountDownLatch(1)
        CancelWork.cancelCoroutineWorkersTask(context) { latch.countDown() }

        val completed = latch.await(3, TimeUnit.SECONDS)
        assertThat(completed, `is`(true))

        assertAllCancelled(SUBSCRIBE_C_WORK_TAG)
        assertAllCancelled(UPDATE_C_WORK_TAG)
        assertAllCancelled(PUSH_EVENT_C_WORK_TAG)
    }
}
