@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import org.junit.Assert.assertEquals
import org.junit.Test

private typealias ACStringBuilder = com.altcraft.sdk.util.StringBuilder

private const val JWT      = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0"
private const val RTOKEN   = "rt_abc123def456"

private const val MSG_SAMPLE = "ok"
private const val MSG_EMPTY  = ""
private const val LOG_KEY    = "key"
private const val LOG_VAL    = "val"

private const val EXPECT_BEARER_JWT         = "Bearer $JWT"
private const val EXPECT_BEARER_RTOKEN      = "Bearer rtoken@$RTOKEN"

/**
 * StringBuilderTest
 *
 * Positive scenarios:
 *  - test_1: eventLogBuilder with non-null message returns the message as-is.
 *  - test_2: eventLogBuilder with null message returns empty string.
 *  - test_3: eventLogBuilder with empty string message returns empty string.
 *  - test_4: eventLogBuilder with logValue appends key-value pairs.
 *  - test_5: eventLogBuilder when logValue contains null/blank values filters them out.
 *  - test_7: bearerJwtToken returns "Bearer <jwt>".
 *  - test_8: bearerRToken returns "Bearer rtoken@<token>".
 */
class StringBuilderTest {

    /** - test_1: eventLogBuilder returns message when no logValue provided. */
    @Test
    fun `eventLogBuilder returns message when no logValue`() {
        val actual = ACStringBuilder.eventLogBuilder(MSG_SAMPLE)
        assertEquals(MSG_SAMPLE, actual)
    }

    /** - test_2: eventLogBuilder returns empty string when message is null. */
    @Test
    fun `eventLogBuilder returns empty string when message is null`() {
        val actual = ACStringBuilder.eventLogBuilder(null)
        assertEquals(MSG_EMPTY, actual)
    }

    /** - test_3: eventLogBuilder returns empty string when message is blank. */
    @Test
    fun `eventLogBuilder returns empty string when message is blank`() {
        val actual = ACStringBuilder.eventLogBuilder(MSG_EMPTY)
        assertEquals(MSG_EMPTY, actual)
    }

    /** - test_4: eventLogBuilder appends logValue details after message. */
    @Test
    fun `eventLogBuilder appends logValue details`() {
        val logValue = mapOf(LOG_KEY to LOG_VAL)
        val actual = ACStringBuilder.eventLogBuilder(MSG_SAMPLE, logValue)
        assertEquals("$MSG_SAMPLE $LOG_KEY: $LOG_VAL", actual)
    }

    /** - test_5: eventLogBuilder filters null and blank logValues. */
    @Test
    fun `eventLogBuilder filters null and blank logValues`() {
        val logValue = mapOf(
            "a" to "visible",
            "b" to null,
            "c" to "",
            "d" to "  "
        )
        val actual = ACStringBuilder.eventLogBuilder(MSG_SAMPLE, logValue)
        assertEquals("$MSG_SAMPLE a: visible", actual)
    }

    @Test
    fun `bearerJwtToken formats correctly`() {
        assertEquals(EXPECT_BEARER_JWT, ACStringBuilder.bearerJwtToken(JWT))
    }

    /** - test_8: bearerRToken formats correctly. */
    @Test
    fun `bearerRToken formats correctly`() {
        assertEquals(EXPECT_BEARER_RTOKEN, ACStringBuilder.bearerRToken(RTOKEN))
    }
}
