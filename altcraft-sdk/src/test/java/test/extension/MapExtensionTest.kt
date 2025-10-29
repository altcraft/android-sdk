package test.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.extension.MapExtension.mapToJson
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/**
 * MapExtensionTest
 *
 * Positive scenarios:
 *  - test_1: Primitives and nulls are encoded as JSON primitives/null.
 *  - test_2: Deep nested maps are encoded correctly.
 *  - test_3: Collections (List/Set) and reference arrays are encoded as JSON arrays.
 *  - test_4: Primitive arrays (all types) are encoded as JSON arrays of primitives.
 *  - test_5: Enum and Char are encoded as strings (name/char).
 *  - test_6: Sequence is truncated properly and encoded as JSON array.
 *  - test_7: Non-string map keys are filtered out from JSON object.
 *  - test_8: Fallback object uses safe toString() (exception → "null").
 *
 * Negative/edge scenarios:
 *  - test_9: Cycles detection returns JsonNull on cyclic reference.
 *  - test_10: Max depth guard returns JsonNull when depth limit reached.
 *  - test_11: maxCollectionElements limits array/object size.
 */
class MapExtensionTest {

    /** test_1: Primitives and nulls */
    @Test
    fun mapToJson_primitivesAndNulls() {
        val src = mapOf(
            "b" to true,
            "i" to 42,
            "d" to 3.14,
            "s" to "hi",
            "c" to 'Z',
            "n" to null
        )
        val json = src.mapToJson().jsonObject
        assertEquals(true,  json["b"]!!.jsonPrimitive.boolean)
        assertEquals(42,    json["i"]!!.jsonPrimitive.int)
        assertEquals(3.14,  json["d"]!!.jsonPrimitive.double, 1e-9)
        assertEquals("hi",  json["s"]!!.jsonPrimitive.content)
        assertEquals("Z",   json["c"]!!.jsonPrimitive.content)
        assertTrue(json["n"] is JsonNull)
    }

    /** test_2: Deep nested maps */
    @Test
    fun mapToJson_nestedMaps() {
        val src = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "value"
                )
            )
        )
        val json = src.mapToJson().jsonObject
        val cVal = json["a"]!!
            .jsonObject["b"]!!
            .jsonObject["c"]!!
            .jsonPrimitive.content
        assertEquals("value", cVal)
    }

    /** test_3: Collections (List/Set) and reference arrays */
    @Test
    fun mapToJson_collectionsAndReferenceArrays() {
        val src = mapOf(
            "list" to listOf(1, 2, 3),
            "set"  to setOf("x", "y"),
            "arr"  to arrayOf("a", "b", "c")
        )
        val json = src.mapToJson().jsonObject
        assertEquals(3, json["list"]!!.jsonArray.size)
        assertEquals(2, json["set"]!!.jsonArray.size)
        assertEquals("b", json["arr"]!!.jsonArray[1].jsonPrimitive.content)
    }

    /** test_4: Primitive arrays (all) */
    @Test
    fun mapToJson_primitiveArrays() {
        val src = mapOf(
            "ba" to booleanArrayOf(true, false),
            "ia" to intArrayOf(1, 2, 3),
            "la" to longArrayOf(10L, 20L),
            "fa" to floatArrayOf(1f, 2f),
            "da" to doubleArrayOf(2.5, 3.5),
            "sa" to shortArrayOf(7, 8),
            "ca" to charArrayOf('Q', 'W')
        )
        val json = src.mapToJson().jsonObject
        assertEquals(true,  json["ba"]!!.jsonArray[0].jsonPrimitive.boolean)
        assertEquals(3,     json["ia"]!!.jsonArray[2].jsonPrimitive.int)
        assertEquals(20L,   json["la"]!!.jsonArray[1].jsonPrimitive.long)
        assertEquals(2.0,   json["fa"]!!.jsonArray[1].jsonPrimitive.double, 1e-9)
        assertEquals(3.5,   json["da"]!!.jsonArray[1].jsonPrimitive.double, 1e-9)
        assertEquals(8,     json["sa"]!!.jsonArray[1].jsonPrimitive.int)
        assertEquals("W",   json["ca"]!!.jsonArray[1].jsonPrimitive.content)
    }

    private enum class Color { GREEN }

    /** test_5: Enum and Char */
    @Test
    fun mapToJson_enumAndChar() {
        val src = mapOf(
            "e" to Color.GREEN,
            "ch" to 'X'
        )
        val json = src.mapToJson().jsonObject
        assertEquals("GREEN", json["e"]!!.jsonPrimitive.content)
        assertEquals("X",     json["ch"]!!.jsonPrimitive.content)
    }

    /** test_6: Sequence truncated and encoded */
    @Test
    fun mapToJson_sequenceLimited() {
        val seq = generateSequence(1) { it + 1 }
        val src = mapOf("seq" to seq)
        val json = src.mapToJson(maxCollectionElements = 3).jsonObject
        val arr = json["seq"]!!.jsonArray
        assertEquals(3, arr.size)
        assertEquals(1, arr[0].jsonPrimitive.int)
        assertEquals(3, arr[2].jsonPrimitive.int)
    }

    /** test_7: Non-string keys are filtered out */
    @Test
    fun mapToJson_filtersNonStringKeys() {
        val inner: Map<Any, Any?> = mapOf(
            123 to "numKey",
            "ok" to "strKey"
        )
        val src = mapOf("wrap" to inner)
        val json = src.mapToJson().jsonObject
        val wrap = json["wrap"]!!.jsonObject
        assertEquals(1, wrap.size)
        assertEquals("strKey", wrap["ok"]!!.jsonPrimitive.content)
    }

    private class BadToString {
        override fun toString(): String = throw RuntimeException("boom")
    }

    /** test_8: Fallback to safe toString() => "null" on exception */
    @Test
    fun mapToJson_fallbackToStringOnError() {
        val src = mapOf("bad" to BadToString())
        val json = src.mapToJson().jsonObject
        assertEquals("null", json["bad"]!!.jsonPrimitive.content)
    }

    /** test_9: Cycles detection => JsonNull */
    @Test
    fun mapToJson_detectsCycles() {
        val self = mutableMapOf<String, Any?>()
        self["self"] = self
        val json = mapOf("root" to self).mapToJson().jsonObject
        assertTrue(json["root"]!!.jsonObject["self"] is JsonNull)
    }

    /** test_10: Max depth guard => JsonNull on overflow */
    @Test
    fun mapToJson_depthGuard() {
        val src = mapOf("a" to mapOf("b" to mapOf("c" to "v")))
        val json = src.mapToJson(maxDepth = 2).jsonObject
        val a = json["a"]!!.jsonObject
        val b = a["b"]!!.jsonObject
        assertTrue(b["c"] is JsonNull)
    }

    /** test_11: maxCollectionElements limits size */
    @Test
    fun mapToJson_collectionLimit() {
        val bigList = (1..100).toList()
        val src = mapOf("l" to bigList)
        val json = src.mapToJson(maxCollectionElements = 5).jsonObject
        val arr = json["l"]!!.jsonArray
        assertEquals(5, arr.size)
        assertEquals(5, arr.last().jsonPrimitive.int)
    }
}
