package test.workers.coroutine

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.util.Log
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

import com.altcraft.sdk.data.Constants.AUTH_REQUEST_O_WORK_TAG
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.workers.CancelWork

/**
 * CancelWorkInstrumentedTest
 *
 * Positive scenario:
 *  - test_1: cancelWorkerTask() cancels jobs with AUTH_REQUEST_O_WORK_TAG.
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
        every { Events.error(any(), any()) } returns SDKEventModel.Error("err")
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
        unmockkAll()
    }

    private fun enqueueAuth() {
        val req = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .addTag(AUTH_REQUEST_O_WORK_TAG).build()
        wm.enqueue(req).result.get()
    }

    private fun assertAllCancelled(tag: String) {
        val infos = wm.getWorkInfosByTag(tag).get()
        if (infos.isNotEmpty()) {
            assertThat(
                infos.all { it.state == WorkInfo.State.CANCELLED || it.state == WorkInfo.State.SUCCEEDED },
                `is`(true)
            )
        }
    }

    /** - test_1: cancelWorkerTask() cancels AUTH_REQUEST_O_WORK_TAG. */
    @Test
    fun cancelAuthWorkerTask_cancels() {
        enqueueAuth()

        runBlocking { CancelWork.cancelWorkerTask(context, listOf(AUTH_REQUEST_O_WORK_TAG)) }

        assertAllCancelled(AUTH_REQUEST_O_WORK_TAG)
    }
}

class TestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
