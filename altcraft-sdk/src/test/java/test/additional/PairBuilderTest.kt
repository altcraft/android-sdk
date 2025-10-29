@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.PairBuilder
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UPDATE_REQUEST
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.MobileEventRequestData
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.interfaces.RequestData
import com.altcraft.sdk.network.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.*
import org.junit.Test

/**
 * PairBuilderTest
 *
 * Positive scenarios:
 *  - test_1: SUBSCRIBE → success code 230; success message contains request name.
 *  - test_2: UPDATE → success code 231; success message contains request name.
 *  - test_3: UNSUSPEND → success code 232; success message contains request name.
 *  - test_4: STATUS → success code 233; success message contains request name.
 *  - test_5: PUSH_EVENT → success code 234; success message contains request name and event type.
 *  - test_6: MOBILE_EVENT → success code 235; success message contains request name and event name.
 *  - test_14: createSetTokenEventPair() returns non-negative code; message has provider and token.
 *
 * Negative scenarios:
 *  - test_7: SUBSCRIBE errors → 5xx→530, 4xx→430; message contains http/error fields.
 *  - test_8: UPDATE errors → 5xx→531, 4xx→431.
 *  - test_9: PUSH_EVENT errors → 5xx→534, 4xx→434; message includes event type.
 *  - test_10: MOBILE_EVENT errors → 5xx→535, 4xx→435; message includes event name.
 *  - test_11: UNSUSPEND error code fixed → 432.
 *  - test_12: STATUS error code fixed → 433.
 *  - test_13: UNKNOWN request → 5xx→539, else→439; message starts with request name.
 */
class PairBuilderTest {

    private companion object {
        private const val URL_EXAMPLE = "https://example.com"
        private const val UID_123 = "uid_123"
        private const val AUTH_BEARER = "Bearer token"
        private const val MATCHING_PUSH = "push"
        private const val UNKNOWN_REQUEST = "unknown request"
        private const val TOKEN_SAMPLE = "tkn123"
        private const val MOBILE_EVENT_NAME = "evt_name"
        private const val MOBILE_EVENT_SID = "sid123"

        private const val FRAG_HTTP_CODE = "http code: "
        private const val FRAG_ERROR = "error: "
        private const val FRAG_ERROR_TEXT = "errorText: "
        private const val FRAG_TYPE = "type: "
        private const val FRAG_NAME = "name: "

        private const val MSG_SUCCESS_HAS_REQ = "Success message must contain request name"
        private const val MSG_ERROR_HAS_HTTP = "Error message must contain http code"
        private const val MSG_ERROR_HAS_ERROR = "Error message must contain response error"
        private const val MSG_ERROR_HAS_ERR_TEXT = "Error message must contain response errorText"
        private const val MSG_SUCCESS_PUSH_EVENT_HAS_TYPE = "Success message must contain event type"
        private const val MSG_ERROR_PUSH_EVENT_HAS_TYPE = "Error message must contain event type"
        private const val MSG_SUCCESS_MOBILE_EVENT_HAS_NAME = "Success message must contain mobile event name"
        private const val MSG_ERROR_MOBILE_EVENT_HAS_NAME = "Error message must contain mobile event name"
        private const val MSG_UNKNOWN_PREFIX = "Unknown request message must start with request name"
        private const val MSG_PAIR_CODE_NON_NEGATIVE = "Event code must be non-negative"
        private const val MSG_PAIR_MSG_HAS_PROVIDER = "Message must contain provider name"
        private const val MSG_PAIR_MSG_HAS_TOKEN = "Message must contain token"
    }

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

    private fun mobileEventRequest(name: String = MOBILE_EVENT_NAME) = MobileEventRequestData(
        url = URL_EXAMPLE,
        authHeader = AUTH_BEARER,
        sid = MOBILE_EVENT_SID,
        name = name
    )

    private fun genericRequest(): RequestData = mockk(relaxed = true)

