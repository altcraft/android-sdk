@file:Suppress("SpellCheckingInspection")

package test.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

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
import com.altcraft.sdk.data.Constants.EXTRA
import com.altcraft.sdk.data.Constants.MSG_ID
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.push.action.AltcraftPushActionActivity
import com.altcraft.sdk.push.action.Intent
import com.altcraft.sdk.push.action.PushAction
import com.altcraft.sdk.push.action.PushAction.handleExtras
import com.altcraft.sdk.push.action.PushAction.launchApp
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.sdk_events.Events
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PushActionInstrumentedTest
 *
 * Positive scenarios:
 * - test_1: [Intent.getIntent] returns a [PendingIntent] that starts [AltcraftPushActionActivity]
 *   and carries extras, including [EXTRA] when it is not empty.
 * - test_1b: [Intent.getIntent] does not put the [EXTRA] key into the extras bundle when the
 *   provided `extra` is empty.
 * - test_2: [PushAction.handleExtras] with [URL] opens an [AndroidIntent.ACTION_VIEW] intent and
 *   sends an [OPEN] event once per unique [UID].
 * - test_3: [PushAction.handleExtras] without [URL] launches the app
 *   via [PackageManager.getLaunchIntentForPackage] and passes [EXTRA] + flags.
 *
 * Negative scenarios:
 * - test_4: Link opening failure falls back to [launchApp] (validated via forced exception).
 */
@RunWith(AndroidJUnit4::class)
class PushActionInstrumentedTest {

    private companion object {
        const val DUMMY_URL = "https://example.com"
        const val DUMMY_URL_2 = "https://example.org"

        const val UID_URL_1 = "uid-url-1"
        const val UID_URL_2 = "uid-url-2"
        const val UID_NO_URL = "uid-no-url-1"
        const val UID_BAD_SCHEME = "uid-bad-scheme-1"

        const val EXTRA_VALUE = "extra-value"
        const val EXTRA_EMPTY = ""

        const val MSG_NO_INTENTS = "No intents started"
        const val MSG_MONITOR_TIMEOUT = "AltcraftPushActionActivity wasn't launched"
        const val MONITOR_TIMEOUT_MS = 3000L
    }

    class TestLaunchActivity : Activity()

    private open class CapturingContext(
        base: Context,
        private val launchComponent: ComponentName
    ) : ContextWrapper(base) {

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

    /**
     * Context that forces a failure when trying to open ACTION_VIEW with "bad-scheme://..."
     * so we can deterministically test PushAction fallback to launchApp().
     */
    private class FailingViewContext(
        base: Context,
        launchComponent: ComponentName
    ) : CapturingContext(base, launchComponent) {

        override fun startActivity(intent: AndroidIntent?) {
            if (intent?.action == AndroidIntent.ACTION_VIEW && intent.data?.scheme == "bad-scheme") {
                throw IllegalStateException("Forced failure for ACTION_VIEW bad-scheme")
            }
            super.startActivity(intent)
        }
    }

    private lateinit var appCtx: Context
    private lateinit var capturingCtx: CapturingContext
    private lateinit var failingCtx: FailingViewContext

    @Before
    fun setUp() {
        appCtx = ApplicationProvider.getApplicationContext()
        val launchComponent = ComponentName(appCtx, TestLaunchActivity::class.java)
        capturingCtx = CapturingContext(appCtx, launchComponent)
        failingCtx = FailingViewContext(appCtx, launchComponent)

        resetPushActionUid()

        mockkObject(PushEvent)
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } returns Unit

        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)
        every { Events.error(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        resetPushActionUid()
        unmockkAll()
    }

    private fun resetPushActionUid() {
        val f = PushAction::class.java.getDeclaredField("uid")
        f.isAccessible = true
        f.set(null, null)
    }

    private fun assertLaunchFlags(intent: AndroidIntent) {
        val flags = intent.flags
        assertTrue(
            "FLAG_ACTIVITY_NEW_TASK must be set",
            flags and AndroidIntent.FLAG_ACTIVITY_NEW_TASK != 0
        )
        assertTrue(
            "FLAG_ACTIVITY_CLEAR_TOP must be set",
            flags and AndroidIntent.FLAG_ACTIVITY_CLEAR_TOP != 0
        )
        assertTrue(
            "FLAG_ACTIVITY_SINGLE_TOP must be set",
            flags and AndroidIntent.FLAG_ACTIVITY_SINGLE_TOP != 0
        )
    }

    /**
     * test_1: [Intent.getIntent] returns a [PendingIntent] that starts [AltcraftPushActionActivity]
     * and carries the expected extras, including [EXTRA] when it is not empty.
     */
    @Test
    fun test_1_getIntent_returnsPendingIntentWithTargetAndExtras_includingExtra() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val monitor = instr.addMonitor(
            AltcraftPushActionActivity::class.java.name,
            null,
            false
        )

        val pi: PendingIntent =
            Intent.getIntent(appCtx, 42, DUMMY_URL, UID_URL_1, EXTRA_VALUE)

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
        Assert.assertEquals(42, intent.extras?.getInt(MSG_ID))
        Assert.assertEquals(DUMMY_URL, intent.extras?.getString(URL))
        Assert.assertEquals(UID_URL_1, intent.extras?.getString(UID))
        Assert.assertEquals(EXTRA_VALUE, intent.extras?.getString(EXTRA))

        activity.finish()
        instr.removeMonitor(monitor)
    }

