@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.room.ConfigurationEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

private typealias ACStringBuilder = com.altcraft.sdk.additional.StringBuilder

private const val FN_PUSH_SUBSCRIBE = "pushSubscribe"
private const val FN_SIMPLE        = "fn"
private const val MSG_OK           = "ok"
private const val URL_VALID        = "https://pxl.altcraft.com"
private const val PROV_INVALID_1   = "android-unknown"
private const val PROV_INVALID_2   = "android-bad"
private const val PROV_INVALID_3   = "android-wrong"
private const val TOTAL_COUNT_EX   = 321
private const val EVENT_NAME_EX    = "event_xyz"

private const val EXPECT_LOG_OK            = "$FN_PUSH_SUBSCRIBE(): $MSG_OK"
private const val EXPECT_LOG_NULL_MESSAGE  = "$FN_PUSH_SUBSCRIBE():"
private const val EXPECT_LOG_EMPTY_MESSAGE = "$FN_SIMPLE(): "
private const val EXPECT_DELETED_MOB_MSG   =
    "Deleted 100 oldest mobile events. Total count before: $TOTAL_COUNT_EX"

private const val PREFIX_INVALID_CONFIG         = "invalid config:"
private const val ERR_API_URL_EMPTY             = "apiUrl is empty"
private const val ERR_INVALID_PROVIDERS_PREFIX  = "providerPriorityList contains invalid values:"
private const val FRAG_ALLOWED                  = "Allowed:"
private const val ERRORS_SEPARATOR              = "; "
private const val FRAG_MOBILE_INVALID_PREFIX    = "invalid mobile event payload: not all values are primitives."
private const val FRAG_EVENT_NAME_PREFIX        = "Event name: "

private const val MSG_NO_TRAILING_SPACE            = "Must not end with space"
private const val MSG_STARTS_WITH_INVALID_PREFIX   = "Should start with 'invalid config:'"
private const val MSG_CONTAINS_API_URL_EMPTY       = "Should contain 'apiUrl is empty'"
private const val MSG_CONTAINS_INVALID_BLOCK       = "Should contain invalid providers block"
private const val MSG_CONTAINS_ALLOWED_LIST        = "Should contain 'Allowed:'"
private const val MSG_ERRORS_ORDERED               = "Errors should be ordered"
private const val MSG_HAS_ERRORS_SEPARATOR         = "Should contain '; ' as errors separator"
private const val MSG_NO_ERRORS_APPENDED           = "No errors expected; only prefix with space"
private const val MSG_DELETED_MOBILE_EVENTS        = "deletedMobileEventsMsg must match expected format"
private const val MSG_MOBILE_EVENT_INVALID_PREFIX  = "mobileEventInvalid must contain the fixed reason prefix"
private const val MSG_MOBILE_EVENT_INVALID_NAME    = "mobileEventInvalid must include the event name"

/**
 * StringBuilderTest
 *
 * Positive scenarios:
 * - test_1: eventLogBuilder with non-null message returns "fn(): <msg>" (message appended).
 * - test_2: eventLogBuilder with null message returns "fn():" with no trailing space.
 * - test_3: eventLogBuilder with empty string returns "fn(): " with a single space after colon.
 * - test_7: invalidConfigMsg with valid configuration returns only the prefix "invalid config: " (no errors).
 * - test_8: deletedMobileEventsMsg returns the expected summary string with total count.
 * - test_9: mobileEventInvalid builds an error message that contains the event name and reason.
 *
 * Negative scenarios:
 * - test_4: invalidConfigMsg with empty apiUrl contains "apiUrl is empty".
 * - test_5: invalidConfigMsg with invalid Android providers contains invalid names and the "Allowed:" list.
 * - test_6: invalidConfigMsg with multiple errors concatenates them with "; " preserving order.
 */
class StringBuilderTest {

