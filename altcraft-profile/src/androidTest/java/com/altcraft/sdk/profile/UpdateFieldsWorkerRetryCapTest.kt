package com.altcraft.sdk.profile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsWorker
import com.altcraft.sdk.util.SubFunction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UpdateFieldsWorkerRetryCapTest
 *
 * Verifies the bounded retry budget of the profile one-time worker.
 *
 * Positive scenarios:
 *  - test_1: retries while the attempt budget is available.
 *
 * Negative scenarios:
 *  - test_2: stops without processing once the budget is exhausted.
 */
@RunWith(AndroidJUnit4::class)
class UpdateFieldsWorkerRetryCapTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns false

        mockkObject(UpdateFieldsProcess)
        coEvery { UpdateFieldsProcess.isRetry(any(), any()) } returns true
    }

    @After
    fun tearDown() = unmockkAll()

    /** test_1: retries while the attempt budget is available. */
    @Test
    fun retries_withinBudget() = runBlocking {
        val worker = TestListenableWorkerBuilder<UpdateFieldsWorker>(context)
            .setRunAttemptCount(0)
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    /** test_2: stops without processing once the budget is exhausted. */
    @Test
    fun stops_whenBudgetExhausted() = runBlocking {
        val worker = TestListenableWorkerBuilder<UpdateFieldsWorker>(context)
            .setRunAttemptCount(15)
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify(exactly = 0) { UpdateFieldsProcess.isRetry(any(), any()) }
    }
}
