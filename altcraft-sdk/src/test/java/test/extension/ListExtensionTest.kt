package test.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.extension.ListExtension.addJsonPart
import com.altcraft.sdk.extension.ListExtension.addTextPart
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

/**
 * ListExtensionTest
 *
 * Positive scenarios:
 *  - test_1: addTextPart adds exactly one part with correct headers, content type, and body.
 *  - test_2: addJsonPart adds exactly one part with correct headers, content type, and body.
 *  - test_3: addJsonPart is skipped when value is null (list unchanged).
 *  - test_4: multiple calls preserve order (text then json).
 *
 * Notes:
 *  - Pure JVM unit tests (no Android runtime).
 *  - Body verification is done by writing the RequestBody into an okio.Buffer.
 */
class ListExtensionTest {

    /** test_1: addTextPart adds one part with proper headers and body */
    @Test
    fun test_1_addTextPart_headersAndBodyAreCorrect() {
        val parts = mutableListOf<MultipartBody.Part>()

        parts.addTextPart(name = "field", value = "hello")

        assertEquals(1, parts.size)

        val part = parts.first()
        val cd = part.headers?.get("Content-Disposition")
        assertEquals("""form-data; name="field"""", cd)

        val ct = part.body.contentType()
        assertEquals("text/plain; charset=utf-8".toMediaType(), ct)

        val sink = Buffer()
        part.body.writeTo(sink)
        assertEquals("hello", sink.readUtf8())
    }

    /** test_2: addJsonPart adds one part with proper headers and body */
    @Test
    fun test_2_addJsonPart_headersAndBodyAreCorrect() {
        val parts = mutableListOf<MultipartBody.Part>()

        val json = """{"a":1,"b":"x"}"""
        parts.addJsonPart(name = "payload", jsonStr = json)

        assertEquals(1, parts.size)

        val part = parts.first()
        val cd = part.headers?.get("Content-Disposition")
        assertEquals("""form-data; name="payload"""", cd)

        val ct = part.body.contentType()
        assertEquals("application/json; charset=utf-8".toMediaType(), ct)

        val sink = Buffer()
        part.body.writeTo(sink)
        assertEquals(json, sink.readUtf8())
    }

    /** test_3: addJsonPart skips when null (no parts added) */
    @Test
    fun test_3_addJsonPart_null_isSkipped() {
        val parts = mutableListOf<MultipartBody.Part>()

        parts.addJsonPart(name = "payload", jsonStr = null)

        assertTrue(parts.isEmpty())
    }

    /** test_4: order is preserved when adding multiple parts */
    @Test
    fun test_4_multiple_calls_preserve_order() {
        val parts = mutableListOf<MultipartBody.Part>()

        parts.addTextPart("first", "one")
        parts.addJsonPart("second", """{"k":2}""")

        assertEquals(2, parts.size)

        val first = parts[0]
        val second = parts[1]

        // First part assertions
        assertEquals("""form-data; name="first"""", first.headers?.get("Content-Disposition"))
        assertEquals("text/plain; charset=utf-8".toMediaType(), first.body.contentType())
        Buffer().also { first.body.writeTo(it); assertEquals("one", it.readUtf8()) }

        // Second part assertions
        assertEquals("""form-data; name="second"""", second.headers?.get("Content-Disposition"))
        assertEquals("application/json; charset=utf-8".toMediaType(), second.body.contentType())
        Buffer().also { second.body.writeTo(it); assertEquals("""{"k":2}""", it.readUtf8()) }
    }
}
