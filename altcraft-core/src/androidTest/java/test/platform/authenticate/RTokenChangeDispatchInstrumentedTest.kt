package test.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.data.Preferences.getCurrentRToken
import com.altcraft.sdk.data.Preferences.setCurrentRToken
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.platform.authenticate.external.RTokenChange
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTInterface
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProvider
import com.altcraft.sdk.util.SubFunction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.room.TestRoom
import java.util.concurrent.Executors

/**
 * RTokenChangeDispatchInstrumentedTest
 *
 * Verifies the initialization dispatch ordering guarantees for role-token
 * changes (`ModuleRegistry.notifyInitializedInternal`).
 *
 * Positive scenarios:
 * - test_1: a change missed at config time (JWT not yet available) is detected
 *   by the dispatch-time re-check and settles before the module initialization
 *   phase runs.
 * - test_2: a change detected while the module initialization phase is already
 *   running (JWT became available mid-dispatch) settles with the dispatch and
 *   the phase is re-run exactly once.
 */
@RunWith(AndroidJUnit4::class)
class RTokenChangeDispatchInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var wm: WorkManager
    private lateinit var db: TestRoom

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)

        wm = WorkManager.getInstance(appContext)
        wm.cancelAllWork().result.get()

        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()

        mockkObject(DB.Companion)
        val sdkDBWrapper = mockk<DB>(relaxed = true)
        every { DB.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.configurationDao() } returns db.configurationDao()
        every { sdkDBWrapper.authStateDao() } returns mockk(relaxed = true)
        every { sdkDBWrapper.authRequestDao() } returns mockk(relaxed = true)

        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns false

        ConfigManager.conf = jwtModeConfig()
        setCurrentRToken(appContext, RTOKEN_OLD)
    }

    @After
    fun tearDown() {
        ModuleRegistry.unregister(MODULE_ID_PROBE)
        JWTProvider.provider = null
        ConfigManager.conf = null
        runCatching { wm.cancelAllWork().result.get() }
        unmockkAll()
        db.close()
    }

    /** JWT-mode configuration: no resource token, the JWT is the auth source. */
    private fun jwtModeConfig() = ConfigurationEntity(
        id = 0,
        apiUrl = API_URL,
        rToken = null,
        appInfo = null
    )

    /**
     * Passes the registered modules through the configuration phase so they
     * join the ready set and receive the `onSdkInitialized` callback
     * (only configured modules are initialized by the dispatch).
     */
    private suspend fun makeRegisteredModulesReady() {
        val config = mockk<com.altcraft.sdk.config.AltcraftConfiguration>()
        every { config.getModuleConfiguration(any()) } returns null

        ModuleRegistry.configureModules(appContext, config)
    }

    /**
     * - test_1: the dispatch-time re-check detects the change missed at
     * config time and settles it before the module initialization phase.
     */
    @Test
    fun dispatchReCheck_settlesChangeBeforeModuleInit() = runBlocking {
        val provider = FirstReadNullJWTProvider()
        JWTProvider.provider = provider

        // Config-time check (Init.init): the provider is not ready yet —
        // the change is missed there.
        RTokenChange.checkRTokenChangedFromInit(appContext)

        // The JWT becomes available before the initialization dispatch.
        provider.arm(JWT_NEW)

        val probe = DispatchProbeModule(MODULE_ID_PROBE)
        ModuleRegistry.register(probe)
        makeRegisteredModulesReady()

        ModuleRegistry.notifyInitializedInternal(appContext)

        assertEquals(
            MSG_CHANGE_SETTLES_BEFORE_MODULE_INIT,
            listOf("rtoken", "init"),
            probe.events
        )
        assertEquals(
            MSG_TOKEN_SAVED,
            RTOKEN_NEW,
            getCurrentRToken(appContext)
        )
        assertEquals(
            MSG_SINGLE_MODULE_INIT,
            1,
            probe.initCount
        )
    }

    /**
     * - test_2: the change detected while the module initialization phase is
     * running settles with the dispatch and the phase is re-run exactly once.
     */
    @Test
    fun changeDuringDispatch_rerunsModuleInit() = runBlocking {
        // No JWT provider registered: the config-time check sees no JWT —
        // the change is missed before the dispatch.
        RTokenChange.checkRTokenChangedFromInit(appContext)

        // The probe triggers the change from inside its own initialization,
        // reproducing a JWT read performed mid-dispatch.
        val probe = DispatchProbeModule(MODULE_ID_PROBE) {
            RTokenChange.checkRTokenChangedFromJWT(appContext, JWT_NEW)
        }
        ModuleRegistry.register(probe)
        makeRegisteredModulesReady()

        ModuleRegistry.notifyInitializedInternal(appContext)

        assertEquals(
            MSG_PHASE_RERUN_AFTER_CHANGE,
            listOf("init", "rtoken", "init"),
            probe.events
        )
        assertEquals(
            MSG_PHASE_RERUN_ONCE,
            2,
            probe.initCount
        )
        assertEquals(
            MSG_TOKEN_SAVED,
            RTOKEN_NEW,
            getCurrentRToken(appContext)
        )
    }
}

