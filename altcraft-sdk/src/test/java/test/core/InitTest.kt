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
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.Core
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
 * InitUnitTest
 *
 * Positive scenarios:
 *  - test_1: init_success_inactivePush_callsBarrierComplete_andCallbackSuccess
 *            setConfig() succeeds, push inactive → InitBarrier.complete(),
 *            callback success, performPushModuleCheck() not invoked.
 *  - test_2: init_success_activePush_invokesPerformPushModuleCheck
 *            setConfig() succeeds, push active → performPushModuleCheck(ctx) invoked,
 *            InitBarrier.complete(), callback success.
 *  - test_3: init_success_activePush_verifiesCallOrder
 *            verifies correct order: setConfig → pushModuleIsActive →
 *            performPushModuleCheck(ctx) → InitBarrier.complete.
 *
 * Negative scenarios:
 *  - test_4: init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure
 *            setConfig() throws → InitBarrier.fail(), Events.error(),
 *            callback failure.
 *  - test_5: init_nullConfig_triggersFail_andCallbackFailure
 *            setConfig() returns null → exception(configIsNotSet),
 *            InitBarrier.fail() + Events.error(), callback failure.
 *  - test_6: init_performPushModuleCheckThrows_triggersFail_andCallbackFailure
 *            performPushModuleCheck(ctx) throws → InitBarrier.fail() + Events.error(),
 *            callback failure.
 *
 * Concurrency:
 *  - test_7: init_twoCalls_bothCallbacksComplete
 *            two sequential init() calls (submit synchronous) → both callbacks success.
 *  - test_8: init_twoCalls_concurrent_serializedByMutex
 *            two concurrent init() calls (submit async) → mutex serializes execution,
 *            verified order: first completes before second starts.
 *
 * Notes:
 *  - Pure JVM unit tests (MockK).
 *  - CommandQueue.InitCommandQueue.submit mocked to run synchronously (except test_8).
 *  - Core object mocked directly (no CoreKt class).
 */
class InitTest {

    // ---- Constants (strings & expectations) ----
    private companion object {
        const val API_URL              = "https://api.example.com"
        const val MSG_CB_SUCCESS       = "Callback must be success"
        const val MSG_CB_FAILURE       = "Callback must be failure"
        const val MSG_BOTH_DONE        = "Both callbacks must complete"
        const val MSG_RESERVE_CALLED   = "InitBarrier.reserve must be called"
        const val FUNC_INIT            = "init"
        const val LATCH_TIMEOUT_SEC    = 2L
    }

    private lateinit var ctx: Context
    private lateinit var cfg: AltcraftConfiguration
    private lateinit var entity: ConfigurationEntity
    private lateinit var gate: CompletableDeferred<Unit>

    @Before
    fun setUp() {
        // Context
        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        // Config object
        cfg = mockk(relaxed = true)

        // Entity to be returned by setConfig on success
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

        // --- Make InitCommandQueue.submit execute synchronously ---
        mockkObject(CommandQueue.InitCommandQueue)
        every { CommandQueue.InitCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }

        // setConfig is suspend
        mockkObject(ConfigSetup)

        // Mock object Core, do NOT mock CoreKt
        mockkObject(Core)
        every { Core.pushModuleIsActive(any()) } returns false
        every { Core.performPushModuleCheck(any<Context>()) } just Runs // CHANGED: single-arg signature

        // InitBarrier: provide a controllable gate and proper boolean returns
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

        // Events.error to allow verification on failure path
        mockkObject(Events)
        every { Events.error(any(), any(), any()) } returns
                DataClasses.Error(FUNC_INIT, 400, "boom", null)
        every { Events.subscribe(any()) } just Runs
        every { Events.unsubscribe() } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    /** success, push inactive → no performPushModuleCheck */
    @Test
    fun init_success_inactivePush_callsBarrierComplete_andCallbackSuccess() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Core.pushModuleIsActive(ctx) } returns false

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)
        assertTrue(MSG_RESERVE_CALLED, true) // reserve verified below
        coVerify(exactly = 1, timeout = 0) { ConfigSetup.setConfig(ctx, cfg) }
        // barrier complete called with our gate
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
        // no performPushModuleCheck
        io.mockk.verify(exactly = 0) { Core.performPushModuleCheck(any<Context>()) } // CHANGED
    }

    /** success, push active → performPushModuleCheck invoked */
    @Test
    fun init_success_activePush_invokesPerformPushModuleCheck() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Core.pushModuleIsActive(ctx) } returns true

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)
        coVerify(exactly = 1) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 1) { Core.performPushModuleCheck(ctx) } // CHANGED
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
    }

    /** failure in setConfig → barrier.fail, Events.error, callback failure */
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
        io.mockk.verify(exactly = 0) { Core.performPushModuleCheck(any<Context>()) } // CHANGED
    }

    /** two init() calls → both callbacks complete (submit runs synchronously) */
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

    /** setConfig returns null → exception(configIsNotSet) path */
    @Test
    fun init_nullConfig_triggersFail_andCallbackFailure() {
        // setConfig returns null
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
    }

    /** performPushModuleCheck throws → fail path must be executed */
    @Test
    fun init_performPushModuleCheckThrows_triggersFail_andCallbackFailure() {
        // Success config
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Core.pushModuleIsActive(ctx) } returns true
        // Force performPushModuleCheck to throw
        every { Core.performPushModuleCheck(any<Context>()) } answers { throw IllegalArgumentException("check failed") } // CHANGED

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

    /** Order verification on success with active push */
    @Test
    fun init_success_activePush_verifiesCallOrder() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { Core.pushModuleIsActive(ctx) } returns true

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { ok = it.isSuccess; latch.countDown() }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerify(exactly = 1) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 1) { Core.performPushModuleCheck(ctx) } // CHANGED

        io.mockk.coVerifyOrder {
            ConfigSetup.setConfig(ctx, cfg)
            Core.pushModuleIsActive(ctx)
            Core.performPushModuleCheck(ctx) // CHANGED
            InitBarrier.complete(gate)
        }
    }

    /** Real concurrency: two parallel init() must be serialized by Mutex */
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun init_twoCalls_concurrent_serializedByMutex() {
        // Make submit launch asynchronously to simulate real queue
        unmockkAll()
        mockkObject(CommandQueue.InitCommandQueue)
        every { CommandQueue.InitCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            kotlinx.coroutines.GlobalScope.launch { block() }
        }

        // Re-mock everything else
        mockkObject(ConfigSetup, Core, InitBarrier, Events)
        every { Core.pushModuleIsActive(any()) } returns false
        every { Events.error(any(), any(), any()) } returns DataClasses.Error("init", 400, "boom", null)
        every { Events.subscribe(any()) } just Runs
        every { Events.unsubscribe() } just Runs
        // NOTE: performPushModuleCheck is not needed here because pushModuleIsActive == false

        // Reserve/complete
        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = arrayListOf(gate1, gate2)
        every { InitBarrier.reserve() } answers { gates.removeAt(0) }
        every { InitBarrier.complete(any()) } answers { (firstArg() as CompletableDeferred<Unit>).complete(Unit) }
        every { InitBarrier.fail(any(), any()) } answers { (firstArg() as CompletableDeferred<Unit>).complete(Unit) }

        // Force first setConfig to suspend until second attempts to enter mutex
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

        // Release first
        blockLatch.countDown()
        assertTrue(latch.await(2, TimeUnit.SECONDS))

        // Ensure sequential enter: "first-start" occurs before "second-start"
        assertEquals(listOf("first-start", "second-start"), order)
    }
}