    /** - test_1: eventLogBuilder adds message when non-null message provided. */
    @Test
    fun `eventLogBuilder adds message when provided`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_PUSH_SUBSCRIBE, MSG_OK)
        assertEquals(EXPECT_LOG_OK, actual)
    }

    /** - test_2: eventLogBuilder omits trailing space when message is null. */
    @Test
    fun `eventLogBuilder omits trailing space when message is null`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_PUSH_SUBSCRIBE, null)
        assertEquals(EXPECT_LOG_NULL_MESSAGE, actual)
        assertFalse(MSG_NO_TRAILING_SPACE, actual.endsWith(" "))
    }

    /** - test_3: eventLogBuilder keeps a single space after colon when message is empty string. */
    @Test
    fun `eventLogBuilder keeps single space when message is empty string`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_SIMPLE, "")
        assertEquals(EXPECT_LOG_EMPTY_MESSAGE, actual)
    }

    /** - test_4: invalidConfigMsg returns apiUrl empty error when apiUrl is blank. */
    @Test
    fun `invalidConfigMsg returns apiUrl empty error only when apiUrl is empty`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns ""
        every { config.providerPriorityList } returns null
        val msg = ACStringBuilder.invalidConfigMsg(config)
        assertTrue(MSG_STARTS_WITH_INVALID_PREFIX, msg.startsWith(PREFIX_INVALID_CONFIG))
        assertTrue(MSG_CONTAINS_API_URL_EMPTY, msg.contains(ERR_API_URL_EMPTY))
    }

    /** - test_5: invalidConfigMsg lists invalid android providers and shows Allowed list. */
    @Test
    fun `invalidConfigMsg lists invalid android providers`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns URL_VALID
        every { config.providerPriorityList } returns listOf(PROV_INVALID_1, PROV_INVALID_2)
        val msg = ACStringBuilder.invalidConfigMsg(config)
        assertTrue(MSG_STARTS_WITH_INVALID_PREFIX, msg.startsWith(PREFIX_INVALID_CONFIG))
        assertTrue(MSG_CONTAINS_INVALID_BLOCK, msg.contains(ERR_INVALID_PROVIDERS_PREFIX))
        assertTrue(msg.contains(PROV_INVALID_1))
        assertTrue(msg.contains(PROV_INVALID_2))
        assertTrue(MSG_CONTAINS_ALLOWED_LIST, msg.contains(FRAG_ALLOWED))
    }

    /** - test_6: invalidConfigMsg concatenates multiple errors with "; " preserving order. */
    @Test
    fun `invalidConfigMsg concatenates multiple errors preserving order`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns ""
        every { config.providerPriorityList } returns listOf(PROV_INVALID_3)
        val msg = ACStringBuilder.invalidConfigMsg(config)
        val i1 = msg.indexOf(ERR_API_URL_EMPTY)
        val i2 = msg.indexOf(ERR_INVALID_PROVIDERS_PREFIX)
        assertTrue(MSG_CONTAINS_API_URL_EMPTY, i1 >= 0)
        assertTrue(MSG_CONTAINS_INVALID_BLOCK, i2 >= 0)
        assertTrue(MSG_ERRORS_ORDERED, i1 < i2)
        assertTrue(MSG_HAS_ERRORS_SEPARATOR, msg.contains(ERRORS_SEPARATOR))
    }

    /** - test_7: invalidConfigMsg returns only prefix when configuration is valid. */
    @Test
    fun `invalidConfigMsg returns empty error list for valid config`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns URL_VALID
        every { config.providerPriorityList } returns listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)
        val msg = ACStringBuilder.invalidConfigMsg(config)
        assertEquals(MSG_NO_ERRORS_APPENDED, "$PREFIX_INVALID_CONFIG ", msg)
    }

    /** - test_8: deletedMobileEventsMsg returns expected summary with total count. */
    @Test
    fun `deletedMobileEventsMsg returns expected summary`() {
        val msg = ACStringBuilder.deletedMobileEventsMsg(TOTAL_COUNT_EX)
        assertEquals(MSG_DELETED_MOBILE_EVENTS, EXPECT_DELETED_MOB_MSG, msg)
    }

    /** - test_9: mobileEventInvalid includes fixed reason and event name. */
    @Test
    fun `mobileEventInvalid includes reason and event name`() {
        val msg = ACStringBuilder.mobileEventPayloadInvalid(EVENT_NAME_EX)
        assertTrue(MSG_MOBILE_EVENT_INVALID_PREFIX, msg.contains(FRAG_MOBILE_INVALID_PREFIX))
        assertTrue(MSG_MOBILE_EVENT_INVALID_NAME, msg.contains("$FRAG_EVENT_NAME_PREFIX$EVENT_NAME_EX"))
    }
}