private const val API_URL = "https://api.example.com"
private const val RTOKEN_OLD = "rt-OLD"
private const val RTOKEN_NEW = "rt-NEW"
private const val MODULE_ID_PROBE = "test-dispatch-probe-module"

/** JWT payload whose "rtoken" claim carries the new role token. */
private val JWT_NEW =
    "x." + Base64.encodeToString(
        """{"rtoken":"$RTOKEN_NEW"}""".toByteArray(),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    ) + ".x"

private const val MSG_CHANGE_SETTLES_BEFORE_MODULE_INIT =
    "The change must settle before the module initialization phase runs"
private const val MSG_PHASE_RERUN_AFTER_CHANGE =
    "The module initialization phase must re-run once after the mid-dispatch change"
private const val MSG_PHASE_RERUN_ONCE =
    "The module initialization phase must run exactly twice (initial + re-run)"
private const val MSG_SINGLE_MODULE_INIT =
    "The module initialization phase must run exactly once"
private const val MSG_TOKEN_SAVED =
    "The new rToken must be saved by the settled notification"

/**
 * JWT provider mirroring an app whose JWT is not yet available: reads before
 * `arm` return `null`, reads after it return the issued JWT.
 */
private class FirstReadNullJWTProvider : JWTInterface {

    @Volatile
    private var jwt: String? = null

    /** Issues the JWT from the next read on. */
    fun arm(token: String) {
        jwt = token
    }

    override fun getJWT(): String? = jwt
}

/**
 * Module recording the lifecycle event order of the initialization dispatch
 * and the role-token change notification.
 *
 * @param onInit Optional action performed inside `onSdkInitialized`
 * (used to trigger a mid-dispatch change).
 */
private class DispatchProbeModule(
    override val moduleId: String,
    private val onInit: (suspend () -> Unit)? = null
) : AltcraftModule {

    private val lock = Any()
    private val eventsList = mutableListOf<String>()
    private var initCounter = 0
    private var rtokenChangedFlag = false

    /** Recorded lifecycle events in dispatch order. */
    val events: List<String>
        get() = synchronized(lock) { eventsList.toList() }

    /** Number of `onSdkInitialized` invocations. */
    val initCount: Int
        get() = synchronized(lock) { initCounter }

    /** Whether `onSdkRTokenChanged` has been dispatched. */
    val rtokenChanged: Boolean
        get() = synchronized(lock) { rtokenChangedFlag }

    override suspend fun onSdkInitialized(context: Context) {
        synchronized(lock) {
            initCounter++
            eventsList.add("init")
        }
        onInit?.invoke()
    }

    override suspend fun onSdkClear(context: Context) = Unit

    override suspend fun onSdkRTokenChanged(context: Context) {
        synchronized(lock) {
            rtokenChangedFlag = true
            eventsList.add("rtoken")
        }
    }
}
