package test.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.work.Data
import com.altcraft.sdk.extension.DataExtension.toStringMap
import org.junit.Assert.*
import org.junit.Test

/**
 * DataExtensionUnitTest
 *
 * Positive scenarios:
 *  - test_1: Converts Data with strings and numbers to string map.
 *  - test_2: Ignores null values.
 *
 * Negative scenarios:
 *  - test_3: Empty Data returns empty map.
 */
class DataExtensionTest {

    /** test_1: Converts Data with strings and numbers */
    @Test
    fun toStringMap_convertsValues() {
        val data = Data.Builder()
            .putString("k1", "v1")
            .putInt("k2", 42)
            .putBoolean("k3", true)
            .build()

        val map = data.toStringMap()

        assertEquals("v1", map["k1"])
        assertEquals("42", map["k2"])
        assertEquals("true", map["k3"])
    }

    /** test_2: Null values are filtered out */
    @Test
    fun toStringMap_filtersNulls() {
        val data = Data.Builder()
            .putString("k1", null)
            .build()

        val map = data.toStringMap()
        assertFalse(map.containsKey("k1"))
    }

    /** test_3: Empty data → empty map */
    @Test
    fun toStringMap_emptyData() {
        val map = Data.Builder().build().toStringMap()
        assertTrue(map.isEmpty())
    }
}