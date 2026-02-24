@file:Suppress("SpellCheckingInspection")

package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.altcraft.sdk.coordination.ForegroundBarrier
import com.altcraft.sdk.core.InitialOperations
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.profile.ProfileUpdate
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.periodical.CommonFunctions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * InitialOperationsTest
 *
 * Positive scenarios:
 * - test_1: foreground=true + push active schedules 6 tasks.
 * - test_2: foreground=true + push inactive schedules 3 tasks.
 *
 * Negative scenarios:
 * - test_3: foreground=false schedules nothing.
 */
class InitialOperationsTest {

    private companion object {
        private const val MSG_TIMEOUT = "Expected tasks to be scheduled in time"
        private const val TIMEOUT_SEC = 2L
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var ctx: Context

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Looper::class)
        val mainLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mainLooper

        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        InitialOperations.initControl.set(false)

        ctx = mockk(relaxed = true)

        mockkObject(ForegroundBarrier)
        everyForegroundExecutesWith(true)

        mockkObject(TokenManager)
        mockkObject(TokenUpdate)
        mockkObject(PushEvent)
        mockkObject(MobileEvent)
        mockkObject(PushSubscribe)
        mockkObject(ProfileUpdate)
        mockkObject(CommonFunctions)

        coEvery { TokenManager.pushModuleIsActive(any()) } returns true
        coEvery { TokenUpdate.pushTokenUpdate(any()) } returns true
        coEvery { PushEvent.isRetry(any()) } returns false
        coEvery { MobileEvent.isRetry(any()) } returns false
        coEvery { PushSubscribe.isRetry(any()) } returns false
        coEvery { ProfileUpdate.isRetry(any()) } returns false
        coEvery { CommonFunctions.periodicalWorkerControl(any()) } returns Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
        InitialOperations.initControl.set(false)
    }

    /** - test_1: foreground=true + push active schedules 6 tasks. */
    @Test
    fun performInitOperations_foreground_true_push_active_schedules_all_tasks() = runTest(testDispatcher) {
        val latch = CountDownLatch(6)

        coEvery { TokenManager.pushModuleIsActive(any()) } returns true

        coEvery { TokenUpdate.pushTokenUpdate(any()) } coAnswers { latch.countDown(); true }
        coEvery { PushEvent.isRetry(any()) } coAnswers { latch.countDown(); false }
        coEvery { MobileEvent.isRetry(any()) } coAnswers { latch.countDown(); false }
        coEvery { PushSubscribe.isRetry(any()) } coAnswers { latch.countDown(); false }
        coEvery { ProfileUpdate.isRetry(any()) } coAnswers { latch.countDown(); false }
        coEvery { CommonFunctions.periodicalWorkerControl(any()) } coAnswers { latch.countDown(); Unit }

        InitialOperations.performInitOperations(ctx)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(MSG_TIMEOUT, latch.await(TIMEOUT_SEC, TimeUnit.SECONDS))

        coVerify(exactly = 1) { TokenUpdate.pushTokenUpdate(ctx) }
        coVerify(exactly = 1) { PushEvent.isRetry(ctx) }
        coVerify(exactly = 1) { MobileEvent.isRetry(ctx) }
        coVerify(exactly = 1) { PushSubscribe.isRetry(ctx) }
        coVerify(exactly = 1) { ProfileUpdate.isRetry(ctx) }
        coVerify(exactly = 1) { CommonFunctions.periodicalWorkerControl(ctx) }
    }

    /** - test_2: foreground=true + push inactive schedules 3 tasks. */
    @Test
    fun performInitOperations_foreground_true_push_inactive_schedules_only_non_push() = runTest(testDispatcher) {
        val latch = CountDownLatch(3)

        coEvery { TokenManager.pushModuleIsActive(any()) } returns false

        coEvery { CommonFunctions.periodicalWorkerControl(any()) } coAnswers { latch.countDown(); Unit }
        coEvery { ProfileUpdate.isRetry(any()) } coAnswers { latch.countDown(); false }
        coEvery { MobileEvent.isRetry(any()) } coAnswers { latch.countDown(); false }

        InitialOperations.performInitOperations(ctx)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(MSG_TIMEOUT, latch.await(TIMEOUT_SEC, TimeUnit.SECONDS))

        coVerify(exactly = 0) { TokenUpdate.pushTokenUpdate(any()) }
        coVerify(exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(exactly = 0) { PushSubscribe.isRetry(any()) }

        coVerify(exactly = 1) { CommonFunctions.periodicalWorkerControl(ctx) }
        coVerify(exactly = 1) { ProfileUpdate.isRetry(ctx) }
        coVerify(exactly = 1) { MobileEvent.isRetry(ctx) }
    }

    /** - test_3: foreground=false schedules nothing. */
    @Test
    fun performInitOperations_foreground_false_schedules_nothing() = runTest(testDispatcher) {
        everyForegroundExecutesWith(false)

        InitialOperations.performInitOperations(ctx)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { TokenUpdate.pushTokenUpdate(any()) }
        coVerify(exactly = 0) { PushEvent.isRetry(any()) }
        coVerify(exactly = 0) { MobileEvent.isRetry(any()) }
        coVerify(exactly = 0) { PushSubscribe.isRetry(any()) }
        coVerify(exactly = 0) { ProfileUpdate.isRetry(any()) }
        coVerify(exactly = 0) { CommonFunctions.periodicalWorkerControl(any()) }
    }

    private fun everyForegroundExecutesWith(value: Boolean) {
        every { ForegroundBarrier.foreground(any()) } answers {
            val block = firstArg<suspend (Boolean) -> Unit>()
            kotlinx.coroutines.runBlocking { block(value) }
        }
    }
}