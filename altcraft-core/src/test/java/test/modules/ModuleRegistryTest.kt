@file:Suppress("SpellCheckingInspection")

package test.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.core.modules.ModuleLifecycle
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.events.Events
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ModuleRegistryTest
 *
 * Covers modular configuration application and error isolation.
 *
 * Positive scenarios:
 *  - test_1: configureModules() applies configurations and marks modules ready.
 *  - test_2: A module without configuration is still ready.
 *
 * Negative scenarios:
 *  - test_3: A failing module does not cancel configuration of other modules.
 *  - test_4: A failing module does not receive onSdkInitialized().
 */
class ModuleRegistryTest {

    private companion object {
        const val MODULE_A = "module-a"
        const val MODULE_B = "module-b"
        const val MSG_EXPECT_CONFIGURED = "Expected module to be configured"
        const val MSG_EXPECT_NOT_CONFIGURED = "Expected module not to be configured"
        const val MSG_EXPECT_INITIALIZED = "Expected module to be initialized"
        const val MSG_EXPECT_NOT_INITIALIZED = "Expected module not to be initialized"
        const val MSG_EXPECT_ERROR = "Expected a module configuration error"
    }

    private lateinit var context: Context
    private val registeredModules = mutableListOf<String>()

    /** Module configuration fake. */
    private class FakeConfiguration(
        override val moduleId: String
    ) : ModuleConfiguration

    /**
     * Module fake with configurable validation and configuration failures.
     */
    private class FakeModule(
        override val moduleId: String,
        private val failConfigure: Boolean = false
    ) : AltcraftModule {
        var configuredConfig: ModuleConfiguration? = null
        var initialized = false
        var cleared = false

        override suspend fun onSdkInitialized(context: Context) {
            initialized = true
        }

        override suspend fun onSdkClear(context: Context) {
            cleared = true
        }

        override suspend fun configure(
            context: Context,
            configuration: ModuleConfiguration
        ) {
            if (failConfigure) throw IllegalStateException("configure boom")
            configuredConfig = configuration
        }
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context

        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)

        mockkObject(ModuleLifecycle)
        coEvery { ModuleLifecycle.onSdkInitialized(any()) } just Runs
    }

    @After
    fun tearDown() {
        registeredModules.forEach {
            ModuleRegistry.unregister(it)
        }
        registeredModules.clear()
        unmockkAll()
    }

    private fun register(module: AltcraftModule) {
        ModuleRegistry.register(module)
        registeredModules.add(module.moduleId)
    }

    private fun configuration(module: FakeModule) = AltcraftConfiguration.Builder(
        apiUrl = "https://api.example.com"
    ).addModuleConfiguration(
        FakeConfiguration(module.moduleId)
    ).build()!!

    private fun emptyConfiguration() = AltcraftConfiguration.Builder(
        apiUrl = "https://api.example.com"
    ).build()!!

    /** test_1: configureModules() applies configurations and marks modules ready. */
    @Test
    fun configureModules_appliesAndInitializes() = runTest {
        val moduleA = FakeModule(MODULE_A)
        register(moduleA)

        ModuleRegistry.configureModules(
            context, configuration(moduleA)
        )
        ModuleRegistry.notifyInitializedInternal(context)

        assertNotNull(MSG_EXPECT_CONFIGURED, moduleA.configuredConfig)
        assertTrue(MSG_EXPECT_INITIALIZED, moduleA.initialized)
    }

    /** test_2: A module without configuration is ready. */
    @Test
    fun configureModules_optionalWithoutConfig_isReady() = runTest {
        val moduleB = FakeModule(MODULE_B)
        register(moduleB)

        ModuleRegistry.configureModules(
            context, emptyConfiguration()
        )
        ModuleRegistry.notifyInitializedInternal(context)

        assertNull(MSG_EXPECT_NOT_CONFIGURED, moduleB.configuredConfig)
        assertTrue(MSG_EXPECT_INITIALIZED, moduleB.initialized)
    }

    /** test_3: A failing module does not cancel configuration of other modules. */
    @Test
    fun configureModules_failureDoesNotCancelOthers() = runTest {
        val moduleA = FakeModule(MODULE_A, failConfigure = true)
        val moduleB = FakeModule(MODULE_B)
        register(moduleA)
        register(moduleB)

        val cfg = AltcraftConfiguration.Builder(
            apiUrl = "https://api.example.com"
        )
            .addModuleConfiguration(FakeConfiguration(MODULE_A))
            .addModuleConfiguration(FakeConfiguration(MODULE_B))
            .build()!!

        ModuleRegistry.configureModules(context, cfg)

        assertNull(MSG_EXPECT_NOT_CONFIGURED, moduleA.configuredConfig)
        assertNotNull(MSG_EXPECT_CONFIGURED, moduleB.configuredConfig)
        verify(exactly = 1) { Events.error(any(), any()) }
    }

    /** test_5: awaiting commands unblock when init swaps the signals (no deadlock). */
    @Test
    fun awaitRetryStarted_unblocks_whenSignalsSwapped() {
        val originalGate = ModuleRegistry.retryStartReady
        val snapshot = CompletableDeferred<Unit>()

        // Occupy the fallback slot: awaitRetryStarted() won't install its own
        // protective timer, exactly like notifyInitialized() cancels timers on init.
        ModuleRegistry.retryFallbacks[snapshot] = Job().apply { cancel() }

        val released = CountDownLatch(1)
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope.launch {
            ModuleRegistry.awaitRetryStarted(snapshot)
            released.countDown()
        }
        Thread.sleep(200)

        try {
            runBlocking {
                // Simulate init: swap the gate. The command may have captured
                // either the old or the new gate reference, so complete both.
                val current = ModuleRegistry.retryStartReady
                if (!current.isCompleted) current.complete(Unit)
                val next = CompletableDeferred<Unit>()
                ModuleRegistry.retryStartReady = next
                if (!next.isCompleted) next.complete(Unit)
                if (!snapshot.isCompleted) snapshot.complete(Unit)
            }

            assertTrue(
                "awaitRetryStarted() must not hang when init swaps the signals",
                released.await(2, TimeUnit.SECONDS)
            )
        } finally {
            scope.cancel()
            ModuleRegistry.retryFallbacks.remove(snapshot)
            ModuleRegistry.retryStartReady = originalGate
        }
    }
}
