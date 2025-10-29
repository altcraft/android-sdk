@file:Suppress("SpellCheckingInspection")

package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.core.Retry
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.sdk_events.Events
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * InitTest
 *
 * Covers init() flow, barrier signaling, retry kickoff, and concurrency.
 *
 * Positive scenarios:
 *  - test_1: init_success_inactivePush_callsBarrierComplete_andCallbackSuccess.
 *  - test_2: init_success_activePush_invokesPerformRetryOperations.
 *  - test_3: init_success_activePush_verifiesCallOrder.
 *
 * Negative scenarios:
 *  - test_4: init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure.
 *  - test_5: init_nullConfig_triggersFail_andCallbackFailure.
 *  - test_6: init_performRetryOperationsThrows_triggersFail_andCallbackFailure.
 *
 * Concurrency:
 *  - test_7: init_twoCalls_bothCallbacksComplete.
 *  - test_8: init_twoCalls_concurrent_serializedByMutex.
 */
class InitTest {

    private companion object {
        const val API_URL = "https://api.example.com"
        const val MSG_CB_SUCCESS = "Callback must be success"
        const val MSG_CB_FAILURE = "Callback must be failure"
        const val MSG_BOTH_DONE = "Both callbacks must complete"
        const val FUNC_INIT = "init"
        const val LATCH_TIMEOUT_SEC = 2L
    }

    private lateinit var ctx: Context
    private lateinit var cfg: AltcraftConfiguration
    private lateinit var entity: ConfigurationEntity
    private lateinit var gate: CompletableDeferred<Unit>

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        cfg = mockk(relaxed = true)

