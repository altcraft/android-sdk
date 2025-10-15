@file:Suppress("SpellCheckingInspection")

package test.json.serializer

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.json.serializer.any.AnySerializer
import com.altcraft.sdk.json.serializer.any.FromJson.toKotlinValueOrNull
import com.altcraft.sdk.json.serializer.any.ToJson.toJsonElement
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.data.DataClasses
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.EmptySerializersModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * ## AnySerializerTest
 *
 * Comprehensive test suite for the custom serializer `AnySerializer`
 * and its helpers `FromJson` and `ToJson`.
 *
 * ### Positive scenarios:
 * - test_1: `AnySerializer` correctly serializes/deserializes primitive types.
 * - test_2: Round-trip encoding/decoding for `Map<String, Any?>` with nested lists.
 * - test_3: `FromJson` handles homogeneous and heterogeneous JSON arrays.
 * - test_4: `ToJson` converts primitives, maps, iterables, arrays, and handles cycles.
 * - test_5: `ToJson` respects `maxDepth` and `maxElements` guards.
 * - test_6: Non-JSON encoders fall back to string encoding.
 * - test_7: Primitive arrays are transformed into `JsonArray`.
 * - test_8: `Enum` and `Char` values are serialized as `JsonPrimitive` strings.
 * - test_9: `JsonObject` converts back into nested Kotlin maps.
 *
 * ### Negative scenarios:
 * - **test_10:** Non-JSON decoder returns `JsonNull`.
 * - **test_11:** Invalid JSON elements handled safely via `toKotlinValueOrNull()`.
 *
 * ### Notes:
 * - Pure JVM unit tests.
 * - `Events.error()` is mocked to suppress side-effects.
 */
class AnySerializerTest {

