package test.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.action.AltcraftPushActionActivity
import com.altcraft.sdk.push.action.PushAction
import com.altcraft.sdk.push.events.PushEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PushActionInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: getIntent returns a PendingIntent that starts AltcraftPushActionActivity and carries extras.
 *  - test_2: handleExtras with URL opens ACTION_VIEW and sends an "open" event once per unique UID.
 *  - test_3: handleExtras without URL launches the app via PackageManager launch intent (fallback).
 *
 * Negative scenarios:
 *  - test_4: openLink failure falls back to launchApp.
 *
 */
@RunWith(AndroidJUnit4::class)
class PushActionInstrumentedTest {

    // ---------- String constants ----------
    private companion object {
        const val DUMMY_URL = "https://example.com"
        const val DUMMY_URL_2 = "https://example.org"
        const val UID_1 = "uid-1"
        const val UID_2 = "uid-2"
        const val MSG_NO_INTENTS = "No intents started"
        const val MSG_MONITOR_TIMEOUT = "AltcraftPushActionActivity wasn't launched"
        const val MONITOR_TIMEOUT_MS = 3000L
    }

    /** Fake launch-able activity used by mocked PackageManager. */
    class TestLaunchActivity : Activity()

    /**
     * Context wrapper that:
     *  - captures all startActivity() calls in [startedIntents]
     *  - provides a mocked PackageManager returning a deterministic launch intent
     */
    private class CapturingContext(base: Context, private val launchComponent: ComponentName)
        : ContextWrapper(base) {

        val startedIntents = mutableListOf<Intent>()

        private val pm: PackageManager = mockk<PackageManager>(relaxed = true).apply {
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                component = launchComponent
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            every { getLaunchIntentForPackage(base.packageName) } returns launchIntent
        }

        override fun getPackageManager(): PackageManager = pm

        override fun startActivity(intent: Intent?) {
            if (intent != null) startedIntents += intent
        }
    }

    private lateinit var appCtx: Context
    private lateinit var capturingCtx: CapturingContext

    @Before
    fun setUp() {
        appCtx = ApplicationProvider.getApplicationContext()

        val launchComponent = ComponentName(appCtx, TestLaunchActivity::class.java)
        capturingCtx = CapturingContext(appCtx, launchComponent)

        mockkObject(PushEvent)
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } returns Unit

        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)
        every { Events.error(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * getIntent returns a PendingIntent that starts AltcraftPushActionActivity and carries extras
     */
    @Test
    fun getIntent_returnsPendingIntentWithTargetAndExtras() {
        val instr = InstrumentationRegistry.getInstrumentation()

        val monitor = instr.addMonitor(
            AltcraftPushActionActivity::class.java.name,
            /* result */ null,
            /* block */ false
        )

        val pi: PendingIntent = PushAction.getIntent(appCtx, DUMMY_URL, UID_1)
        assertNotNull(pi)

        pi.send()

        val started = instr.waitForMonitorWithTimeout(monitor, MONITOR_TIMEOUT_MS)
        assertNotNull(MSG_MONITOR_TIMEOUT, started)

        val activity = started!!
        val intent = activity.intent
        assertEquals(
            ComponentName(appCtx, AltcraftPushActionActivity::class.java),
            intent.component
        )
        assertEquals(DUMMY_URL, intent.extras?.getString(URL))
        assertEquals(UID_1, intent.extras?.getString(UID))

        activity.finish()
        instr.removeMonitor(monitor)
    }

    /**
     * handleExtras with URL opens ACTION_VIEW and sends an "open" event once per unique UID
     */
    @Test
    fun handleExtras_withUrl_opensLink_andSendsOpenEventOnce() {
        val extras: Bundle = bundleOf(URL to DUMMY_URL, UID to UID_1)

        PushAction.handleExtras(capturingCtx, extras)

        assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val first = capturingCtx.startedIntents.first()
        assertEquals(Intent.ACTION_VIEW, first.action)
        assertEquals(DUMMY_URL, first.data?.toString())

        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }

        PushAction.handleExtras(capturingCtx, bundleOf(URL to DUMMY_URL_2, UID to UID_1))
        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }

        PushAction.handleExtras(capturingCtx, bundleOf(URL to DUMMY_URL, UID to UID_2))
        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_2) }
    }

    /**
     * handleExtras without URL launches the app via PackageManager launch intent (fallback)
     */
    @Test
    fun handleExtras_withoutUrl_launchesApp() {
        val extras: Bundle = bundleOf(URL to "", UID to UID_1)

        PushAction.handleExtras(capturingCtx, extras)

        assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val launched = capturingCtx.startedIntents.last()

        assertEquals(
            ComponentName(capturingCtx, TestLaunchActivity::class.java),
            launched.component
        )

        coVerify { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }
    }

    /**
     * openLink failure falls back to launchApp
     */
    @Test
    fun openLink_failure_fallsBackToLaunch() {
        val extras: Bundle = bundleOf(URL to "bad-scheme://host/path", UID to UID_1)

        PushAction.handleExtras(capturingCtx, extras)

        capturingCtx.startedIntents.clear()
        PushAction.launchApp(capturingCtx)

        assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val launched = capturingCtx.startedIntents.last()
        assertEquals(
            ComponentName(capturingCtx, TestLaunchActivity::class.java),
            launched.component
        )
    }
}