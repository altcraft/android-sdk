@file:Suppress("SpellCheckingInspection")

package test.target

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.SubFunction.fieldsIsObjects
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TargetPayloadGuardTest
 *
 * Verifies the primitive-field guard applied to target payloads.
 *
 * Positive scenarios:
 *  - Maps with only primitive values are accepted.
 *  - Null and empty payloads are accepted.
 *
 * Negative scenarios:
 *  - A payload containing an object value is rejected.
 *  - A payload containing an array value is rejected.
 */
class TargetPayloadGuardTest {

    @Test
    fun `primitives only is accepted`() {
        assertFalse(
            fieldsIsObjects(mapOf("a" to 1, "b" to "x", "c" to true, "d" to 4.5))
        )
    }

    @Test
    fun `null or empty payload is accepted`() {
        assertFalse(fieldsIsObjects(null))
        assertFalse(fieldsIsObjects(emptyMap()))
        assertFalse(fieldsIsObjects(mapOf("a" to null)))
    }

    @Test
    fun `object value is rejected`() {
        assertTrue(
            fieldsIsObjects(mapOf("a" to mapOf("nested" to 1)))
        )
    }

    @Test
    fun `array value is rejected`() {
        assertTrue(
            fieldsIsObjects(mapOf("a" to listOf(1, 2)))
        )
    }
}