        entity = ConfigurationEntity(
            id = 1,
            icon = null,
            apiUrl = API_URL,
            rToken = "rt",
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        mockkObject(CommandQueue.InitCommandQueue)
        every { CommandQueue.InitCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        mockkObject(ConfigSetup)
        mockkObject(Retry)
        every { Retry.pushModuleIsActive(any()) } returns false
        every { Retry.performRetryOperations(any<Context>()) } just Runs

        mockkObject(InitBarrier)
        gate = CompletableDeferred()
        every { InitBarrier.reserve() } returns gate
        every { InitBarrier.complete(any()) } answers {
            val g = firstArg<CompletableDeferred<Unit>>()
            g.complete(Unit)
        }
        every { InitBarrier.fail(any(), any()) } answers {
            val g = firstArg<CompletableDeferred<Unit>>()
            val e = secondArg<Throwable>()
            g.completeExceptionally(e)
        }

        mockkObject(Events)
        every { Events.error(any(), any(), any()) } returns DataClasses.Error(FUNC_INIT, 400, "boom", null)
        every { Events.subscribe(any()) } just Runs
        every { Events.unsubscribe() } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: init_success_inactivePush_callsBarrierComplete_andCallbackSuccess. */
    @Test
    fun init_success_inactivePush_callsBarrierComplete_andCallbackSuccess() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Retry.pushModuleIsActive(ctx) } returns false

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)
        coVerify(exactly = 1, timeout = 0) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
        io.mockk.verify(exactly = 1) { Retry.performRetryOperations(ctx) }
    }

    /** - test_2: init_success_activePush_invokesPerformRetryOperations. */
    @Test
    fun init_success_activePush_invokesPerformRetryOperations() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Retry.pushModuleIsActive(ctx) } returns true

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)
        coVerify(exactly = 1) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 1) { Retry.performRetryOperations(ctx) }
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
    }

    /** - test_3: init_success_activePush_verifiesCallOrder. */
    @Test
    fun init_success_activePush_verifiesCallOrder() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Retry.pushModuleIsActive(ctx) } returns true

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { ok = it.isSuccess; latch.countDown() }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerify(exactly = 1) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 1) { Retry.performRetryOperations(ctx) }

        io.mockk.coVerifyOrder {
            ConfigSetup.setConfig(ctx, cfg)
            Retry.performRetryOperations(ctx)
            InitBarrier.complete(gate)
        }
    }

    /** - test_4: init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure. */
    @Test
    fun init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure() {
        coEvery { ConfigSetup.setConfig(any(), any()) } throws IllegalStateException("boom")

        val latch = CountDownLatch(1)
        var err: Throwable? = null

        Init.init(ctx, cfg) { result ->
            if (result.isFailure) err = result.exceptionOrNull()
            latch.countDown()
        }

        assertTrue(MSG_CB_FAILURE, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && err != null)
        io.mockk.verify(exactly = 1) { InitBarrier.reserve() }
        io.mockk.verify(exactly = 1) { InitBarrier.fail(gate, any()) }
        io.mockk.verify(exactly = 0) { InitBarrier.complete(any()) }
        io.mockk.verify(exactly = 1) { Events.error(eq(FUNC_INIT), any(), any()) }
        io.mockk.verify(exactly = 0) { Retry.performRetryOperations(any<Context>()) }
    }

    /** - test_5: init_nullConfig_triggersFail_andCallbackFailure. */
    @Test
    fun init_nullConfig_triggersFail_andCallbackFailure() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns null

        val latch = CountDownLatch(1)
        var err: Throwable? = null

        Init.init(ctx, cfg) { result ->
            if (result.isFailure) err = result.exceptionOrNull()
            latch.countDown()
        }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertNotNull(err)
        io.mockk.verify(exactly = 1) { InitBarrier.reserve() }
        io.mockk.verify(exactly = 1) { InitBarrier.fail(gate, any()) }
        io.mockk.verify(exactly = 0) { InitBarrier.complete(any()) }
        io.mockk.verify(exactly = 1) { Events.error(eq("init"), any(), any()) }
        io.mockk.verify(exactly = 0) { Retry.performRetryOperations(any<Context>()) }
    }

    /** - test_6: init_performRetryOperationsThrows_triggersFail_andCallbackFailure. */
    @Test
    fun init_performRetryOperationsThrows_triggersFail_andCallbackFailure() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Retry.pushModuleIsActive(ctx) } returns true
        every { Retry.performRetryOperations(any<Context>()) } answers {
            throw IllegalArgumentException("check failed")
        }

        val latch = CountDownLatch(1)
        var err: Throwable? = null

        Init.init(ctx, cfg) { result ->
            if (result.isFailure) err = result.exceptionOrNull()
            latch.countDown()
        }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertNotNull(err)
        io.mockk.verify(exactly = 1) { InitBarrier.reserve() }
        io.mockk.verify(exactly = 1) { InitBarrier.fail(gate, any()) }
        io.mockk.verify(exactly = 0) { InitBarrier.complete(any()) }
        io.mockk.verify(exactly = 1) { Events.error(eq("init"), any(), any()) }
    }

    /** - test_7: init_twoCalls_bothCallbacksComplete. */
    @Test
    fun init_twoCalls_bothCallbacksComplete() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity

        val latch = CountDownLatch(2)
        var ok1 = false
        var ok2 = false

        Init.init(ctx, cfg) { ok1 = it.isSuccess; latch.countDown() }
        Init.init(ctx, cfg) { ok2 = it.isSuccess; latch.countDown() }

        assertTrue(MSG_BOTH_DONE, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue(ok1 && ok2)
        coVerify(exactly = 2) { ConfigSetup.setConfig(ctx, cfg) }
    }

    /** - test_8: init_twoCalls_concurrent_serializedByMutex. */
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun init_twoCalls_concurrent_serializedByMutex() {
        unmockkAll()
        mockkObject(CommandQueue.InitCommandQueue)
        every { CommandQueue.InitCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            kotlinx.coroutines.GlobalScope.launch { block() }
        }

        mockkObject(ConfigSetup, Retry, InitBarrier, Events)
        every { Retry.pushModuleIsActive(any()) } returns false
        every { Retry.performRetryOperations(any<Context>()) } just Runs
        every { Events.error(any(), any(), any()) } returns DataClasses.Error("init", 400, "boom", null)
        every { Events.subscribe(any()) } just Runs
        every { Events.unsubscribe() } just Runs

        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = arrayListOf(gate1, gate2)
        every { InitBarrier.reserve() } answers { gates.removeAt(0) }
        every { InitBarrier.complete(any()) } answers {
            (firstArg() as CompletableDeferred<Unit>).complete(Unit)
        }
        every { InitBarrier.fail(any(), any()) } answers {
            (firstArg() as CompletableDeferred<Unit>).complete(Unit)
        }

        val blockLatch = CountDownLatch(1)
        val startedSecond = CountDownLatch(1)
        val order = mutableListOf<String>()

        coEvery { ConfigSetup.setConfig(any(), any()) } coAnswers {
            if (order.isEmpty()) {
                order += "first-start"
                startedSecond.countDown()
                blockLatch.await(1, TimeUnit.SECONDS)
                entity
            } else {
                order += "second-start"
                entity
            }
        }

        val latch = CountDownLatch(2)
        Init.init(ctx, cfg) { latch.countDown() }
        startedSecond.await(1, TimeUnit.SECONDS)
        Init.init(ctx, cfg) { latch.countDown() }

        blockLatch.countDown()
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("first-start", "second-start"), order)
        ":)"
    }
}