@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.PairBuilder
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UPDATE_REQUEST
import com.altcraft.sdk.interfaces.RequestData
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.network.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PairBuilder.
 *
 * Verifies:
 * - getRequestMessages(): success code mapping per request type (SUBSCRIBE→230, UPDATE→231,
 *   PUSH_EVENT→232, UN_SUSPEND→233, STATUS→234) and error code mapping for 4xx/5xx.
 * - Message contents include request name, HTTP code, error fields, and for push events — event type.
 * - Unknown request mapping (5xx→539, else→439) and message prefix.
 * - createSetTokenEventPair(): returns non-negative code and message includes provider/token.
 */

// ---------- Test data & constants ----------
private const val URL_EXAMPLE = "https://example.com"
private const val UID_123 = "uid_123"
private const val AUTH_BEARER = "Bearer token"
private const val MATCHING_PUSH = "push"
private const val UNKNOWN_REQUEST = "unknown request"
private const val TOKEN_SAMPLE = "tkn123"

// Message fragments
private const val FRAG_HTTP_CODE = "http code: "
private const val FRAG_ERROR = "error: "
private const val FRAG_ERROR_TEXT = "errorText: "
private const val FRAG_TYPE = "type: "
private const val FRAG_TOKEN = "token:"

// Assertion messages
private const val MSG_SUCCESS_HAS_REQ = "Success message must contain request name"
private const val MSG_ERROR_HAS_HTTP = "Error message must contain http code"
private const val MSG_ERROR_HAS_ERROR = "Error message must contain response error"
private const val MSG_ERROR_HAS_ERR_TEXT = "Error message must contain response errorText"
private const val MSG_SUCCESS_PUSH_EVENT_HAS_TYPE = "Success message must contain event type"
private const val MSG_ERROR_PUSH_EVENT_HAS_TYPE = "Error message must contain event type"
private const val MSG_UNKNOWN_PREFIX = "Unknown request message must start with request name"
private const val MSG_PAIR_CODE_NON_NEGATIVE = "Event code must be non-negative"
private const val MSG_PAIR_MSG_HAS_PROVIDER = "Message must contain provider name"
private const val MSG_PAIR_MSG_HAS_TOKEN = "Message must contain token"
private const val MSG_SUBSCRIBE_ERR_5XX = "SUBSCRIBE_REQUEST: 5xx should map to 530"
private const val MSG_SUBSCRIBE_ERR_4XX = "SUBSCRIBE_REQUEST: 4xx should map to 430"
private const val MSG_UPDATE_ERR_5XX = "UPDATE_REQUEST: 5xx should map to 531"
private const val MSG_UPDATE_ERR_4XX = "UPDATE_REQUEST: 4xx should map to 431"
private const val MSG_PUSH_EVENT_ERR_5XX = "PUSH_EVENT_REQUEST: 5xx should map to 532"
private const val MSG_PUSH_EVENT_ERR_4XX = "PUSH_EVENT_REQUEST: 4xx should map to 432"
private const val MSG_UN_SUSPEND_ERR = "UN_SUSPEND_REQUEST must map to 433"
private const val MSG_STATUS_ERR = "STATUS_REQUEST must map to 434"
private const val MSG_UNKNOWN_5XX = "UNKNOWN: 5xx should map to 539"
private const val MSG_UNKNOWN_4XX = "UNKNOWN: else should map to 439"

/**
 * PairBuilderTest
 *
 * Positive scenarios:
 *  - test_1: SUBSCRIBE_REQUEST success → getRequestMessages returns success code 230 and
 *            success message contains request name; error pair echoes HTTP code.
 *  - test_2: UPDATE_REQUEST success → success code 231; message contains request name.
 *  - test_3: UN_SUSPEND_REQUEST success → success code 233; message contains request name.
 *  - test_4: STATUS_REQUEST success → success code 234; message contains request name.
 *  - test_5: PUSH_EVENT_REQUEST success → success code 232; message contains request name
 *            and includes event type (e.g., "open").
 *  - test_12: createSetTokenEventPair → returns non-negative code; message includes provider
 *             and token, and contains "token:" fragment.
 *
 * Negative scenarios:
 *  - test_6: SUBSCRIBE_REQUEST error mapping → 5xx→530, 4xx→430; error message includes
 *            "http code:", "error:", "errorText:" fragments.
 *  - test_7: UPDATE_REQUEST error mapping → 5xx→531, 4xx→431.
 *  - test_8: PUSH_EVENT_REQUEST error mapping → 5xx→532, 4xx→432; error message includes
 *            event type ("type: <value>").
 *  - test_9: UN_SUSPEND_REQUEST client error (e.g., 409) → maps to 433; message mentions "unsuspend".
 *  - test_10: STATUS_REQUEST client error (e.g., 400) → maps to 434; message mentions "status".
 *  - test_11: Unknown request mapping → 5xx→539, else→439; error message starts with the
 *             unknown request name as prefix.
 *
 * Notes:
 *  - Response.getRequestName(...) is mocked per test to emulate request type.
 *  - Constants for fragments/messages are defined above for consistency across assertions.
 */
