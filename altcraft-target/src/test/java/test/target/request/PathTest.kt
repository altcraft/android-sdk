@file:Suppress("SpellCheckingInspection")

package test.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.platform.target.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies the target event endpoint path builder.
 */
class PathTest {

    @Test
    fun `targetPath returns correct path`() {
        assertEquals("/event/post", Path.targetPath())
    }
}
