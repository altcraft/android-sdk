package test.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.events.Events
import com.altcraft.sdk.json.Converter.fromStringJson
import com.altcraft.sdk.json.Converter.toStringJson
import com.altcraft.sdk.data.DataClasses
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ConverterTest
 *
 * Positive:
 *  - test_1: toStringJson serializes with defaults & nulls.
 *  - test_2: fromStringJson deserializes with ignoreUnknownKeys + isLenient.
 *
 * Negative:
 *  - test_3: fromStringJson on malformed JSON -> null + Events.error.
 *  - test_4: toStringJson on non-serializable -> null + Events.error.
 *  - test_5: fromStringJson on empty string -> null + error; null input -> null w/o error.
 *
 * Notes:
 *  - Pure JVM unit tests. Uses real kotlinx.serialization.
 *  - Events.error is mocked for verification and to avoid side effects.
 */
class ConverterTest {

    private companion object {
        const val FN_TO = "toStringJson"
        const val FN_FROM = "fromStringJson"

        const val NAME_1 = "Anna"
        const val NAME_2 = "Andrey"
        const val AGE_1 = 30
        const val AGE_2 = 32

        val LIKES = listOf("math", "logic")

        val SRC_LENIENT = """
            {
              'name': '$NAME_2',
              'age': $AGE_2,
              'unknown': 'skip-me',
              'likes': ['${LIKES[0]}','${LIKES[1]}']
            }
        """.trimIndent().replace('\'', '"')

        const val SRC_BAD = "{ name: 'no-quotes, broken json' "
    }

    @Before
    fun setUp() {
        mockkObject(Events)
        every { Events.error(any(), any(), any()) } returns DataClasses.Error("fn", 400, "err", null)
    }

    @After
    fun tearDown() = unmockkAll()

    @Serializable
    data class Person(
        val name: String,
        val age: Int = AGE_1,
        val nick: String? = null,
        val likes: List<String> = emptyList()
    )

    /** Verifies serialization with defaults and nulls */
    @Test
    fun toStringJson_serializesWithDefaultsAndNulls() {
        val json = Person(name = NAME_1).toStringJson(FN_TO)
        assertNotNull(json)
        val s = json!!

        assertTrue(s.contains("\"name\":\"$NAME_1\""))
        assertTrue(s.contains("\"age\":$AGE_1"))
        assertTrue(s.contains("\"nick\":null"))
        assertTrue(s.contains("\"likes\":[]"))

        verify(exactly = 0) { Events.error(any(), any(), any()) }
    }

    /** Verifies deserialization with ignoreUnknownKeys and isLenient */
    @Test
    fun fromStringJson_deserializesLenientAndIgnoresUnknown() {
        val obj = SRC_LENIENT.fromStringJson<Person>(FN_FROM)
        assertNotNull(obj)
        obj!!

        assertEquals(NAME_2, obj.name)
        assertEquals(AGE_2, obj.age)
        assertEquals(LIKES, obj.likes)
        assertNull(obj.nick)

        verify(exactly = 0) { Events.error(any(), any(), any()) }
    }

    /** Verifies malformed JSON returns null and logs error */
    @Test
    fun fromStringJson_malformed_returnsNull_andLogsError() {
        val res = SRC_BAD.fromStringJson<Person>(FN_FROM)
        assertNull(res)
        verify(exactly = 1) { Events.error(eq(FN_FROM), any(), any()) }
    }

    /** Verifies non-serializable type returns null and logs error */
    @Test
    fun toStringJson_nonSerializable_returnsNull_andLogsError() {
        class NotSerializable(val x: Int)
        val obj = NotSerializable(10)
        val res = obj.toStringJson(FN_TO)
        assertNull(res)
        verify(exactly = 1) { Events.error(eq(FN_TO), any(), any()) }
    }

    /** Verifies empty string → null + error; null input → null w/o error */
    @Test
    fun fromStringJson_emptyAndNullInputs() {
        val empty = "".fromStringJson<Person>(FN_FROM)
        assertNull(empty)
        verify(exactly = 1) { Events.error(eq(FN_FROM), any(), any()) }

        // reset
        unmockkAll()
        mockkObject(Events)
        every { Events.error(any(), any(), any()) } returns DataClasses.Error("fn", 400, "err", null)

        val nullStr: String? = null
        val nullRes = nullStr.fromStringJson<Person>(FN_FROM)
        assertNull(nullRes)
        verify(exactly = 0) { Events.error(any(), any(), any()) }
    }
}
