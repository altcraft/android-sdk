@file:Suppress("SpellCheckingInspection")

package test.inapp.static

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.platform.static.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies in-app static resources path builder.
 */
class PathTest {

    @Test
    fun `inAppStaticPaths return correct paths`() {
        assertEquals(
            "/inapp/v1/js/inappLibrary.js",
            Path.inAppLibJsPath()
        )
        assertEquals(
            "/inapp/v1/js/inappBlock.js",
            Path.inAppBlockJsPath()
        )
        assertEquals(
            "/inapp/v1/css/inappBlock.css",
            Path.inAppBlockCssPath()
        )
    }
}
