package test.push.action

//  Created by Andrey Pogodin.
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altcraft.sdk.data.Constants.MESSAGE_ID
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.push.action.AltcraftPushActionActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IntentInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: getIntent returns a PendingIntent that starts AltcraftPushActionActivity and carries extras.
 *  - test_2: PendingIntents for different UIDs are distinct (unique request codes).
 *
 * Notes:
 *  - Uses a single launch per test to avoid SINGLE_TOP/onNewIntent interference.
 *  - Uses targetContext to ensure proper package/component resolution.
 */
@RunWith(AndroidJUnit4::class)
class IntentInstrumentedTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun tearDown() {
    }

    /** test_1: PendingIntent launches the target activity and carries extras */
    @Test
    fun test_1_getIntent_launchesTarget_withExtras() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val monitor = instr.addMonitor(
            AltcraftPushActionActivity::class.java.name,
            null,
            false
        )

        val messageId = 101
        val url = "https://example.com/path"
        val uid = "uid-101"

        val pi: PendingIntent =
            com.altcraft.sdk.push.action.Intent.getIntent(ctx, messageId, url, uid)
        assertNotNull(pi)

        pi.send()

        val started = instr.waitForMonitorWithTimeout(monitor, /* ms */ 5000L)
        assertNotNull("AltcraftPushActionActivity wasn't launched", started)

        val activity = started!!
        val intent = activity.intent

        assertEquals(
            ComponentName(ctx, AltcraftPushActionActivity::class.java),
            intent.component
        )

        assertEquals(messageId, intent.extras?.getInt(MESSAGE_ID))
        assertEquals(url, intent.extras?.getString(URL))
        assertEquals(uid, intent.extras?.getString(UID))

        instr.runOnMainSync { activity.finish() }
        instr.waitForIdleSync()
        instr.removeMonitor(monitor)
    }

    /** test_2: Different UIDs produce distinct PendingIntents (no clobbering) */
    @Test
    fun test_2_getIntent_different_UIDs_produceDistinctPendingIntents() {
        val url = "https://example.org"
        val pi1 = com.altcraft.sdk.push.action.Intent.getIntent(ctx, 1, url, "uid-A")
        val pi2 = com.altcraft.sdk.push.action.Intent.getIntent(ctx, 2, url, "uid-B")

        assertNotEquals(pi1, pi2)
        assertNotEquals(pi1.hashCode(), pi2.hashCode())
    }
}

