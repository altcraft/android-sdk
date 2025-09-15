package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.additional.SubFunction.isJsonString
import org.junit.Assert.*
import org.junit.Test

/**
 * SubFunctionTest
 *
 * Positive scenarios:
 *  - test_2: fieldsIsObjects() returns true when the map contains at least one non-primitive
 *            value (e.g., Map/List/Array).
 *  - test_3: String?.isJsonString() correctly detects JSON object/array (leading whitespace allowed).
 *  - test_5: altcraftPush() returns true when the "_ac_push" key is present in the map.
 *
 * Negative scenarios:
 *  - test_1: fieldsIsObjects() returns false for null input and for maps with only primitives/nulls.
 *  - test_4: String?.isJsonString() returns false for null/blank/non-JSON strings.
 *            (Also guards simple non-JSON symbols like "(").
 *  - test_5: altcraftPush() returns false when "_ac_push" is absent or the map is empty.
 *
 * Notes:
 *  - Pure Kotlin tests (no Android dependencies).
 *  - Primitive set considered: String, Number, Boolean, and null (treated as non-object).
 */
class SubFunctionTest {

    /** fieldsIsObjects(): returns false for null or all-primitive maps. */
    @Test
    fun fieldsIsObjects_allPrimitives_false() {
        val map = mapOf<String, Any?>(
            "s" to "str",
            "i" to 1,
            "d" to 1.0,
            "b" to true,
            "n" to null
        )
        assertFalse(SubFunction.fieldsIsObjects(map))
        assertFalse(SubFunction.fieldsIsObjects(null))
    }

    /** fieldsIsObjects(): returns true when a non-primitive is present (Map/List/Array/etc). */
    @Test
    fun fieldsIsObjects_hasObject_true() {
        val map1 = mapOf("obj" to mapOf("k" to "v"))
        val map2 = mapOf("list" to listOf(1, 2, 3))
        val map3 = mapOf("arr" to arrayOf(mapOf("a" to 1)))
        assertTrue(SubFunction.fieldsIsObjects(map1))
        assertTrue(SubFunction.fieldsIsObjects(map2))
        assertTrue(SubFunction.fieldsIsObjects(map3))
    }

    /** String?.isJsonString(): detects JSON object/array with optional leading spaces. */
    @Test
    fun isJsonString_detectsJson() {
        assertTrue(" {\"a\":1}".isJsonString())
        assertTrue("\n\t[1,2,3]".isJsonString())
    }

    /** String?.isJsonString(): returns false for null/blank or non-JSON. */
    @Test
    fun isJsonString_nonJson_false() {
        assertFalse(null.isJsonString())
        assertFalse("   ".isJsonString())
        assertFalse("not-json".isJsonString())
        assertFalse("(".isJsonString())
    }

    /** altcraftPush(): true when _ac_push key exists, false otherwise. */
    @Test
    fun altcraftPush_keyCheck() {
        assertTrue(SubFunction.altcraftPush(mapOf("_ac_push" to "1", "x" to "y")))
        assertFalse(SubFunction.altcraftPush(mapOf("x" to "y")))
        assertFalse(SubFunction.altcraftPush(emptyMap()))
    }
}