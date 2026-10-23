@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import org.junit.Assert.assertEquals
import org.junit.Test

private typealias ACEndpoint = com.altcraft.sdk.platform.authenticate.internal.request.Path

private const val EXPECT_AUTHENTICATE = "/profile/authenticate"

/**
 * PathTest
 *
 * Positive scenarios:
 *  - test_1: authenticatePath returns the relative authorization path.
 */
class PathTest {

    /** - test_1: authenticatePath returns the relative path for the authorization request. */
    @Test
    fun `authenticatePath returns relative path`() {
        assertEquals(EXPECT_AUTHENTICATE, ACEndpoint.authenticatePath())
    }
}
