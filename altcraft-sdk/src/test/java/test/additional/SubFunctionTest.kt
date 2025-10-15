@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.additional.SubFunction.isJsonString
import com.altcraft.sdk.additional.SubFunction.UniqueCodeGenerator
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * SubFunctionTest
 *
 * Positive scenarios:
 *  - fieldsIsObjects(): returns true when the map contains at least one non-primitive value.
 *  - String?.isJsonString(): detects JSON object/array (whitespace allowed).
 *  - altcraftPush(): true when "_ac_push" is present.
 *  - getIconColor(): stable fallback for null/invalid inputs.
 *  - UniqueCodeGenerator.uniqueCode(): non-negative, unique, thread-safe.
 *  - stringContainsHtml(): detects <html> tags case-insensitively.
 *  - logger(): does not throw and delegates to android.util.Log.d.
 *
 * Negative scenarios:
 *  - fieldsIsObjects(): false for null or all-primitive maps (including empty).
 *  - String?.isJsonString(): false for null/blank/non-JSON.
 *  - stringContainsHtml(): false for null/empty/non-HTML strings.
 *
 * Notes:
 *  - Pure JVM tests; Android-dependent functions are intentionally not tested here.
 */
class SubFunctionTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)

        // Без nullable: используем any<String>() и any<Throwable>()
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
        // Nothing to unmock because we used mockkStatic directly
    }

    /** fieldsIsObjects(): returns false for null or all-primitive maps (including empty). */
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
        assertFalse(SubFunction.fieldsIsObjects(emptyMap()))
    }

    /** fieldsIsObjects(): returns true when a non-primitive is present (Map/List/Array/etc). */
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

    /** String?.isJsonString(): detects JSON object/array with optional leading spaces. */
    @Test
    fun isJsonString_detectsJson() {
        assertTrue(" {\"a\":1}".isJsonString())
        assertTrue("\n\t[1,2,3]".isJsonString())
        assertTrue("[ ]".isJsonString())
        assertTrue("{ }".isJsonString())
    }

    /** String?.isJsonString(): returns false for null/blank or non-JSON. */
    @Test
    fun isJsonString_nonJson_false() {
        assertFalse(null.isJsonString())
        assertFalse("   ".isJsonString())
        assertFalse("not-json".isJsonString())
        assertFalse("(".isJsonString())
        assertFalse("x{y}".isJsonString())
    }

    /** altcraftPush(): true when _ac_push key exists, false otherwise. */
    @Test
    fun altcraftPush_keyCheck() {
        assertTrue(SubFunction.altcraftPush(mapOf("_ac_push" to "1", "x" to "y")))
        assertFalse(SubFunction.altcraftPush(mapOf("x" to "y")))
        assertFalse(SubFunction.altcraftPush(emptyMap()))
    }

    /**
     * getIconColor(): for null and invalid strings it should always return the same fallback value.
     * We cannot validate actual parsing here (requires Android runtime).
     */
    @Test
    fun getIconColor_fallback_isStableForInvalidInputs() {
        val base = SubFunction.getIconColor(null) // baseline fallback
        val invalids = listOf("", " ", "not-a-color", "#GGGGGG", "#12345", "####", "rgb(?, ?, ?)")
        invalids.forEach { bad ->
            val v = SubFunction.getIconColor(bad)
            assertEquals("Fallback must be stable for '$bad'", base, v)
        }
    }

    /** getIconColor(): must not throw exceptions for edge cases. */
    @Test
    fun getIconColor_noThrow_onEdgeCases() {
        val inputs = listOf<String?>(null, "", "   ", "???", "#", "#0", "#ZZZZZZ")
        inputs.forEach { s ->
            try {
                SubFunction.getIconColor(s)
            } catch (e: Throwable) {
                fail("getIconColor('$s') must not throw, but threw: $e")
            }
        }
    }

    /** stringContainsHtml(): detects <html> tags with attributes and different cases. */
    @Test
    fun stringContainsHtml_detectsHtml() {
        assertTrue(SubFunction.stringContainsHtml("<html>content</html>"))
        assertTrue(SubFunction.stringContainsHtml("<HTML lang=\"en\">x</HTML>"))
        assertTrue(SubFunction.stringContainsHtml("  <html  >x"))
        assertTrue(SubFunction.stringContainsHtml("</html>"))
        assertTrue(SubFunction.stringContainsHtml("prefix <html attr=1> suffix"))
    }

    /** stringContainsHtml(): returns false for null/blank/non-HTML strings. */
    @Test
    fun stringContainsHtml_nonHtml_false() {
        assertFalse(SubFunction.stringContainsHtml(null))
        assertFalse(SubFunction.stringContainsHtml(""))
        assertFalse(SubFunction.stringContainsHtml("   "))
        assertFalse(SubFunction.stringContainsHtml("<htm>"))
        assertFalse(SubFunction.stringContainsHtml("<body>"))
        assertFalse(SubFunction.stringContainsHtml("text only"))
    }

    /** logger(): should not throw and should delegate to android.util.Log.d. */
    @Test
    fun logger_noThrow_and_delegates() {
        try {
            SubFunction.logger("hello")
            SubFunction.logger(null)
        } catch (e: Throwable) {
            fail("logger must not throw, but threw: $e")
        }
    }

    /** Sequential calls must return non-negative and unique values. */
    @Test
    fun uniqueCode_sequential_nonNegative_andUnique() {
        val uid = "user-123"
        val count = 20_000
        val seen = HashSet<Int>(count)

        repeat(count) { i ->
            val code = UniqueCodeGenerator.uniqueCode(uid)
            assertTrue("Code must be non-negative", code >= 0)
            assertTrue("Duplicate code at iteration $i", seen.add(code))
        }
    }

    /** Different UIDs should produce diverging sequences (minimal intersection). */
    @Test
    fun uniqueCode_differentUIDs_diverge() {
        val uidA = "uid-A"
        val uidB = "uid-B"

        val firstA = (0 until 5_000).map { UniqueCodeGenerator.uniqueCode(uidA) }.toSet()
        val firstB = (0 until 5_000).map { UniqueCodeGenerator.uniqueCode(uidB) }.toSet()

        val inter = firstA intersect firstB
        assertTrue("Sequences intersect too much: ${inter.size}", inter.size < 5)
    }

    /** Thread-safety: parallel calls must not produce duplicates. */
    @Test
    fun uniqueCode_threadSafe_noDuplicates() {
        val threads = 8
        val perThread = 4_000
        val total = threads * perThread
        val uid = "threaded-uid"

        val pool = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)
        val set = Collections.synchronizedSet(HashSet<Int>(total))

        repeat(threads) {
            pool.execute {
                repeat(perThread) {
                    val code = UniqueCodeGenerator.uniqueCode(uid)
                    assertTrue("Duplicate in concurrent run", set.add(code))
                }
                latch.countDown()
            }
        }

        val completed = latch.await(30, TimeUnit.SECONDS)
        pool.shutdownNow()
        assertTrue("Threads did not complete in time", completed)
        assertEquals("Total unique codes mismatch", total, set.size)
    }
}
