@file:Suppress("SpellCheckingInspection")

package test.inapp.subscription.status

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.platform.subscription.status.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies in-app subscription status path builder.
 */
class PathTest {

    @Test
    fun `inAppStatusPath returns correct path`() {
        assertEquals(
            "/inapp/status",
            Path.inAppStatusPath()
        )
    }
}
