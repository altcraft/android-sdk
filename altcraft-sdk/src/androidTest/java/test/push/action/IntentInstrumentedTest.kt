package test.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
 *  - test_1: getIntent returns a PendingIntent.
 *  - test_2: PendingIntents for different UIDs are distinct (unique request codes).
 *
 * Notes:
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

    /** test_1: getIntent returns a PendingIntent */
    @Test
    fun test_1_getIntent_returnsPendingIntent() {
        val messageId = 101
        val url = "https://example.com/path"
        val uid = "uid-101"
        val extra = "extra-101"

        val pi: PendingIntent =
            com.altcraft.sdk.push.action.Intent.getIntent(ctx, messageId, url, uid, extra)
        assertNotNull(pi)

        assertEquals(ctx.packageName, pi.creatorPackage)
    }

    /** test_2: Different UIDs produce distinct PendingIntents (no clobbering) */
    @Test
    fun test_2_getIntent_different_UIDs_produceDistinctPendingIntents() {
        val url = "https://example.org"
        val extra = "x"

        val pi1 = com.altcraft.sdk.push.action.Intent.getIntent(ctx, 1, url, "uid-A", extra)
        val pi2 = com.altcraft.sdk.push.action.Intent.getIntent(ctx, 2, url, "uid-B", extra)

        assertNotNull(pi1)
        assertNotNull(pi2)

        assertNotEquals(pi1, pi2)
        assertNotEquals(pi1.hashCode(), pi2.hashCode())

        assertEquals(ctx.packageName, pi1.creatorPackage)
        assertEquals(ctx.packageName, pi2.creatorPackage)
    }
}