class PairBuilderTest {

    // ---------- Helpers ----------
    private fun response(error: Int?, text: String?) =
        DataClasses.Response(error = error, errorText = text, profile = null)

    private fun pushEventRequest(type: String = DELIVERY) = PushEventRequestData(
        url = URL_EXAMPLE,
        time = 0L,
        type = type,
        uid = UID_123,
        authHeader = AUTH_BEARER,
        matchingMode = MATCHING_PUSH
    )

    private fun genericRequest(): RequestData = mockk(relaxed = true)

    // ---------- Success mappings ----------

    /**
     * Success: SUBSCRIBE_REQUEST → code 230; message contains request name; errorPair echoes HTTP code.
     */
    @Test
    fun `success for SUBSCRIBE_REQUEST code 230`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns SUBSCRIBE_REQUEST

        val (errorPair, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())

        assertEquals(230, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(SUBSCRIBE_REQUEST))
        assertTrue(MSG_ERROR_HAS_HTTP, errorPair.second.contains("${FRAG_HTTP_CODE}200"))
    }

    /**
     * Success: UPDATE_REQUEST → code 231; message contains request name.
     */
    @Test
    fun `success for UPDATE_REQUEST code 231`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UPDATE_REQUEST

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())

        assertEquals(231, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UPDATE_REQUEST))
    }

    /**
     * Success: UN_SUSPEND_REQUEST → code 233; message contains request name.
     */
    @Test
    fun `success for UNSUSPEND_REQUEST code 233`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNSUSPEND_REQUEST

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())

        assertEquals(233, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UNSUSPEND_REQUEST))
    }

    /**
     * Success: STATUS_REQUEST → code 234; message contains request name.
     */
    @Test
    fun `success for STATUS_REQUEST code 234`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns STATUS_REQUEST

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())

        assertEquals(234, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(STATUS_REQUEST))
    }

    /**
     * Success: PUSH_EVENT_REQUEST → code 232; message contains request name and event type.
     */
    @Test
    fun `success for PUSH_EVENT_REQUEST code 232 and message contains event type`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns PUSH_EVENT_REQUEST

        val request = pushEventRequest(type = OPEN)
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)

        assertEquals(232, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(PUSH_EVENT_REQUEST))
        assertTrue(MSG_SUCCESS_PUSH_EVENT_HAS_TYPE, successPair.second.contains(OPEN))
    }

    // ---------- Error mappings ----------

    /**
     * Error mapping for SUBSCRIBE_REQUEST: 5xx → 530, 4xx → 430; message contains http/error fields.
     */
    @Test
    fun `error mapping SUBSCRIBE_REQUEST  5xx to 530, 4xx to 430`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns SUBSCRIBE_REQUEST

        val (err5xx, _) = PairBuilder.getRequestMessages(
            502,
            response(123, "boom"),
            genericRequest()
        )
        assertEquals(MSG_SUBSCRIBE_ERR_5XX, 530, err5xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err5xx.second.contains("${FRAG_HTTP_CODE}502"))
        assertTrue(MSG_ERROR_HAS_ERROR, err5xx.second.contains("${FRAG_ERROR}123"))
        assertTrue(MSG_ERROR_HAS_ERR_TEXT, err5xx.second.contains("${FRAG_ERROR_TEXT}boom"))

        val (err4xx, _) = PairBuilder.getRequestMessages(
            400,
            response(400, "bad"),
            genericRequest()
        )
        assertEquals(MSG_SUBSCRIBE_ERR_4XX, 430, err4xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err4xx.second.contains("${FRAG_HTTP_CODE}400"))
    }

    /**
     * Error mapping for UPDATE_REQUEST: 5xx → 531, 4xx → 431.
     */
    @Test
    fun `error mapping UPDATE_REQUEST  5xx to 531, 4xx to 431`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UPDATE_REQUEST

        val (err5xx, _) = PairBuilder.getRequestMessages(500, response(1, "srv"), genericRequest())
        assertEquals(MSG_UPDATE_ERR_5XX, 531, err5xx.first)

        val (err4xx, _) = PairBuilder.getRequestMessages(404, response(404, "nf"), genericRequest())
        assertEquals(MSG_UPDATE_ERR_4XX, 431, err4xx.first)
    }

    /**
     * Error mapping for PUSH_EVENT_REQUEST: 5xx → 532, 4xx → 432; message includes event type.
     */
    @Test
    fun `error mapping PUSH_EVENT_REQUEST 5xx to 532, 4xx to 432 and includes event type`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns PUSH_EVENT_REQUEST

        val req = pushEventRequest(type = DELIVERY)

        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(MSG_PUSH_EVENT_ERR_5XX, 532, err5xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err5xx.second.contains("${FRAG_TYPE}$DELIVERY"))

        val (err4xx, _) = PairBuilder.getRequestMessages(401, response(16, "unauth"), req)
        assertEquals(MSG_PUSH_EVENT_ERR_4XX, 432, err4xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err4xx.second.contains("${FRAG_TYPE}$DELIVERY"))
    }

    /**
     * Error mapping for UN_SUSPEND_REQUEST: maps to 433 for client errors (e.g., 409).
     */
    @Test
    fun `error mapping UNSUSPEND_REQUEST 433`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNSUSPEND_REQUEST

        val (err, _) = PairBuilder.getRequestMessages(
            409,
            response(10, "conflict"),
            genericRequest()
        )
        assertEquals(MSG_UN_SUSPEND_ERR, 433, err.first)
        assertTrue(err.second.contains("unsuspend"))
    }

    /**
     * Error mapping for STATUS_REQUEST: maps to 434 for client errors (e.g., 400).
     */
    @Test
    fun `error mapping STATUS_REQUEST 434`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns STATUS_REQUEST

        val (err, _) = PairBuilder.getRequestMessages(400, response(11, "bad"), genericRequest())
        assertEquals(MSG_STATUS_ERR, 434, err.first)
        assertTrue(err.second.contains("status"))
    }

    /**
     * Error mapping for unknown requests: 5xx → 539, else → 439 (prefix starts with request name).
     */
    @Test
    fun `error mapping UNKNOWN  539 for 5xx else 439`() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNKNOWN_REQUEST

        val (err5xx, _) = PairBuilder.getRequestMessages(599, response(9, "x"), genericRequest())
        assertEquals(MSG_UNKNOWN_5XX, 539, err5xx.first)
        assertTrue(MSG_UNKNOWN_PREFIX, err5xx.second.startsWith(UNKNOWN_REQUEST))

        val (err4xx, _) = PairBuilder.getRequestMessages(
            418,
            response(9, "teapot"),
            genericRequest()
        )
        assertEquals(MSG_UNKNOWN_4XX, 439, err4xx.first)
        assertTrue(MSG_UNKNOWN_PREFIX, err4xx.second.startsWith(UNKNOWN_REQUEST))
    }

    /**
     * createSetTokenEventPair(): returns non-negative code; message contains provider and token.
     */
    @Test
    fun `createSetTokenEventPair returns code and readable message with provider and token`() {
        val data = TokenData(provider = FCM_PROVIDER, token = TOKEN_SAMPLE)
        val (code, msg) = PairBuilder.createSetTokenEventPair(data)

        assertTrue(MSG_PAIR_CODE_NON_NEGATIVE, code >= 0)
        assertTrue(MSG_PAIR_MSG_HAS_PROVIDER, msg.contains(FCM_PROVIDER))
        assertTrue(MSG_PAIR_MSG_HAS_TOKEN, msg.contains(TOKEN_SAMPLE))
        assertTrue(msg.contains(FRAG_TOKEN))
    }
}