    /**
     * test_1b: [Intent.getIntent] does not put the [EXTRA] key into the extras bundle when the
     * provided `extra` is empty, because the SDK adds [EXTRA] only if `extra.isNotEmpty()`.
     */
    @Test
    fun test_1b_getIntent_doesNotPutExtra_whenExtraEmpty() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val monitor = instr.addMonitor(
            AltcraftPushActionActivity::class.java.name,
            null,
            false
        )

        val pi: PendingIntent =
            Intent.getIntent(appCtx, 43, DUMMY_URL, UID_URL_2, EXTRA_EMPTY)

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
        Assert.assertEquals(43, intent.extras?.getInt(MSG_ID))
        Assert.assertEquals(DUMMY_URL, intent.extras?.getString(URL))
        Assert.assertEquals(UID_URL_2, intent.extras?.getString(UID))

        val extras = intent.extras
        Assert.assertNotNull(extras)
        Assert.assertFalse(
            "EXTRA key must be absent when extra is empty",
            extras!!.containsKey(EXTRA)
        )

        activity.finish()
        instr.removeMonitor(monitor)
    }

    /**
     * test_2: [handleExtras] with a non-empty [URL] opens an [AndroidIntent.ACTION_VIEW] intent and
     * sends an [OPEN] event once per unique [UID]. Also validates that [EXTRA] is passed into the
     * started intent.
     */
    @Test
    fun test_2_handleExtras_withUrl_opensLink_andSendsOpenEventOnce_andPassesExtra() {
        val extras: Bundle = bundleOf(
            URL to DUMMY_URL,
            UID to UID_URL_1,
            EXTRA to EXTRA_VALUE
        )

        handleExtras(capturingCtx, extras)

        assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val first = capturingCtx.startedIntents.first()

        Assert.assertEquals(AndroidIntent.ACTION_VIEW, first.action)
        Assert.assertEquals(DUMMY_URL, first.data?.toString())
        Assert.assertEquals(EXTRA_VALUE, first.getStringExtra(EXTRA))

        coVerify(exactly = 1, timeout = 3000L) {
            PushEvent.sendPushEvent(capturingCtx, OPEN, UID_URL_1)
        }

        handleExtras(
            capturingCtx,
            bundleOf(URL to DUMMY_URL_2, UID to UID_URL_1, EXTRA to EXTRA_VALUE)
        )
        coVerify(exactly = 1, timeout = 3000L) {
            PushEvent.sendPushEvent(capturingCtx, OPEN, UID_URL_1)
        }

        handleExtras(
            capturingCtx,
            bundleOf(URL to DUMMY_URL, UID to UID_URL_2, EXTRA to EXTRA_VALUE)
        )
        coVerify(exactly = 1, timeout = 3000L) {
            PushEvent.sendPushEvent(capturingCtx, OPEN, UID_URL_2)
        }
    }

    /**
     * test_3: [handleExtras] without [URL] launches the app via
     * [PackageManager.getLaunchIntentForPackage] and passes [EXTRA] + flags.
     */
    @Test
    fun test_3_handleExtras_withoutUrl_launchesApp_andPassesExtra_andFlags() {
        val extras: Bundle = bundleOf(
            URL to "",
            UID to UID_NO_URL,
            EXTRA to EXTRA_VALUE
        )

        handleExtras(capturingCtx, extras)

        assertTrue(MSG_NO_INTENTS, capturingCtx.startedIntents.isNotEmpty())
        val launched = capturingCtx.startedIntents.last()

        Assert.assertEquals(
            ComponentName(capturingCtx, TestLaunchActivity::class.java),
            launched.component
        )
        Assert.assertEquals(EXTRA_VALUE, launched.getStringExtra(EXTRA))
        assertLaunchFlags(launched)

        coVerify(timeout = 3000L) {
            PushEvent.sendPushEvent(capturingCtx, OPEN, UID_NO_URL)
        }
    }

    /**
     * test_4: If opening the link fails, [handleExtras] falls back to [launchApp].
     * We force a failure for ACTION_VIEW "bad-scheme://..." using [FailingViewContext].
     */
    @Test
    fun test_4_openLink_failure_fallsBackToLaunch_andLaunchAppAddsFlags() {
        val extras: Bundle = bundleOf(
            URL to "bad-scheme://host/path",
            UID to UID_BAD_SCHEME,
            EXTRA to EXTRA_VALUE
        )

        handleExtras(failingCtx, extras)

        assertTrue(MSG_NO_INTENTS, failingCtx.startedIntents.isNotEmpty())
        val launchedAfterFallback = failingCtx.startedIntents.last()

        Assert.assertEquals(
            "Fallback must launch app main activity intent",
            ComponentName(failingCtx, TestLaunchActivity::class.java),
            launchedAfterFallback.component
        )

        Assert.assertEquals(EXTRA_VALUE, launchedAfterFallback.getStringExtra(EXTRA))
        assertLaunchFlags(launchedAfterFallback)

        failingCtx.startedIntents.clear()
        launchApp(failingCtx, null)

        assertTrue(MSG_NO_INTENTS, failingCtx.startedIntents.isNotEmpty())
        val launchedDirect = failingCtx.startedIntents.last()
        Assert.assertEquals(
            ComponentName(failingCtx, TestLaunchActivity::class.java),
            launchedDirect.component
        )
        assertLaunchFlags(launchedDirect)
    }
}
