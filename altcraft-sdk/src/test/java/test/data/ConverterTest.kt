@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2024 Altcraft. All rights reserved.

import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.Converter
import com.altcraft.sdk.events.Events
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkAll

/**
 * ConverterUnitTest
 *
 * Positive scenarios:
 *  - test_1: String list round-trip with tricky strings (unicode, quotes, spaces).
 *  - test_2: AppInfo round-trip preserves all fields.
 *  - test_3: JsonElement object/array round-trip and idempotence.
 *  - test_7: Empty String list round-trip remains empty.
 *
 * Negative scenarios:
 *  - test_4: toStringList returns null on malformed JSON.
 *  - test_5: toAppInfo returns null on malformed JSON.
 *  - test_6: toCategoryDataList returns null on malformed JSON.
 *  - test_8: CategoryData list null-handling (both directions) returns null.
 *
 * Notes:
 *  - Pure JVM unit tests; no Android runtime needed.
 *  - We DO NOT test json.Converter internals; we only ensure room.Converter
 *    returns null on bad input. To avoid noise, Events.error is stubbed.
 */

// ---------- Assertion messages ----------
private const val MSG_JSON_NOT_NULL = "Serialized JSON must not be null"
private const val MSG_ROUND_TRIP_EQUAL = "Round-trip result must equal original"
private const val MSG_NULL_IN_NULL_OUT = "Null input must yield null output"
private const val MSG_MALFORMED_RETURNS_NULL = "Malformed JSON must return null"
private const val MSG_IDEMPOTENT = "Result must be idempotent across conversions"
private const val MSG_TO_STRING_LIST_NULL = "toStringList(null) must return null"
private const val MSG_FROM_STRING_LIST_NULL = "fromStringList(null) must return null"
private const val MSG_TO_APP_INFO_NULL = "toAppInfo(null) must return null"
private const val MSG_FROM_APP_INFO_NULL = "fromAppInfo(null) must return null"
private const val MSG_EMPTY_ROUND_TRIP = "Empty list must survive round-trip unchanged"
private const val MSG_CAT_LIST_TO_NULL = "toCategoryDataList(null) must return null"
private const val MSG_CAT_LIST_FROM_NULL = "fromCategoryDataList(null) must return null"

// ---------- Sample test data ----------
private const val STR_SIMPLE = "hello"
private const val STR_EMPTY = ""
private const val STR_SPACES = "  with spaces  "
private const val STR_QUOTES = "quotes: \"double\" and 'single' and backslash \\ "
private const val STR_UNICODE = "emoji: 😎 snow: ❄  cyrillic: кириллица"
private val SAMPLE_STR_LIST = listOf(STR_SIMPLE, STR_EMPTY, STR_SPACES, STR_QUOTES, STR_UNICODE)

private const val APP_ID = "AltcraftMobile"
private const val APP_IID = "1.2.3"
private const val APP_VER = "9.9.9"

// Intentionally malformed JSON strings
private const val JSON_MALFORMED_1 = "not a json"
private const val JSON_MALFORMED_2 = "{ broken: [ 1, 2, }"

class ConverterTest {

    private val conv = Converter()

