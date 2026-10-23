@file:Suppress("SpellCheckingInspection")

package test.inapp.placements

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.platform.placements.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies in-app placements path builder.
 */
class PathTest {

    @Test
    fun `inAppPlacementsPath returns correct path`() {
        assertEquals(
            "/inapp/placements",
            Path.inAppPlacementsPath()
        )
    }
}
