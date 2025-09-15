package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altcraft.sdk.additional.SubFunction
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SubFunctionInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: fromAssets() with an existing asset returns a non-null Bitmap with positive width/height.
 *
 * Negative scenarios:
 *  - test_2: fromAssets() with a missing asset returns null without crashing.
 *
 * Notes:
 *  - Use targetContext to read assets bundled with the app-under-test (src/main/assets).
 *  - Asset filenames and assertion messages are defined as constants below.
 */
@RunWith(AndroidJUnit4::class)
class SubFunctionInstrumentedTest {

    // -------- Constants --------
    private companion object {
        // Asset filenames
        private const val ASSET_EXISTING = "test.jpg"
        private const val ASSET_MISSING  = "no_such_file.png"

        // Assertion messages
        private const val MSG_BITMAP_NOT_NULL   = "Expected bitmap from test asset"
        private const val MSG_BITMAP_NON_EMPTY  = "Bitmap must have positive size"
    }

    /** fromAssets(): returns Bitmap for existing asset file. */
    @Test
    fun fromAssets_success() {
        // Use targetContext to read assets from app-under-test (src/main/assets)
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val bmp: Bitmap? = SubFunction.fromAssets(ctx, ASSET_EXISTING)

        assertNotNull(MSG_BITMAP_NOT_NULL, bmp)
        val nonNull = requireNotNull(bmp)
        assertTrue(MSG_BITMAP_NON_EMPTY, nonNull.width > 0 && nonNull.height > 0)
    }

    /** fromAssets(): returns null for non-existing asset file without crashing. */
    @Test
    fun fromAssets_missing_returnsNull() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val bmp = SubFunction.fromAssets(ctx, ASSET_MISSING)
        assertNull(bmp)
    }
}
