package test.inapp.presentation

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Activity
import android.app.Dialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altcraft.sdk.inapp.system.dialog.InAppDialog
import com.altcraft.sdk.inapp.system.lifecycle.ActivityTracker
import java.lang.ref.WeakReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * InAppPresentationGuardInstrumentedTest
 *
 * Verifies that In-App presentation guards prevent
 * WindowManager.BadTokenException when the host activity dies before or
 * during presentation.
 *
 * Positive scenarios:
 * - test_1: showFullscreen presents the dialog on a live resumed activity.
 *
 * Negative scenarios:
 * - test_2: showFullscreen skips a destroyed activity without throwing.
 * - test_3: runOnNextResumed defers the block for a finishing activity
 *   instead of executing it synchronously.
 */
@RunWith(AndroidJUnit4::class)
class InAppPresentationGuardInstrumentedTest {

    // ── showFullscreen ───────────────────────────────────────────────────

    /** test_1: showFullscreen shows the dialog on a live activity. */
    @Test
    fun showFullscreen_showsOnLiveActivity() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)

        scenario.onActivity { activity ->
            val dialog = createDialog(activity)

            InAppDialog.showFullscreen(dialog)

            assertTrue("Expected dialog to be showing", dialog.isShowing)

            dialog.dismiss()
        }

        scenario.close()
    }

    /** test_2: showFullscreen skips a destroyed activity without throwing. */
    @Test
    fun showFullscreen_skipsDestroyedActivity() {
        var activityRef: Activity? = null

        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activityRef = it }
        scenario.close()

        val activity = checkNotNull(activityRef)
        assertTrue(activity.isDestroyed)

        val instrumentation = InstrumentationRegistry.getInstrumentation()

        instrumentation.runOnMainSync {
            val dialog = createDialog(activity)

            InAppDialog.showFullscreen(dialog)

            assertFalse("Expected dialog not to be shown", dialog.isShowing)
        }
    }

    // ── runOnNextResumed ─────────────────────────────────────────────────

    /**
     * test_3: runOnNextResumed defers the block for a finishing activity
     * instead of executing it synchronously.
     */
    @Test
    fun runOnNextResumed_defersBlockForFinishingActivity() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        var deferred = false

        scenario.onActivity { activity ->
            activity.finish()

            val resumedRefField = ActivityTracker::class.java
                .getDeclaredField("currentResumedRef")
            resumedRefField.isAccessible = true
            resumedRefField.set(ActivityTracker, WeakReference(activity))

            var executed = false
            ActivityTracker.runOnNextResumed { executed = true }

            deferred = !executed

            resumedRefField.set(ActivityTracker, null)

            ActivityTracker::class.java
                .getDeclaredField("runOnceOnResumed")
                .apply {
                    isAccessible = true
                    set(ActivityTracker, null)
                }
        }

        assertTrue(
            "Expected block to be deferred for a finishing activity",
            deferred
        )

        scenario.close()
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun createDialog(activity: Activity): Dialog =
        InAppDialog.createDialog(activity = activity, fullScreen = false)
}
