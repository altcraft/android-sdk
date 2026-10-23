@file:Suppress("SpellCheckingInspection")

package test.inapp.subscription.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.platform.subscription.subscribe.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies in-app subscription path builder.
 */
class PathTest {

    @Test
    fun `inAppSubscribePath returns correct path`() {
        assertEquals(
            "/inapp/subscribe",
            Path.inAppSubscribePath()
        )
    }
}
