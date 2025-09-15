@file:Suppress("SpellCheckingInspection")

package test.workers.periodical

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.coroutine.CancelWork
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.Workers
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * WorkersInstrumentedTest
 *
 * What this class verifies:
 *  test_1: RetryPushEventWorker in background → calls awaitCancel, PushEvent.isRetry, and cleanup → returns success.
 *  test_2: RetrySubscribeWorker in background → calls awaitCancel and PushSubscribe.isRetry → returns success.
 *  test_3: RetryUpdateWorker in background → calls awaitCancel and TokenUpdate.tokenUpdate → returns success.
 *  test_4: TokenCheckWorker in background → calls TokenManager.getCurrentToken → returns success.
 *  test_5: RetryPushEventWorker in foreground → skips body (no calls) → returns success.
 *  test_6: RetrySubscribeWorker in foreground → skips body (no calls) → returns success.
 *  test_7: RetryUpdateWorker in foreground → skips body (no calls) → returns success.
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkersInstrumentedTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()

        // Mock dependent singletons/objects
        mockkObject(SubFunction)          // isAppInForegrounded()
        mockkObject(CommonFunctions)      // awaitCancel()
        mockkObject(PushEvent)            // isRetry()
        mockkObject(PushSubscribe)        // isRetry()
        mockkObject(TokenUpdate)          // tokenUpdate()
        mockkObject(TokenManager)         // getCurrentToken()
        mockkObject(RoomRequest)          // clearOldPushEventsFromRoom()
        mockkObject(CancelWork)           // cancel*WorkerTask()
        mockkObject(Events)               // error() to silence logs

        // Default: make awaitCancel a no-op that immediately resumes.
        coEvery { CommonFunctions.awaitCancel(any(), any()) } returns Unit

        // Silence Events.error/retry during tests.
        every { Events.error(any(), any(), any()) } answers {
            // Return a DataClasses.Error if your Events.error expects to return one.
            // Here just a relaxed stub:
            mockk(relaxed = true)
        }
    }

    @After
    fun tearDown() = unmockkAll()

    // ---------- test_1 ----------

    /** test_1: RetryPushEventWorker (background) performs awaitCancel, push retry, and cleanup. */
    @Test
    fun retryPushEventWorker_background_callsRetryAndCleanup_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { PushEvent.isRetry(ctx) } returns true
        coEvery { RoomRequest.clearOldPushEventsFromRoom(any()) } returns Unit

        val worker = TestListenableWorkerBuilder<Workers.RetryPushEventWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { CommonFunctions.awaitCancel(ctx, any()) }
        coVerify(exactly = 1) { PushEvent.isRetry(ctx) }
        coVerify(exactly = 1) { RoomRequest.clearOldPushEventsFromRoom(any()) }
    }

    // ---------- test_2 ----------

    /** test_2: RetrySubscribeWorker (background) performs awaitCancel and subscription retry. */
    @Test
    fun retrySubscribeWorker_background_callsRetry_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { PushSubscribe.isRetry(ctx) } returns false

        val worker = TestListenableWorkerBuilder<Workers.RetrySubscribeWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { CommonFunctions.awaitCancel(ctx, any()) }
        coVerify(exactly = 1) { PushSubscribe.isRetry(ctx) }
    }

    // ---------- test_3 ----------

    /** test_3: RetryUpdateWorker (background) performs awaitCancel and tokenUpdate. */
    @Test
    fun retryUpdateWorker_background_callsTokenUpdate_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { TokenUpdate.tokenUpdate(ctx) } returns Unit

        val worker = TestListenableWorkerBuilder<Workers.RetryUpdateWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { CommonFunctions.awaitCancel(ctx, any()) }
        coVerify(exactly = 1) { TokenUpdate.tokenUpdate(ctx) }
    }

    // ---------- test_4 ----------

    /** test_4: TokenCheckWorker (background) requests current token and succeeds. */
    @Test
    fun tokenCheckWorker_background_requestsToken_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { TokenManager.getCurrentToken(ctx) } returns DataClasses.TokenData(
            FCM_PROVIDER,
            "token"
        )
        val worker = TestListenableWorkerBuilder<Workers.TokenCheckWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { TokenManager.getCurrentToken(ctx) }
    }

    // ---------- test_5 ----------

    /** test_5: RetryPushEventWorker (foreground) skips body, returns success. */
    @Test
    fun retryPushEventWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetryPushEventWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(exactly = 0) { RoomRequest.clearOldPushEventsFromRoom(any()) }
    }

    // ---------- test_6 ----------

    /** test_6: RetrySubscribeWorker (foreground) skips body, returns success. */
    @Test
    fun retrySubscribeWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetrySubscribeWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { PushSubscribe.isRetry(any()) }
    }

    // ---------- test_7 ----------

    /** test_7: RetryUpdateWorker (foreground) skips body, returns success. */
    @Test
    fun retryUpdateWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetryUpdateWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }
}
