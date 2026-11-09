package test.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.data.Constants.AUTH_REQUEST_O_WORK_TAG
import com.altcraft.sdk.data.Preferences.clearCurrentRToken
import com.altcraft.sdk.data.Preferences.getCurrentRToken
import com.altcraft.sdk.data.Preferences.setCurrentRToken
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import com.altcraft.sdk.platform.authenticate.external.RTokenChange
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.workers.CancelWork
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * RTokenChangeInstrumentedTest
 *
 * Verifies that the RToken change notification chain always completes,
 * even when it cancels the worker that triggered it.
 *
 * Positive scenarios:
 * - test_1: rToken change detected inside a running one-time worker → the chain cancels
 *   the worker itself, but still completes: the new rToken is saved and the worker
 *   is left CANCELLED.
 * - test_2: module handler fails with an internal CancellationException → the remaining
 *   modules are still notified and the new rToken is saved.
 * - test_3: first rotation (no previously saved rToken) → the new rToken is saved
 *   without the change notification.
 */
@RunWith(AndroidJUnit4::class)
class RTokenChangeInstrumentedTest {

    private lateinit var context: Context
    private lateinit var wm: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        wm = WorkManager.getInstance(context)
        wm.cancelAllWork().result.get()

        mockkObject(DB.Companion)
        val sdkDBWrapper = mockk<DB>(relaxed = true)
        every { DB.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.authRequestDao() } returns mockk<AuthRequestDao>(relaxed = true)

        mockkObject(AuthenticateProcess)
        coEvery { AuthenticateProcess.logOutUnlocked(any()) } returns Unit

        setCurrentRToken(context, RTOKEN_OLD)
    }

    @After
    fun tearDown() {
        runCatching { wm.cancelAllWork().result.get() }
        ModuleRegistry.unregister(MODULE_ID_BAD)
        ModuleRegistry.unregister(MODULE_ID_GOOD)
        unmockkAll()
    }

    /** - test_1: change detected in a running worker → worker cancelled, new rToken saved. */
    @Test
    fun rTokenChange_detectedInWorker_savesNewToken_andCancelsWorker() {
        val request = OneTimeWorkRequest.Builder(SelfCancelWorker::class.java)
            .addTag(AUTH_REQUEST_O_WORK_TAG)
            .build()
        wm.enqueue(request).result.get()

        awaitWorkerCancelledAndTokenSaved(AUTH_REQUEST_O_WORK_TAG)

        assertEquals(
            MSG_TOKEN_SAVED_BY_CANCELLED_WORKER,
            RTOKEN_NEW,
            getCurrentRToken(context)
        )

        val infos = wm.getWorkInfosByTag(AUTH_REQUEST_O_WORK_TAG).get()
        assertTrue(
            MSG_WORKER_CANCELLED,
            infos.all { it.state == WorkInfo.State.CANCELLED }
        )
    }

    /** - test_2: module internal cancellation → remaining modules notified, new rToken saved. */
    @Test
    fun rTokenChange_moduleInternalCancellation_chainStillCompletes() = runBlocking {
        mockkObject(CancelWork)
        coEvery { CancelWork.cancelWorkerTask(any(), any<List<String>>()) } returns Unit

        val bad = FailingModule(MODULE_ID_BAD)
        val good = NotifyingModule(MODULE_ID_GOOD)
        ModuleRegistry.register(bad)
        ModuleRegistry.register(good)

        RTokenChange.checkRTokenChangedFromJWT(context, JWT_NEW)

        assertTrue(MSG_BAD_MODULE_INVOKED, bad.invoked)
        assertTrue(MSG_GOOD_MODULE_NOTIFIED, good.notified)
        assertEquals(MSG_TOKEN_SAVED_DESPITE_MODULE_FAILURE, RTOKEN_NEW, getCurrentRToken(context))
        coVerify(exactly = 1) { AuthenticateProcess.logOutUnlocked(context) }
    }

    /** - test_3: first rotation (no saved rToken) → token saved, no notification. */
    @Test
    fun rTokenChange_firstRotation_savesTokenWithoutNotification() = runBlocking {
        clearCurrentRToken(context)

        val probe = NotifyingModule(MODULE_ID_GOOD)
        ModuleRegistry.register(probe)

        RTokenChange.checkRTokenChangedFromJWT(context, JWT_NEW)

        assertFalse(MSG_FIRST_ROTATION_NO_NOTIFICATION, probe.notified)
        assertEquals(MSG_TOKEN_SAVED_ON_FIRST_ROTATION, RTOKEN_NEW, getCurrentRToken(context))
    }

    /**
     * Waits until every worker with the tag reaches the CANCELLED state
     * and the chain has saved the new rToken.
     *
     * WorkManager marks the work CANCELLED while the notification chain is
     * still finishing, so both conditions are awaited together.
     */
    private fun awaitWorkerCancelledAndTokenSaved(
        tag: String,
        timeoutMs: Long = AWAIT_TIMEOUT_MS
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var cancelled = false

        while (System.currentTimeMillis() < deadline) {
            val infos = wm.getWorkInfosByTag(tag).get()
            cancelled = infos.isNotEmpty() &&
                    infos.all { it.state == WorkInfo.State.CANCELLED }

            if (cancelled && RTOKEN_NEW == getCurrentRToken(context)) return

            Thread.sleep(POLL_INTERVAL_MS)
        }

        fail(MSG_WORKER_CANCELLED_TIMEOUT)
    }
}