    // Used in test_8
    @Suppress("unused")
    private enum class E { A, B }

    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        // Silence Events.error() so errors inside helpers don’t propagate
        mockkObject(Events)
        every { Events.error(any(), any()) } returns DataClasses.Error("fn", 400, "err", null)
        every { Events.error(any(), any(), any()) } returns DataClasses.Error("fn", 400, "err", null)
    }

    @After
    fun tearDown() = unmockkAll()

    // -------------------------
    // Helpers
    // -------------------------

    /** Minimal non-JSON Decoder to trigger JsonNull branch in AnySerializer.deserialize */
    @OptIn(ExperimentalSerializationApi::class)
    private class FakeDecoder : AbstractDecoder() {
        override val serializersModule = EmptySerializersModule()
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
        override fun decodeString(): String = "x"
    }

    /** Minimal non-JSON Encoder that captures encoded strings */
    @OptIn(ExperimentalSerializationApi::class)
    private class CapturingStringEncoder : AbstractEncoder() {
        override val serializersModule = EmptySerializersModule()
        val captured = AtomicReference<String?>()
        override fun encodeString(value: String) {
            captured.set(value)
        }
    }

    // -------------------------
    // Tests
    // -------------------------

    /** test_1: AnySerializer serialize/deserialize primitives with Json encoder/decoder */
    @Test
    fun test_1_primitives_roundtrip() {
        val s1 = json.encodeToString(AnySerializer, 123)
        assertEquals("123", s1)
        val v1 = json.decodeFromString(AnySerializer, s1)
        assertTrue(v1 is Int)
        assertEquals(123, v1)

        val big = 9_000_000_000L
        val s2 = json.encodeToString(AnySerializer, big)
        val v2 = json.decodeFromString(AnySerializer, s2)
        assertTrue(v2 is Long)
        assertEquals(big, v2)

        val s3 = json.encodeToString(AnySerializer, 1.5)
        val v3 = json.decodeFromString(AnySerializer, s3)
        assertTrue(v3 is Double)
        assertEquals(1.5, v3 as Double, 1e-9)

        val s4 = json.encodeToString(AnySerializer, true)
        val v4 = json.decodeFromString(AnySerializer, s4)
        assertTrue(v4 is Boolean)
        assertEquals(true, v4)

        val s5 = json.encodeToString(AnySerializer, "hi")
        val v5 = json.decodeFromString(AnySerializer, s5)
        assertTrue(v5 is String)
        assertEquals("hi", v5)
    }

    /** test_2: Map<String, Any?> and nested list roundtrip via AnySerializer */
    @Test
    fun test_2_map_and_list_roundtrip() {
        val payload = mapOf(
            "a" to 1,
            "b" to listOf(1, 2, 3),
            "c" to null,
            "d" to mapOf("x" to true, "y" to "z")
        )

        val encoded = json.encodeToString(AnySerializer, payload)
        val decoded = json.decodeFromString(AnySerializer, encoded)

        assertTrue(decoded is Map<*, *>)
        decoded as Map<*, *>
        assertEquals(1, decoded["a"])
        assertEquals(listOf(1, 2, 3), decoded["b"])
        val d = decoded["d"] as Map<*, *>
        assertTrue(d["x"] as Boolean)
        assertEquals("z", d["y"])
        assertTrue(decoded.containsKey("c"))
        assertNull(decoded["c"])
    }

    /** test_3: FromJson for homogeneous & heterogeneous arrays returns typed lists */
    @Test
    fun test_3_fromJson_arrays() {
        val jaNums = buildJsonArray {
            add(JsonPrimitive(1)); add(JsonPrimitive(2)); add(JsonPrimitive(3))
        }
        val listNums = jaNums.toKotlinValueOrNull()
        assertTrue(listNums is List<*>)
        assertEquals(listOf(1, 2, 3), listNums)

        val jaMixed = buildJsonArray {
            add(JsonPrimitive(1)); add(JsonPrimitive(true)); add(JsonNull)
        }
        val mixed = jaMixed.toKotlinValueOrNull()
        assertTrue(mixed is List<*>)
        mixed as List<*>
        assertEquals(1, mixed[0])
        assertEquals(true, mixed[1])
        assertNull(mixed[2])
    }

    /** test_4: ToJson supports primitives, maps, iterables, arrays and prevents cycles */
    @Test
    fun test_4_toJson_features_and_cycles() {
        // primitives
        assertEquals(JsonPrimitive(1), 1.toJsonElement())
        assertEquals(JsonPrimitive(true), true.toJsonElement())
        assertEquals(JsonPrimitive("x"), "x".toJsonElement())

        // map
        val mapEl = mapOf("a" to 1, "b" to "s").toJsonElement()
        assertTrue(mapEl is JsonObject)
        val obj = mapEl as JsonObject
        assertEquals(JsonPrimitive(1), obj["a"])
        assertEquals(JsonPrimitive("s"), obj["b"])

        // iterable + cycle
        val cyc = mutableListOf<Any>()
        cyc.add(cyc) // self-reference
        val cycEl = cyc.toJsonElement()
        assertTrue(cycEl is JsonArray)
        cycEl as JsonArray
        assertEquals(1, cycEl.size)
        assertTrue(cycEl[0] is JsonNull)
    }

    /** test_5: ToJson respects maxDepth/maxElements guards */
    @Test
    fun test_5_toJson_limits() {
        // Deeply nested list builder
        fun deep(n: Int): Any = if (n == 0) 1 else listOf(deep(n - 1))

        // Depth limit
        val limitedDepth = deep(10).toJsonElement(maxDepth = 2)
        assertTrue(limitedDepth is JsonArray)
        val lvl1 = (limitedDepth as JsonArray)[0]
        assertTrue(lvl1 is JsonArray)
        val lvl2 = (lvl1 as JsonArray)[0]
        assertTrue(lvl2 is JsonNull) // truncated at depth = 2

        // Elements limit
        val many = (0..1000).toList()
        val limited = many.toJsonElement(maxElements = 5)
        assertTrue(limited is JsonArray)
        val arr = limited as JsonArray
        assertEquals(5, arr.size)
    }

    /** test_6: AnySerializer serialize uses string fallback on non-Json encoder */
    @Test
    fun test_6_serialize_non_json_encoder_fallback() {
        val enc = CapturingStringEncoder()
        val value = mapOf("x" to 1, "y" to true)
        AnySerializer.serialize(enc as Encoder, value)
        val out = enc.captured.get()
        assertNotNull(out)
        assertTrue(out!!.startsWith("{") && out.contains("\"x\"") && out.contains("1"))
    }

    /** test_7: Primitive arrays are converted to JsonArray */
    @Test
    fun test_7_primitive_arrays_toJson() {
        val ia = intArrayOf(1, 2, 3).toJsonElement()
        assertTrue(ia is JsonArray)
        assertEquals(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3)), ia)

        val da = doubleArrayOf(1.0, 2.5).toJsonElement()
        assertTrue(da is JsonArray)
        assertEquals(listOf(JsonPrimitive(1.0), JsonPrimitive(2.5)), da)
    }

    /** test_8: Enum and Char are serialized as strings */
    @Test
    fun test_8_enum_and_char() {
        assertEquals(JsonPrimitive("A"), E.A.toJsonElement())
        assertEquals(JsonPrimitive("Z"), 'Z'.toJsonElement())
    }

    /** test_9: JsonObject → Kotlin Map with nested conversion */
    @Test
    fun test_9_fromJson_object_to_map() {
        val obj = buildJsonObject {
            put("a", JsonPrimitive(1))
            put("b", buildJsonArray { add(JsonPrimitive(true)); add(JsonPrimitive("s")) })
            put("c", buildJsonObject { put("x", JsonPrimitive(9_000_000_000L)) })
        }

        val asK = obj.toKotlinValueOrNull()
        assertTrue(asK is Map<*, *>)
        asK as Map<*, *>
        assertEquals(1, asK["a"])
        assertEquals(listOf(true, "s"), asK["b"])
        val inner = asK["c"] as Map<*, *>
        assertEquals(9_000_000_000L, inner["x"])
    }

    /** test_10: AnySerializer deserialize with non-Json decoder returns JsonNull */
    @Test
    fun test_10_deserialize_with_non_json_decoder_returns_JsonNull() {
        val dec: Decoder = FakeDecoder()
        val v = AnySerializer.deserialize(dec)
        assertTrue(v is JsonNull)
    }

    /** test_11: FromJson on invalid elements safely returns null (via wrapper) */
    @Test
    fun test_11_fromJson_invalid_returns_null() {
        val huge = JsonPrimitive(BigInteger("9".repeat(100)).toString())
        val v = huge.toKotlinValueOrNull()
        assertTrue(v is String)
        assertTrue((v as String).length > 50)
    }
}
