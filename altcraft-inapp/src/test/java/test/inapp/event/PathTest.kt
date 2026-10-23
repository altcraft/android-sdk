@file:Suppress("SpellCheckingInspection")

package test.inapp.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.platform.event.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies in-app event path builder.
 */
class PathTest {

    @Test
    fun `eventInAppPath returns correct path`() {
        assertEquals(
            "/inapp/event/impression",
            Path.eventInAppPath("impression")
        )
    }
}
