@file:Suppress("SpellCheckingInspection")

package test.profile.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.profile.platform.updateFields.request.Path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PathTest
 *
 * Verifies the profile update endpoint path builder.
 */
class PathTest {

    @Test
    fun `profileUpdatePath returns correct path`() {
        assertEquals("/profile/update", Path.profileUpdatePath())
    }
}