    /** - test_1: SUBSCRIBE → success code 230; success message contains request name. */
    @Test
    fun test_1_success_SUBSCRIBE_230() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns SUBSCRIBE_REQUEST
        val (errorPair, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())
        assertEquals(230, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(SUBSCRIBE_REQUEST))
        assertTrue(MSG_ERROR_HAS_HTTP, errorPair.second.contains("${FRAG_HTTP_CODE}200"))
    }

    /** - test_2: UPDATE → success code 231; success message contains request name. */
    @Test
    fun test_2_success_UPDATE_231() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UPDATE_REQUEST
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())
        assertEquals(231, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UPDATE_REQUEST))
    }

    /** - test_3: UNSUSPEND → success code 232; success message contains request name. */
    @Test
    fun test_3_success_UNSUSPEND_232() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNSUSPEND_REQUEST
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())
        assertEquals(232, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UNSUSPEND_REQUEST))
    }

    /** - test_4: STATUS → success code 233; success message contains request name. */
    @Test
    fun test_4_success_STATUS_233() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns STATUS_REQUEST
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), genericRequest())
        assertEquals(233, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(STATUS_REQUEST))
    }

    /** - test_5: PUSH_EVENT → success code 234; message contains request name and event type. */
    @Test
    fun test_5_success_PUSH_EVENT_234_containsType() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns PUSH_EVENT_REQUEST
        val request = pushEventRequest(type = OPEN)
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)
        assertEquals(234, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(PUSH_EVENT_REQUEST))
        assertTrue(MSG_SUCCESS_PUSH_EVENT_HAS_TYPE, successPair.second.contains("${FRAG_TYPE}${OPEN}"))
    }

    /** - test_6: MOBILE_EVENT → success code 235; message contains request name and event name. */
    @Test
    fun test_6_success_MOBILE_EVENT_235_containsName() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns MOBILE_EVENT_REQUEST
        val request = mobileEventRequest()
        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)
        assertEquals(235, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(MOBILE_EVENT_REQUEST))
        assertTrue(MSG_SUCCESS_MOBILE_EVENT_HAS_NAME, successPair.second.contains("${FRAG_NAME}${MOBILE_EVENT_NAME}"))
    }

    /** - test_7: SUBSCRIBE errors → 5xx→530, 4xx→430; message contains http/error fields. */
    @Test
    fun test_7_error_SUBSCRIBE_5xx_530_4xx_430() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns SUBSCRIBE_REQUEST
        val (err5xx, _) = PairBuilder.getRequestMessages(502, response(123, "boom"), genericRequest())
        assertEquals(530, err5xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err5xx.second.contains("${FRAG_HTTP_CODE}502"))
        assertTrue(MSG_ERROR_HAS_ERROR, err5xx.second.contains("${FRAG_ERROR}123"))
        assertTrue(MSG_ERROR_HAS_ERR_TEXT, err5xx.second.contains("${FRAG_ERROR_TEXT}boom"))
        val (err4xx, _) = PairBuilder.getRequestMessages(400, response(400, "bad"), genericRequest())
        assertEquals(430, err4xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err4xx.second.contains("${FRAG_HTTP_CODE}400"))
    }

    /** - test_8: UPDATE errors → 5xx→531, 4xx→431. */
    @Test
    fun test_8_error_UPDATE_5xx_531_4xx_431() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UPDATE_REQUEST
        val (err5xx, _) = PairBuilder.getRequestMessages(500, response(1, "srv"), genericRequest())
        assertEquals(531, err5xx.first)
        val (err4xx, _) = PairBuilder.getRequestMessages(404, response(404, "nf"), genericRequest())
        assertEquals(431, err4xx.first)
    }

    /** - test_9: PUSH_EVENT errors → 5xx→534, 4xx→434; message includes event type. */
    @Test
    fun test_9_error_PUSH_EVENT_5xx_534_4xx_434_containsType() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns PUSH_EVENT_REQUEST
        val req = pushEventRequest(type = DELIVERY)
        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(534, err5xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err5xx.second.contains("${FRAG_TYPE}${DELIVERY}"))
        val (err4xx, _) = PairBuilder.getRequestMessages(401, response(16, "unauth"), req)
        assertEquals(434, err4xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err4xx.second.contains("${FRAG_TYPE}${DELIVERY}"))
    }

    /** - test_10: MOBILE_EVENT errors → 5xx→535, 4xx→435; message includes event name. */
    @Test
    fun test_10_error_MOBILE_EVENT_5xx_535_4xx_435_containsName() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns MOBILE_EVENT_REQUEST
        val req = mobileEventRequest(MOBILE_EVENT_NAME)
        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(535, err5xx.first)
        assertTrue(MSG_ERROR_MOBILE_EVENT_HAS_NAME, err5xx.second.contains("${FRAG_NAME}${MOBILE_EVENT_NAME}"))
        val (err4xx, _) = PairBuilder.getRequestMessages(409, response(9, "cl"), req)
        assertEquals(435, err4xx.first)
        assertTrue(MSG_ERROR_MOBILE_EVENT_HAS_NAME, err4xx.second.contains("${FRAG_NAME}${MOBILE_EVENT_NAME}"))
    }

    /** - test_11: UNSUSPEND error code fixed → 432. */
    @Test
    fun test_11_error_UNSUSPEND_fixed_432() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNSUSPEND_REQUEST
        val (err, _) = PairBuilder.getRequestMessages(409, response(10, "conflict"), genericRequest())
        assertEquals(432, err.first)
        assertTrue(err.second.contains("unsuspend"))
    }

    /** - test_12: STATUS error code fixed → 433. */
    @Test
    fun test_12_error_STATUS_fixed_433() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns STATUS_REQUEST
        val (err, _) = PairBuilder.getRequestMessages(400, response(11, "bad"), genericRequest())
        assertEquals(433, err.first)
        assertTrue(err.second.contains("status"))
    }

    /** - test_13: UNKNOWN request → 5xx→539, else→439; message starts with request name. */
    @Test
    fun test_13_error_UNKNOWN_5xx_539_else_439_prefix() {
        mockkObject(Response)
        every { Response.getRequestName(any()) } returns UNKNOWN_REQUEST
        val (err5xx, _) = PairBuilder.getRequestMessages(599, response(9, "x"), genericRequest())
        assertEquals(539, err5xx.first)
        assertTrue(MSG_UNKNOWN_PREFIX, err5xx.second.startsWith(UNKNOWN_REQUEST))
        val (err4xx, _) = PairBuilder.getRequestMessages(418, response(9, "teapot"), genericRequest())
        assertEquals(439, err4xx.first)
        assertTrue(MSG_UNKNOWN_PREFIX, err4xx.second.startsWith(UNKNOWN_REQUEST))
    }

    /** - test_14: createSetTokenEventPair() returns non-negative code; message has provider and token. */
    @Test
    fun test_14_createSetTokenEventPair_containsProviderAndToken() {
        val data = TokenData(provider = FCM_PROVIDER, token = TOKEN_SAMPLE)
        val (code, msg) = PairBuilder.createSetTokenEventPair(data)
        assertTrue(MSG_PAIR_CODE_NON_NEGATIVE, code >= 0)
        assertTrue(MSG_PAIR_MSG_HAS_PROVIDER, msg.contains(FCM_PROVIDER))
        assertTrue(MSG_PAIR_MSG_HAS_TOKEN, msg.contains(TOKEN_SAMPLE))
        assertTrue(msg.contains("token:"))
    }
}