    @Before
    fun setUp() {
        mockkObject(Events)
        every { Events.error(any(), any(), any()) } returns
                DataClasses.Error("converter", 400, "err", null)
        every { Events.subscribe(any()) } just Runs
        every { Events.unsubscribe() } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    // ----------------- String List -----------------

    /** fromStringList/toStringList: round-trip with tricky values */
    @Test
    fun stringList_roundTrip_ok() {
        val json = conv.fromStringList(SAMPLE_STR_LIST)
        assertNotNull(MSG_JSON_NOT_NULL, json)

        val restored = conv.toStringList(json)
        assertEquals(MSG_ROUND_TRIP_EQUAL, SAMPLE_STR_LIST, restored)
    }

    /** Empty list round-trip remains empty */
    @Test
    fun stringList_empty_roundTrip_ok() {
        val empty = emptyList<String>()
        val json = conv.fromStringList(empty)
        assertNotNull(MSG_JSON_NOT_NULL, json)

        val restored = conv.toStringList(json)
        assertEquals(MSG_EMPTY_ROUND_TRIP, empty, restored)
    }

    /** Null handling for string list */
    @Test
    fun stringList_nullHandling_ok() {
        assertNull(MSG_FROM_STRING_LIST_NULL, conv.fromStringList(null))
        assertNull(MSG_TO_STRING_LIST_NULL, conv.toStringList(null))
    }

    /** Malformed JSON → toStringList returns null */
    @Test
    fun stringList_malformedJson_returnsNull() {
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toStringList(JSON_MALFORMED_1))
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toStringList(JSON_MALFORMED_2))
    }

    // ----------------- AppInfo -----------------

    /** fromAppInfo/toAppInfo: round-trip preserves fields */
    @Test
    fun appInfo_roundTrip_ok() {
        val original = DataClasses.AppInfo(
            appID = APP_ID,
            appIID = APP_IID,
            appVer = APP_VER
        )
        val json = conv.fromAppInfo(original)
        assertNotNull(MSG_JSON_NOT_NULL, json)

        val restored = conv.toAppInfo(json)
        assertEquals(MSG_ROUND_TRIP_EQUAL, original, restored)
    }

    /** Null handling for AppInfo */
    @Test
    fun appInfo_nullHandling_ok() {
        assertNull(MSG_FROM_APP_INFO_NULL, conv.fromAppInfo(null))
        assertNull(MSG_TO_APP_INFO_NULL, conv.toAppInfo(null))
    }

    /** Malformed JSON → toAppInfo returns null */
    @Test
    fun appInfo_malformedJson_returnsNull() {
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toAppInfo(JSON_MALFORMED_1))
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toAppInfo(JSON_MALFORMED_2))
    }

    // ----------------- CategoryData List -----------------
    // We don’t construct CategoryData here — we only validate null/malformed behavior.

    /** Malformed JSON → toCategoryDataList returns null */
    @Test
    fun categoryDataList_malformedJson_returnsNull() {
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toCategoryDataList(JSON_MALFORMED_1))
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toCategoryDataList(JSON_MALFORMED_2))
    }

    /** Null handling for CategoryData list in both directions */
    @Test
    fun categoryDataList_nullHandling_ok() {
        assertNull(MSG_CAT_LIST_FROM_NULL, conv.fromCategoryDataList(null))
        assertNull(MSG_CAT_LIST_TO_NULL, conv.toCategoryDataList(null))
    }

    // ----------------- JsonElement -----------------

    /** JsonElement object round-trip */
    @Test
    fun jsonElement_object_roundTrip_ok() {
        val original: JsonElement = buildJsonObject {
            put("k1", "v1")
            put("k2", 123)
            put("nested", buildJsonObject {
                put("flag", true)
                put("arr", buildJsonArray { add("x"); add(42) })
            })
        }

        val json = conv.fromJsonElement(original)
        assertNotNull(MSG_JSON_NOT_NULL, json)

        val restored = conv.toJsonElement(json)
        assertEquals(MSG_ROUND_TRIP_EQUAL, original, restored)
    }

    /** JsonElement array round-trip + idempotence check */
    @Test
    fun jsonElement_array_roundTrip_and_idempotent_ok() {
        val original: JsonElement = buildJsonArray {
            add(buildJsonObject { put("a", 1) })
            add(buildJsonObject { put("b", "2") })
        }

        val json = conv.fromJsonElement(original)
        val restored = conv.toJsonElement(json)
        assertEquals(MSG_ROUND_TRIP_EQUAL, original, restored)

        // Idempotence: toJsonElement(fromJsonElement(x)) == x
        val json2 = conv.fromJsonElement(restored)
        val restored2 = conv.toJsonElement(json2)
        assertEquals(MSG_IDEMPOTENT, original, restored2)
    }

    /** JsonElement null handling */
    @Test
    fun jsonElement_nullHandling_ok() {
        assertNull(MSG_NULL_IN_NULL_OUT, conv.fromJsonElement(null))
        assertNull(MSG_NULL_IN_NULL_OUT, conv.toJsonElement(null))
    }

    /** JsonElement malformed string → toJsonElement returns null */
    @Test
    fun jsonElement_malformed_returnsNull() {
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toJsonElement(JSON_MALFORMED_1))
        assertNull(MSG_MALFORMED_RETURNS_NULL, conv.toJsonElement(JSON_MALFORMED_2))
    }
}
