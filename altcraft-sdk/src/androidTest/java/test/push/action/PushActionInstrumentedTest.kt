package test.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.


import com.altcraft.sdk.push.action.AltcraftPushActionActivity
import com.altcraft.sdk.push.action.Intent
import com.altcraft.sdk.push.action.PushAction.handleExtras
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent as AndroidIntent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altcraft.sdk.data.Constants.MESSAGE_ID
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.push.action.PushAction.launchApp
import com.altcraft.sdk.push.events.PushEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.*
import org.junit.runner.RunWith

/**
 * PushActionInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: IntentUtil.getIntent returns a PendingIntent that starts AltcraftPushActionActivity and carries extras.
 *  - test_2: PushAction.handleExtras with URL opens ACTION_VIEW and sends an "open" event once per unique UID.
 *  - test_3: PushAction.handleExtras without URL launches the app via PackageManager launch intent (fallback).
 *
 * Negative scenarios:
 *  - test_4: PushAction.openLink failure falls back to launchApp.
 */
@RunWith(AndroidJUnit4::class)
class PushActionInstrumentedTest {

    private companion object {
        const val DUMMY_URL = "https://example.com"
        const val DUMMY_URL_2 = "https://example.org"
        const val UID_1 = "uid-1"
        const val UID_2 = "uid-2"
        const val MSG_NO_INTENTS = "No intents started"
        const val MSG_MONITOR_TIMEOUT = "AltcraftPushActionActivity wasn't launched"
        const val MONITOR_TIMEOUT_MS = 3000L
    }

    /** Fake launch-able activity for fallback app launch. */
    class TestLaunchActivity : Activity()

    /** Context wrapper that captures startActivity calls. */
    private class CapturingContext(base: Context, private val launchComponent: ComponentName)
        : ContextWrapper(base) {

        val startedIntents = mutableListOf<AndroidIntent>()

        private val pm: PackageManager = mockk<PackageManager>(relaxed = true).apply {
            val launchIntent = AndroidIntent(AndroidIntent.ACTION_MAIN).apply {
                component = launchComponent
                addCategory(AndroidIntent.CATEGORY_LAUNCHER)
                addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK)
            }
            every { getLaunchIntentForPackage(base.packageName) } returns launchIntent
        }

        override fun getPackageManager(): PackageManager = pm

        override fun startActivity(intent: AndroidIntent?) {
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

    // ---------- Intent.getIntent ----------

    /**
     * test_1: getIntent returns PendingIntent with correct target and extras
     */
    @Test
    fun test_1_getIntent_returnsPendingIntentWithTargetAndExtras() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val monitor = instr.addMonitor(
            AltcraftPushActionActivity::class.java.name,
            null,
            false
        )

        val pi: PendingIntent =
            Intent.getIntent(appCtx, 42, DUMMY_URL, UID_1)
        Assert.assertNotNull(pi)

        pi.send()

        val started = instr.waitForMonitorWithTimeout(monitor, MONITOR_TIMEOUT_MS)
        Assert.assertNotNull(MSG_MONITOR_TIMEOUT, started)

        val activity = started!!
        val intent = activity.intent
        Assert.assertEquals(
            ComponentName(appCtx, AltcraftPushActionActivity::class.java),
            intent.component
        )
        Assert.assertEquals(42, intent.extras?.getInt(MESSAGE_ID))
        Assert.assertEquals(DUMMY_URL, intent.extras?.getString(URL))
        Assert.assertEquals(UID_1, intent.extras?.getString(UID))

        activity.finish()
        instr.removeMonitor(monitor)
    }

    // ---------- PushAction.handleExtras ----------

    /**
     * test_2: handleExtras with URL opens link and sends event once per UID
     */
    @Test
    fun test_2_handleExtras_withUrl_opensLink_andSendsOpenEventOnce() {
        val extras: Bundle = bundleOf(URL to DUMMY_URL, UID to UID_1)

        handleExtras(capturingCtx, extras)

        Assert.assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val first = capturingCtx.startedIntents.first()
        Assert.assertEquals(AndroidIntent.ACTION_VIEW, first.action)
        Assert.assertEquals(DUMMY_URL, first.data?.toString())

        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }

        // same UID again → no duplicate event
        handleExtras(capturingCtx, bundleOf(URL to DUMMY_URL_2, UID to UID_1))
        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }

        // new UID → new event
        handleExtras(capturingCtx, bundleOf(URL to DUMMY_URL, UID to UID_2))
        coVerify(exactly = 1) { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_2) }
    }

    /**
     * test_3: handleExtras without URL launches app
     */
    @Test
    fun test_3_handleExtras_withoutUrl_launchesApp() {
        val extras: Bundle = bundleOf(URL to "", UID to UID_1)

        handleExtras(capturingCtx, extras)

        Assert.assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val launched = capturingCtx.startedIntents.last()
        Assert.assertEquals(
            ComponentName(capturingCtx, TestLaunchActivity::class.java),
            launched.component
        )

        coVerify { PushEvent.sendPushEvent(capturingCtx, OPEN, UID_1) }
    }

    /**
     * test_4: openLink failure falls back to launchApp
     */
    @Test
    fun test_4_openLink_failure_fallsBackToLaunch() {
        val extras: Bundle = bundleOf(URL to "bad-scheme://host/path", UID to UID_1)

        handleExtras(capturingCtx, extras)

        capturingCtx.startedIntents.clear()
        launchApp(capturingCtx)

        Assert.assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val launched = capturingCtx.startedIntents.last()
        Assert.assertEquals(
            ComponentName(capturingCtx, TestLaunchActivity::class.java),
            launched.component
        )
    }
}