private const val RTOKEN_OLD = "rt-OLD"
private const val RTOKEN_NEW = "rt-NEW"

/** JWT payload whose "rtoken" claim carries the new role token. */
private val JWT_NEW =
    "x." + Base64.encodeToString(
        """{"rtoken":"$RTOKEN_NEW"}""".toByteArray(),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    ) + ".x"
private const val MODULE_ID_BAD = "test-module-failing"
private const val MODULE_ID_GOOD = "test-module-notifying"
private const val AWAIT_TIMEOUT_MS = 15_000L
private const val POLL_INTERVAL_MS = 50L

private const val MSG_TOKEN_SAVED_BY_CANCELLED_WORKER =
    "The new rToken must be saved even when the chain cancels the triggering worker"
private const val MSG_WORKER_CANCELLED =
    "The triggering worker must end up in the CANCELLED state"
private const val MSG_WORKER_CANCELLED_TIMEOUT =
    "The triggering worker was not cancelled in time"
private const val MSG_BAD_MODULE_INVOKED =
    "The failing module handler must be invoked"
private const val MSG_GOOD_MODULE_NOTIFIED =
    "The remaining module must be notified despite the failing handler"
private const val MSG_TOKEN_SAVED_DESPITE_MODULE_FAILURE =
    "The new rToken must be saved despite a module handler failure"
private const val MSG_FIRST_ROTATION_NO_NOTIFICATION =
    "The first saved rToken must not trigger the change notification"
private const val MSG_TOKEN_SAVED_ON_FIRST_ROTATION =
    "The rToken must be saved after the first rotation"

/**
 * One-time worker that detects an RToken change from its own body,
 * reproducing the self-cancel scenario: the notification chain cancels
 * this worker by its tag while it is still running.
 */
class SelfCancelWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        RTokenChange.checkRTokenChangedFromJWT(
            applicationContext, JWT_NEW
        )
        awaitCancellation()
    }
}

/** Module whose rToken handler fails with an internal cancellation. */
private class FailingModule(
    override val moduleId: String
) : AltcraftModule {

    var invoked = false

    override suspend fun onSdkInitialized(context: Context) = Unit

    override suspend fun onSdkClear(context: Context) = Unit

    override suspend fun onSdkRTokenChanged(context: Context) {
        invoked = true
        throw CancellationException("internal module cancellation")
    }
}

/** Module that records the rToken change notification. */
private class NotifyingModule(
    override val moduleId: String
) : AltcraftModule {

    var notified = false

    override suspend fun onSdkInitialized(context: Context) = Unit

    override suspend fun onSdkClear(context: Context) = Unit

    override suspend fun onSdkRTokenChanged(context: Context) {
        notified = true
    }
}
