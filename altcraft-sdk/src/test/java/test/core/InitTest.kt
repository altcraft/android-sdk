@file:Suppress("SpellCheckingInspection")

package test.core

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.core.InitialOperations
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.Events
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
 *  - test_2: init_success_invokesPerformInitOperations.
 *  - test_3: init_success_verifiesCallOrder.
 *
 * Negative scenarios:
 *  - test_4: init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure.
 *  - test_5: init_nullConfig_triggersFail_andCallbackFailure.
 *  - test_6: init_performInitOperationsThrows_triggersFail_andCallbackFailure.
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
    private lateinit var room: SDKdb

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        cfg = mockk(relaxed = true)
        every { cfg.getEnableLogging() } returns false

        entity = ConfigurationEntity(
            id = 1,
            icon = null,
            apiUrl = API_URL,
            rToken = "rt",
            appInfo = null,
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
        mockkObject(InitialOperations)
        every { InitialOperations.performInitOperations(any<Context>()) } just Runs

        mockkObject(Environment.Companion)
        room = mockk(relaxed = true)
        val env: Environment = mockk(relaxed = true)
        every { env.room } returns room
        every { Environment.create(any()) } returns env

        mockkObject(RoomRequest)
        coJustRun { RoomRequest.roomOverflowControl(any()) }

        mockkObject(InitBarrier)
        gate = CompletableDeferred()
        every { InitBarrier.reserve() } returns gate
        every { InitBarrier.complete(any()) } answers {
            firstArg<CompletableDeferred<Unit>>().complete(Unit)
        }
        every { InitBarrier.fail(any(), any()) } answers {
            firstArg<CompletableDeferred<Unit>>().completeExceptionally(secondArg())
        }

        mockkObject(Events)
        every { Events.error(any(), any()) } returns DataClasses.Error(FUNC_INIT, 400, "boom", null)
    }

    @After
    fun tearDown() = unmockkAll()

    /** test_1: init_success_callsBarrierComplete_andCallbackSuccess. */
    @Test
    fun init_success_callsBarrierComplete_andCallbackSuccess() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerify(exactly = 1) { ConfigSetup.setConfig(ctx, cfg) }
        coVerify(exactly = 1) { RoomRequest.roomOverflowControl(room) }
        io.mockk.verify(exactly = 1) { InitialOperations.performInitOperations(ctx) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
    }

    /** test_2: init_success_invokesPerformInitOperations. */
    @Test
    fun init_success_invokesPerformInitOperations() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { result ->
            ok = result.isSuccess
            latch.countDown()
        }

        assertTrue(MSG_CB_SUCCESS, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        io.mockk.verify(exactly = 1) { InitialOperations.performInitOperations(ctx) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate) }
        io.mockk.verify(exactly = 0) { InitBarrier.fail(any(), any()) }
    }

    /** test_3: init_success_verifiesCallOrder. */
    @Test
    fun init_success_verifiesCallOrder() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity

        val latch = CountDownLatch(1)
        var ok = false

        Init.init(ctx, cfg) { ok = it.isSuccess; latch.countDown() }

        assertTrue(latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS) && ok)

        coVerifyOrder {
            ConfigSetup.setConfig(ctx, cfg)
            RoomRequest.roomOverflowControl(room)
        }
        io.mockk.verifyOrder {
            InitialOperations.performInitOperations(ctx)
            InitBarrier.complete(gate)
        }
    }

    /** test_4: init_failure_fromSetConfig_callsBarrierFail_andCallbackFailure. */
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

        io.mockk.verify(exactly = 1) { Events.error(eq(FUNC_INIT), any()) }
        io.mockk.verify(exactly = 0) { InitialOperations.performInitOperations(any<Context>()) }
        coVerify(exactly = 0) { RoomRequest.roomOverflowControl(any()) }
    }

    /** test_5: init_nullConfig_triggersFail_andCallbackFailure. */
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
        io.mockk.verify(exactly = 1) { Events.error(eq(FUNC_INIT), any()) }

        io.mockk.verify(exactly = 0) { InitialOperations.performInitOperations(any<Context>()) }
        coVerify(exactly = 0) { RoomRequest.roomOverflowControl(any()) }
    }

    /** test_6: init_performInitOperationsThrows_triggersFail_andCallbackFailure. */
    @Test
    fun init_performInitOperationsThrows_triggersFail_andCallbackFailure() {
        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity
        every { InitialOperations.performInitOperations(any<Context>()) } answers { throw IllegalArgumentException("check failed") }

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
        io.mockk.verify(exactly = 1) { Events.error(eq(FUNC_INIT), any()) }
    }

    /** test_7: init_twoCalls_bothCallbacksComplete. */
    @Test
    fun init_twoCalls_bothCallbacksComplete() {
        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = ArrayDeque(listOf(gate1, gate2))
        every { InitBarrier.reserve() } answers { gates.removeFirst() }

        coEvery { ConfigSetup.setConfig(any(), any()) } returns entity

        val latch = CountDownLatch(2)
        var ok1 = false
        var ok2 = false

        Init.init(ctx, cfg) { ok1 = it.isSuccess; latch.countDown() }
        Init.init(ctx, cfg) { ok2 = it.isSuccess; latch.countDown() }

        assertTrue(MSG_BOTH_DONE, latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue(ok1 && ok2)
        coVerify(exactly = 2) { ConfigSetup.setConfig(ctx, cfg) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate1) }
        io.mockk.verify(exactly = 1) { InitBarrier.complete(gate2) }
    }

    /** test_8: init_twoCalls_concurrent_serializedByMutex. */
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun init_twoCalls_concurrent_serializedByMutex() {
        unmockkAll()

        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        cfg = mockk(relaxed = true)
        every { cfg.getEnableLogging() } returns false

        entity = ConfigurationEntity(
            id = 1,
            icon = null,
            apiUrl = API_URL,
            rToken = "rt",
            appInfo = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        mockkObject(CommandQueue.InitCommandQueue)
        every { CommandQueue.InitCommandQueue.submit(any()) } answers {
            val block = firstArg<suspend () -> Unit>()
            kotlinx.coroutines.GlobalScope.launch { block() }
        }

        mockkObject(ConfigSetup)
        mockkObject(InitialOperations)
        mockkObject(InitBarrier)
        mockkObject(Events)
        mockkObject(RoomRequest)
        mockkObject(Environment.Companion)

        room = mockk(relaxed = true)
        val env: Environment = mockk(relaxed = true)
        every { env.room } returns room
        every { Environment.create(any()) } returns env

        coJustRun { RoomRequest.roomOverflowControl(any()) }
        every { InitialOperations.performInitOperations(any<Context>()) } just Runs
        every { Events.error(any(), any()) } returns DataClasses.Error("init", 400, "boom", null)

        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val gates = ArrayDeque(listOf(gate1, gate2))

        every { InitBarrier.reserve() } answers { gates.removeFirst() }
        every { InitBarrier.complete(any()) } answers { firstArg<CompletableDeferred<Unit>>().complete(Unit) }
        every { InitBarrier.fail(any(), any()) } answers { firstArg<CompletableDeferred<Unit>>().complete(Unit) }

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
    }
}