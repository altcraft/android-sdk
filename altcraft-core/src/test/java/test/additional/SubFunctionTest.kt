@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.util.SubFunction.isJsonString
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SubFunctionTest
 *
 * Positive scenarios:
 *  - test_1: fieldsIsObjects() returns false for null or all-primitive maps (including empty).
 *  - test_2: fieldsIsObjects() returns true when a non-primitive value is present.
 *  - test_3: String?.isJsonString() detects JSON object/array (whitespace allowed).
 *  - test_4: String?.isJsonString() returns false for null/blank/non-JSON.
 */
class SubFunctionTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.v(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        // Intentionally left blank
    }

    /** - test_1: fieldsIsObjects() returns false for null or all-primitive maps (including empty). */
    @Test
    fun fieldsIsObjects_allPrimitives_false() {
        val map = mapOf<String, Any?>("s" to "str", "i" to 1, "d" to 1.0, "b" to true, "n" to null)
        assertFalse(SubFunction.fieldsIsObjects(map))
        assertFalse(SubFunction.fieldsIsObjects(null))
        assertFalse(SubFunction.fieldsIsObjects(emptyMap()))
    }

    /** - test_2: fieldsIsObjects() returns true when a non-primitive value is present. */
    @Test
    fun fieldsIsObjects_hasObject_true() {
        val map1 = mapOf("obj" to mapOf("k" to "v"))
        val map2 = mapOf("list" to listOf(1, 2, 3))
        val map3 = mapOf("arr" to arrayOf(mapOf("a" to 1)))
        val map4 = mapOf("mixed" to listOf(mapOf("x" to 1), 2, true))
        assertTrue(SubFunction.fieldsIsObjects(map1))
        assertTrue(SubFunction.fieldsIsObjects(map2))
        assertTrue(SubFunction.fieldsIsObjects(map3))
        assertTrue(SubFunction.fieldsIsObjects(map4))
    }

    /** - test_3: String?.isJsonString() detects JSON object/array (whitespace allowed). */
    @Test
    fun isJsonString_detectsJson() {
        assertTrue(" {\"a\":1}".isJsonString())
        assertTrue("\n\t[1,2,3]".isJsonString())
        assertTrue("[ ]".isJsonString())
        assertTrue("{ }".isJsonString())
    }

    /** - test_4: String?.isJsonString() returns false for null/blank/non-JSON. */
    @Test
    fun isJsonString_nonJson_false() {
        assertFalse(null.isJsonString())
        assertFalse("   ".isJsonString())
        assertFalse("not-json".isJsonString())
        assertFalse("(".isJsonString())
        assertFalse("x{y}".isJsonString())
    }
}