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

/**
 * StringBuilderTest
 *
 * Positive scenarios:
 *  - test_1: eventLogBuilder with non-null message → returns "fn(): <msg>" (message is appended).
 *  - test_2: eventLogBuilder with null message → returns "fn():" with NO trailing space.
 *  - test_3: eventLogBuilder with empty string → returns "fn(): " with a single space after colon.
 *  - test_7: invalidConfigMsg with valid configuration → returns only the prefix "invalid config: "
 *            (no errors appended).
 *
 * Negative scenarios:
 *  - test_4: invalidConfigMsg with empty apiUrl → message contains "apiUrl is empty".
 *  - test_5: invalidConfigMsg with invalid Android providers → message contains the invalid
 *            provider names and the "Allowed:" list.
 *  - test_6: invalidConfigMsg with multiple errors → errors are concatenated with "; " and
 *            ordering is preserved: "apiUrl is empty" appears before invalid providers block.
 *
 * Notes:
 *  - Valid providers used in tests: android-firebase, android-huawei, android-rustore.
 *  - Assertion messages and common fragments are defined as constants for consistency.
 */

// Alias to avoid clash with java.lang.StringBuilder
private typealias ACStringBuilder = com.altcraft.sdk.additional.StringBuilder

// ---------- Test inputs ----------
private const val FN_PUSH_SUBSCRIBE = "pushSubscribe"
private const val FN_SIMPLE        = "fn"
private const val MSG_OK           = "ok"
private const val URL_VALID        = "https://pxl.altcraft.com"
private const val PROV_INVALID_1   = "android-unknown"
private const val PROV_INVALID_2   = "android-bad"
private const val PROV_INVALID_3   = "android-wrong"

// ---------- Expected full strings ----------
private const val EXPECT_LOG_OK            = "$FN_PUSH_SUBSCRIBE(): $MSG_OK"
private const val EXPECT_LOG_NULL_MESSAGE  = "$FN_PUSH_SUBSCRIBE():"
private const val EXPECT_LOG_EMPTY_MESSAGE = "$FN_SIMPLE(): "

// ---------- Expected fragments / formatting ----------
private const val PREFIX_INVALID_CONFIG         = "invalid config:"
private const val ERR_API_URL_EMPTY             = "apiUrl is empty"
private const val ERR_INVALID_PROVIDERS_PREFIX  = "providerPriorityList contains invalid values:"
private const val FRAG_ALLOWED                  = "Allowed:"
private const val ERRORS_SEPARATOR              = "; "

// ---------- Assertion messages ----------
private const val MSG_NO_TRAILING_SPACE            = "Must not end with space"
private const val MSG_STARTS_WITH_INVALID_PREFIX   = "Should start with 'invalid config:'"
private const val MSG_CONTAINS_API_URL_EMPTY       = "Should contain 'apiUrl is empty'"
private const val MSG_CONTAINS_INVALID_BLOCK       = "Should contain invalid providers block"
private const val MSG_CONTAINS_ALLOWED_LIST        = "Should contain 'Allowed:'"
private const val MSG_ERRORS_ORDERED               = "Errors should be ordered"
private const val MSG_HAS_ERRORS_SEPARATOR         = "Should contain '; ' as errors separator"
private const val MSG_NO_ERRORS_APPENDED           = "No errors expected; only prefix with space"

class StringBuilderTest {

    // ---------------------------
    // eventLogBuilder
    // ---------------------------

    @Test
    // Appends message text when provided (non-null).
    fun `eventLogBuilder adds message when provided`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_PUSH_SUBSCRIBE, MSG_OK)
        assertEquals(EXPECT_LOG_OK, actual)
    }

    @Test
    // Omits any trailing space when message is null.
    fun `eventLogBuilder omits trailing space when message is null`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_PUSH_SUBSCRIBE, null)
        assertEquals(EXPECT_LOG_NULL_MESSAGE, actual)
        assertFalse(MSG_NO_TRAILING_SPACE, actual.endsWith(" "))
    }

    @Test
    // Keeps a single space after colon when message is an empty string.
    fun `eventLogBuilder keeps single space when message is empty string`() {
        val actual = ACStringBuilder.eventLogBuilder(FN_SIMPLE, "")
        assertEquals(EXPECT_LOG_EMPTY_MESSAGE, actual)
    }

    // ---------------------------
    // invalidConfigMsg
    // ---------------------------

    @Test
    // Returns only 'apiUrl is empty' error when apiUrl is blank and no other issues.
    fun `invalidConfigMsg returns apiUrl empty error only when apiUrl is empty`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns ""
        every { config.providerPriorityList } returns null

        val msg = ACStringBuilder.invalidConfigMsg(config)

        assertTrue(MSG_STARTS_WITH_INVALID_PREFIX, msg.startsWith(PREFIX_INVALID_CONFIG))
        assertTrue(MSG_CONTAINS_API_URL_EMPTY, msg.contains(ERR_API_URL_EMPTY))
    }

    @Test
    // Reports invalid providers when list contains unknown Android providers.
    fun `invalidConfigMsg lists invalid android providers`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns URL_VALID
        // valid: android-firebase, android-huawei, android-rustore
        every { config.providerPriorityList } returns listOf(PROV_INVALID_1, PROV_INVALID_2)

        val msg = ACStringBuilder.invalidConfigMsg(config)

        assertTrue(MSG_STARTS_WITH_INVALID_PREFIX, msg.startsWith(PREFIX_INVALID_CONFIG))
        assertTrue(MSG_CONTAINS_INVALID_BLOCK, msg.contains(ERR_INVALID_PROVIDERS_PREFIX))
        assertTrue(msg.contains(PROV_INVALID_1))
        assertTrue(msg.contains(PROV_INVALID_2))
        assertTrue(MSG_CONTAINS_ALLOWED_LIST, msg.contains(FRAG_ALLOWED))
    }

    @Test
    // Concatenates multiple errors with '; ' and keeps order (apiUrl first, then providers).
    fun `invalidConfigMsg concatenates multiple errors preserving order`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns "" // first error
        every { config.providerPriorityList } returns listOf(PROV_INVALID_3) // second error

        val msg = ACStringBuilder.invalidConfigMsg(config)

        val i1 = msg.indexOf(ERR_API_URL_EMPTY)
        val i2 = msg.indexOf(ERR_INVALID_PROVIDERS_PREFIX)
        assertTrue(MSG_CONTAINS_API_URL_EMPTY, i1 >= 0)
        assertTrue(MSG_CONTAINS_INVALID_BLOCK, i2 >= 0)
        assertTrue(MSG_ERRORS_ORDERED, i1 < i2)
        assertTrue(MSG_HAS_ERRORS_SEPARATOR, msg.contains(ERRORS_SEPARATOR))
    }

    @Test
    // Returns only prefix when config is valid (no errors).
    fun `invalidConfigMsg returns empty error list for valid config`() {
        val config = mockk<ConfigurationEntity>()
        every { config.apiUrl } returns URL_VALID
        every { config.providerPriorityList } returns listOf(
            FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER
        )

        val msg = ACStringBuilder.invalidConfigMsg(config)
        assertEquals(MSG_NO_ERRORS_APPENDED, "$PREFIX_INVALID_CONFIG ", msg) // no errors appended
    }
}
