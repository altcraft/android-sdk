@file:Suppress("SpellCheckingInspection")

package test.push.receiver

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.push.data.models.PushModels.ButtonStructure
import com.altcraft.sdk.push.system.receiver.PushData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PushDataTest
 *
 * Verifies incoming push payload parsing.
 *
 * Positive scenarios:
 *  - Field parsing extracts UID, body, title, URL, icon, image and extra.
 *  - Vibration/soundless flags are derived from the payload.
 *  - `_buttons` JSON is parsed into [ButtonStructure] entries.
 *
 * Negative scenarios:
 *  - Missing fields default to empty strings.
 *  - Malformed `_buttons` JSON is logged and degrades to null.
 */
class PushDataTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun fieldParsing_extractsKnownKeys() {
        val message = mapOf(
            "_uid" to "uid-1",
            "_body" to "Hello",
            "_title" to "Title",
            "_icon" to "https://icon",
            "_image" to "https://img",
            "_color" to "#FF0000",
            "_extra" to "payload",
            "_click_action" to "https://open"
        )

        val push = PushData(message)

        assertEquals("uid-1", push.uid)
        assertEquals("Hello", push.body)
        assertEquals("Title", push.title)
        assertEquals("https://icon", push.icon)
        assertEquals("https://img", push.image)
        assertEquals("#FF0000", push.color)
        assertEquals("payload", push.extra)
        assertEquals("https://open", push.url)
    }

    /** Missing fields default to empty strings. */
    @Test
    fun fieldParsing_missingFieldsDefaultToEmpty() {
        val push = PushData(emptyMap())

        assertEquals("", push.uid)
        assertEquals("", push.body)
        assertEquals("", push.title)
        assertEquals("", push.icon)
        assertEquals("", push.url)
    }

    /** Vibration and soundless flags are derived from their boolean payload. */
    @Test
    fun flags_vibrationAndSoundlessParsed() {
        assertTrue(PushData(mapOf("_vibration" to "true")).vibration)
        assertFalse(PushData(mapOf("_vibration" to "false")).vibration)
        assertTrue(PushData(mapOf("_soundless" to "true")).soundless)
        assertFalse(PushData(mapOf("_soundless" to "false")).soundless)
    }

    /** `_buttons` JSON is decoded into button structures. */
    @Test
    fun buttons_validJsonParses() {
        val push = PushData(
            mapOf("_buttons" to """[{"label":"Ok","link":"https://go"}]""")
        )

        val buttons = push.buttons
        assertEquals(1, buttons!!.size)
        assertEquals("Ok", buttons.first().label)
        assertEquals("https://go", buttons.first().link)
    }

    /** Malformed `_buttons` JSON degrades to null. */
    @Test
    fun buttons_malformedJsonReturnsNull() {
        val push = PushData(mapOf("_buttons" to "not-json"))

        assertNull(push.buttons)
    }

    /** Missing `_buttons` field returns null. */
    @Test
    fun buttons_missingFieldReturnsNull() {
        assertNull(PushData(emptyMap()).buttons)
    }
}
