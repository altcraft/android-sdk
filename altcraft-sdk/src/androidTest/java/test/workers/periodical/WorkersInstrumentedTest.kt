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
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.coroutine.CancelWork
import com.altcraft.sdk.workers.periodical.CommonFunctions
import com.altcraft.sdk.workers.periodical.Workers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * WorkersInstrumentedTest
 *
 * Verifies periodic workers behavior in foreground/background:
 * - test_1: RetryPushEventWorker (background) calls awaitCancel, PushEvent.isRetry, clearOldPushEventsFromRoom → success.
 * - test_2: RetrySubscribeWorker (background) calls awaitCancel, PushSubscribe.isRetry → success.
 * - test_3: RetryUpdateWorker (background) calls awaitCancel, TokenUpdate.tokenUpdate → success.
 * - test_4: RetryMobileEventWorker (background) calls awaitCancel, MobileEvent.isRetry, clearOldMobileEventsFromRoom → success.
 * - test_5: RetryPushEventWorker (foreground) skips body → success.
 * - test_6: RetrySubscribeWorker (foreground) skips body → success.
 * - test_7: RetryUpdateWorker (foreground) skips body → success.
 * - test_8: RetryMobileEventWorker (foreground) skips body → success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkersInstrumentedTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(
            SubFunction,
            CommonFunctions,
            PushEvent,
            PushSubscribe,
            TokenUpdate,
            MobileEvent,
            RoomRequest,
            CancelWork,
            Events
        )
        coEvery { CommonFunctions.awaitCancel(any(), any()) } returns Unit
        every { Events.error(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: RetryPushEventWorker (background) calls retry and cleanup, returns success. */
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

    /** - test_2: RetrySubscribeWorker (background) calls retry, returns success. */
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

    /** - test_3: RetryUpdateWorker (background) calls tokenUpdate, returns success. */
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

    /** - test_4: RetryMobileEventWorker (background) calls retry and cleanup, returns success. */
    @Test
    fun retryMobileEventWorker_background_callsRetryAndCleanup_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns false
        coEvery { MobileEvent.isRetry(ctx) } returns false
        coEvery { RoomRequest.clearOldMobileEventsFromRoom(any()) } returns Unit

        val worker = TestListenableWorkerBuilder<Workers.RetryMobileEventWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { CommonFunctions.awaitCancel(ctx, any()) }
        coVerify(exactly = 1) { MobileEvent.isRetry(ctx) }
        coVerify(exactly = 1) { RoomRequest.clearOldMobileEventsFromRoom(any()) }
    }

    /** - test_5: RetryPushEventWorker (foreground) skips body, returns success. */
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

    /** - test_6: RetrySubscribeWorker (foreground) skips body, returns success. */
    @Test
    fun retrySubscribeWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetrySubscribeWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { PushSubscribe.isRetry(any()) }
    }

    /** - test_7: RetryUpdateWorker (foreground) skips body, returns success. */
    @Test
    fun retryUpdateWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetryUpdateWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { TokenUpdate.tokenUpdate(any()) }
    }

    /** - test_8: RetryMobileEventWorker (foreground) skips body, returns success. */
    @Test
    fun retryMobileEventWorker_foreground_skipsBody_success() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val worker = TestListenableWorkerBuilder<Workers.RetryMobileEventWorker>(ctx).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { CommonFunctions.awaitCancel(any(), any()) }
        coVerify(exactly = 0) { MobileEvent.isRetry(any()) }
        coVerify(exactly = 0) { RoomRequest.clearOldMobileEventsFromRoom(any()) }
    }
}