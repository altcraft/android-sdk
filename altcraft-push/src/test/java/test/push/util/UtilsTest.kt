@file:Suppress("SpellCheckingInspection")

package test.push.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.util.Utils.UniqueCodeGenerator
import com.altcraft.sdk.push.util.Utils.altcraftPush
import com.altcraft.sdk.push.util.Utils.getIconColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UtilsTest
 *
 * Verifies pure utility helpers of the push module.
 */
class UtilsTest {

    private companion object {
        const val AC_PUSH = "_ac_push"
    }

    /** An Altcraft push contains the marker key. */
    @Test
    fun altcraftPush_detectsMarkerKey() {
        assertTrue(altcraftPush(mapOf(AC_PUSH to "1", "_uid" to "u")))
        assertFalse(altcraftPush(mapOf("_uid" to "u")))
        assertFalse(altcraftPush(emptyMap()))
    }

    /** Unique codes are non-negative and distinct across UIDs and calls. */
    @Test
    fun uniqueCode_generatesNonNegativeCodes() {
        val a = UniqueCodeGenerator.uniqueCode("uid-a")
        val b = UniqueCodeGenerator.uniqueCode("uid-a")
        val c = UniqueCodeGenerator.uniqueCode("uid-b")

        assertTrue(a >= 0)
        assertTrue(b >= 0)
        assertTrue(c >= 0)
        assertNotEquals(a, c)
    }

    /** Invalid or missing colors fall back to black without throwing. */
    @Test
    fun getIconColor_invalidColorFallsBackToBlack() {
        assertEquals(android.graphics.Color.BLACK, getIconColor("not-a-color"))
        assertEquals(android.graphics.Color.BLACK, getIconColor(null))
    }
}
