@file:Suppress("SpellCheckingInspection")

package test.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.events.Events
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * InitTest
 *
 * Covers init() flow, barrier signaling, error handling, and concurrency.
 *
 * Positive scenarios:
 *  - test_1: init_success_callsBarrierComplete_andCallbackSuccess.
 *  - test_2: init_success_invokesNotifyInitialized.
 *  - test_3: init_success_verifiesCallOrder.
 *
 * Negative scenarios:
 *  - test_4: init_failure_fromSetConfig_callsCallbackFailure.
 *  - test_6: init_notifyInitializedThrows_triggersCallbackFailure.
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
    private lateinit var gate: CompletableDeferred<Unit>
    private lateinit var room: DB

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        cfg = mockk(relaxed = true)
        every { cfg.enableLogging } returns false

        mockkObject(ConfigManager)
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs

        mockkObject(ModuleRegistry)
        coEvery { ModuleRegistry.configureModules(any(), any()) } just Runs
        coEvery { ModuleRegistry.notifyInitialized(any()) } just Runs

        mockkObject(Environment.Companion)
        room = mockk(relaxed = true)
        val env: Environment = mockk(relaxed = true)
        every { env.room } returns room
        every { Environment.create(any()) } returns env

        mockkObject(DB)
        every { DB.getDb(any()) } returns room

        mockkObject(InitBarrier)
        gate = CompletableDeferred()
        every { InitBarrier.reserve() } returns gate
        every { InitBarrier.initBarrierComplete(any()) } answers {
            firstArg<CompletableDeferred<Unit>>().complete(Unit)
        }

        mockkObject(Events)
        every { Events.error(any(), any()) } returns SDKEventModel.Error(
            FUNC_INIT,
            event = null,
            eventMessage = "boom",
            eventValue = null
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: init_success_callsBarrierComplete_andCallbackSuccess. */
    @Test
    fun init_success_callsBarrierComplete_andCallbackSuccess() {
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerify(timeout = 2000, exactly = 1) { ConfigManager.setConfig(ctx, cfg) }
        coVerify(timeout = 2000, exactly = 1) { ModuleRegistry.configureModules(ctx, cfg) }
        coVerify(timeout = 2000, exactly = 1) { ModuleRegistry.notifyInitialized(ctx) }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate) }
    }

    /** test_2: init_success_invokesNotifyInitialized. */
    @Test
    fun init_success_invokesNotifyInitialized() {
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerify(exactly = 1) { ModuleRegistry.notifyInitialized(ctx) }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate) }
    }

    /** test_3: init_success_verifiesCallOrder. */
    @Test
    fun init_success_verifiesCallOrder() {
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) {
            ok = it.isSuccess
            latch.countDown()
        }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerifyOrder {
            ConfigManager.setConfig(ctx, cfg)
            ModuleRegistry.configureModules(ctx, cfg)
            ModuleRegistry.notifyInitialized(ctx)
        }

        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate) }
    }

    /** test_4: init_failure_fromSetConfig_callsCallbackFailure. */
    @Test
    fun init_failure_fromSetConfig_callsCallbackFailure() {
        coEvery { ConfigManager.setConfig(any(), any()) } throws IllegalStateException("boom")

        val latch = CountDownLatch(1)
        var err: Throwable? = null

        Init.init(ctx, cfg) { result ->
            if (result.isFailure) err = result.exceptionOrNull()
            latch.countDown()
        }

        assertTrue(MSG_CB_FAILURE, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && err != null)

        verify(exactly = 1) { InitBarrier.reserve() }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate) }
        verify(exactly = 1) { Events.error(eq("[altcraft-core] $FUNC_INIT"), any()) }
        coVerify(exactly = 0) { ModuleRegistry.configureModules(any(), any()) }
        coVerify(exactly = 0) { ModuleRegistry.notifyInitialized(any()) }
    }

    /** test_6: init_notifyInitializedThrows_triggersCallbackFailure. */
    @Test
    fun init_notifyInitializedThrows_triggersCallbackFailure() {
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs
        coEvery {
            ModuleRegistry.notifyInitialized(any())
        } throws IllegalArgumentException("check failed")

        val latch = CountDownLatch(1)
        var err: Throwable? = null

        Init.init(ctx, cfg) { result ->
            if (result.isFailure) err = result.exceptionOrNull()
            latch.countDown()
        }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertNotNull(err)

        verify(exactly = 1) { InitBarrier.reserve() }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate) }
        verify(exactly = 1) { Events.error(eq("[altcraft-core] $FUNC_INIT"), any()) }
    }

    /** test_7: init_twoCalls_bothCallbacksComplete. */
    @Test
    fun init_twoCalls_bothCallbacksComplete() {
        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = ArrayDeque(listOf(gate1, gate2))

        every { InitBarrier.reserve() } answers { gates.removeFirst() }
        coEvery { ConfigManager.setConfig(any(), any()) } just Runs

        val latch = CountDownLatch(2)
        var ok1 = false
        var ok2 = false

        Init.init(ctx, cfg) {
            ok1 = it.isSuccess
            latch.countDown()
        }

        Init.init(ctx, cfg) {
            ok2 = it.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_BOTH_DONE, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue(ok1 && ok2)

        coVerify(exactly = 2) { ConfigManager.setConfig(ctx, cfg) }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate1) }
        verify(exactly = 1) { InitBarrier.initBarrierComplete(gate2) }
    }

    /** test_8: init_twoCalls_concurrent_serializedByMutex. */
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun init_twoCalls_concurrent_serializedByMutex() {
        unmockkAll()

        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        cfg = mockk(relaxed = true)
        every { cfg.enableLogging } returns false

        mockkObject(ConfigManager)
        mockkObject(InitBarrier)
        mockkObject(Events)
        mockkObject(ModuleRegistry)
        coEvery { ModuleRegistry.configureModules(any(), any()) } just Runs
        coEvery { ModuleRegistry.notifyInitialized(any()) } just Runs
        mockkObject(Environment.Companion)

        room = mockk(relaxed = true)
        val env: Environment = mockk(relaxed = true)
        every { env.room } returns room
        every { Environment.create(any()) } returns env

        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = ArrayDeque(listOf(gate1, gate2))

        every { InitBarrier.reserve() } answers { gates.removeFirst() }
        every { InitBarrier.initBarrierComplete(any()) } answers {
            firstArg<CompletableDeferred<Unit>>().complete(Unit)
        }

        val blockLatch = CountDownLatch(1)
        val startedFirst = CountDownLatch(1)
        val order = mutableListOf<String>()

        coEvery { ConfigManager.setConfig(any(), any()) } coAnswers {
            if (order.isEmpty()) {
                order += "first-start"
                startedFirst.countDown()
                blockLatch.await(1, TimeUnit.SECONDS)
            } else {
                order += "second-start"
            }
        }

        val latch = CountDownLatch(2)

        Init.init(ctx, cfg) {
            latch.countDown()
        }

        startedFirst.await(1, TimeUnit.SECONDS)

        Init.init(ctx, cfg) {
            latch.countDown()
        }

        blockLatch.countDown()

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("first-start", "second-start"), order)
    }
}