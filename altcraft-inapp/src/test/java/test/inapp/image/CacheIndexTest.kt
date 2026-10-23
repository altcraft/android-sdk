@file:Suppress("SpellCheckingInspection")

package test.inapp.image

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.system.image.CacheIndex
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * CacheIndexTest
 *
 * Positive scenarios:
 *  - test_1: put and get round-trip a file for the same key.
 *  - test_2: get returns null for a key that was never stored.
 *  - test_3: clear removes all entries.
 *  - test_4: put overwrites previous value for the same key.
 */
class CacheIndexTest {

    @After
    fun tearDown() {
        CacheIndex.clear()
    }

    /** test_1: put then get returns the same file. */
    @Test
    fun test_1_putAndGet_roundTrip() {
        val file = File("/tmp/test.png")
        CacheIndex.put("https://example.com/img.png", file)
        assertEquals(file, CacheIndex.get("https://example.com/img.png"))
    }

    /** test_2: get returns null for unknown key. */
    @Test
    fun test_2_get_missingKey_returnsNull() {
        assertNull(CacheIndex.get("https://example.com/nonexistent.png"))
    }

    /** test_3: clear removes all entries. */
    @Test
    fun test_3_clear_emptiesIndex() {
        CacheIndex.put("k1", File("/tmp/1.png"))
        CacheIndex.put("k2", File("/tmp/2.png"))
        CacheIndex.clear()
        assertNull(CacheIndex.get("k1"))
        assertNull(CacheIndex.get("k2"))
    }

    /** test_4: put overwrites existing value for the same key. */
    @Test
    fun test_4_put_overwritesExisting() {
        val first = File("/tmp/first.png")
        val second = File("/tmp/second.png")
        CacheIndex.put("k", first)
        CacheIndex.put("k", second)
        assertEquals(second, CacheIndex.get("k"))
    }
}
