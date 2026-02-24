@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.PairBuilder
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.MOBILE_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.PROFILE_UPDATE_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.SUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UNSUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_REQUEST
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.MobileEventRequestData
import com.altcraft.sdk.data.DataClasses.ProfileUpdateRequestData
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.DataClasses.RequestData
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.network.Request.RequestName
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PairBuilderTest
 *
 * Positive scenarios:
 * - test_1: SUBSCRIBE returns success code 230 and message contains request name.
 * - test_2: SUSPEND returns success code 231 and message contains request name.
 * - test_3: UNSUBSCRIBED returns success code 232 and message contains request name.
 * - test_4: TOKEN_UPDATE returns success code 233 and message contains request name.
 * - test_5: UNSUSPEND returns success code 234 and message contains request name.
 * - test_6: STATUS returns success code 235 and message contains request name.
 * - test_7: PUSH_EVENT returns success code 236 and message contains event type.
 * - test_8: MOBILE_EVENT returns success code 237 and message contains event name.
 * - test_9: PROFILE_UPDATE returns success code 238 and message contains request name.
 * - test_17: createSetTokenEventPair returns non-negative code and message contains provider and token.
 *
 * Negative scenarios:
 * - test_10: SUBSCRIBE error maps to 530 for 5xx and 430 for non-5xx.
 * - test_11: TOKEN_UPDATE error maps to 533 for 5xx and 433 for non-5xx.
 * - test_12: PUSH_EVENT error maps to 536 for 5xx and 436 for non-5xx and contains event type.
 * - test_13: MOBILE_EVENT error maps to 537 for 5xx and 437 for non-5xx and contains event name.
 * - test_14: PROFILE_UPDATE error maps to 538 for 5xx and 438 for non-5xx.
 * - test_15: UNSUSPEND error maps to fixed code 434.
 * - test_16: STATUS error maps to fixed code 435.
 */
class PairBuilderTest {

    private companion object {
        private const val URL_EXAMPLE = "https://example.com"
        private const val UID_123 = "uid_123"
        private const val AUTH_BEARER = "Bearer token"
        private const val MATCHING_PUSH = "push"
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
        private const val MSG_SUCCESS_PUSH_EVENT_HAS_TYPE =
            "Success message must contain event type"
        private const val MSG_ERROR_PUSH_EVENT_HAS_TYPE = "Error message must contain event type"
        private const val MSG_SUCCESS_MOBILE_EVENT_HAS_NAME =
            "Success message must contain mobile event name"
        private const val MSG_ERROR_MOBILE_EVENT_HAS_NAME =
            "Error message must contain mobile event name"
        private const val MSG_PAIR_CODE_NON_NEGATIVE = "Event code must be non-negative"
        private const val MSG_PAIR_MSG_HAS_PROVIDER = "Message must contain provider name"
        private const val MSG_PAIR_MSG_HAS_TOKEN = "Message must contain token"
    }

    private fun response(error: Int?, text: String?) =
        DataClasses.Response(error = error, errorText = text, profile = null)

    private fun requestWithName(name: RequestName): RequestData =
        mockk(relaxed = true) {
            every { requestName } returns name
        }

    private fun pushEventRequest(type: String = DELIVERY) = PushEventRequestData(
        url = URL_EXAMPLE,
        requestId = "req_push_$type",
        time = 0L,
        type = type,
        uid = UID_123,
        authHeader = AUTH_BEARER,
        matchingMode = MATCHING_PUSH
    )

    private fun mobileEventRequest() = MobileEventRequestData(
        url = URL_EXAMPLE,
        requestId = "req_mobile_$MOBILE_EVENT_NAME",
        sid = MOBILE_EVENT_SID,
        name = MOBILE_EVENT_NAME,
        authHeader = AUTH_BEARER
    )

    private fun profileUpdateRequest() = ProfileUpdateRequestData(
        url = URL_EXAMPLE,
        requestId = "req_profile_update",
        authHeader = AUTH_BEARER,
        profileFields = JsonNull,
        skipTriggers = null
    )

    /**
     * test_1: SUBSCRIBE → success code 230; success message contains request name.
     */
    @Test
    fun test_1_success_SUBSCRIBE_230() {
        val req = requestWithName(RequestName.SUBSCRIBE_REQUEST)

        val (errorPair, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(230, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(SUBSCRIBE_REQUEST))
        assertTrue(MSG_ERROR_HAS_HTTP, errorPair.second.contains("${FRAG_HTTP_CODE}200"))
    }

    /**
     * test_2: SUSPEND → success code 231; success message contains request name.
     */
    @Test
    fun test_2_success_SUSPEND_231() {
        val req = requestWithName(RequestName.SUSPEND_REQUEST)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(231, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(SUSPEND_REQUEST))
    }

    /**
     * test_3: UNSUBSCRIBED → success code 232; success message contains request name.
     */
    @Test
    fun test_3_success_UNSUBSCRIBED_232() {
        val req = requestWithName(RequestName.UNSUBSCRIBE_REQUEST)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(232, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UNSUBSCRIBE_REQUEST))
    }

    /**
     * test_4: TOKEN_UPDATE → success code 233; success message contains request name.
     */
    @Test
    fun test_4_success_TOKEN_UPDATE_233() {
        val req = requestWithName(RequestName.TOKEN_UPDATE_REQUEST)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(233, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(TOKEN_UPDATE_REQUEST))
    }

    /**
     * test_5: UNSUSPEND → success code 234; success message contains request name.
     */
    @Test
    fun test_5_success_UNSUSPEND_234() {
        val req = requestWithName(RequestName.UNSUSPEND_REQUEST)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(234, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(UNSUSPEND_REQUEST))
    }

    /**
     * test_6: STATUS → success code 235; success message contains request name.
     */
    @Test
    fun test_6_success_STATUS_235() {
        val req = requestWithName(RequestName.STATUS_REQUEST)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), req)

        assertEquals(235, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(STATUS_REQUEST))
    }

    /**
     * test_7: PUSH_EVENT → success code 236; message contains request name and event type.
     */
    @Test
    fun test_7_success_PUSH_EVENT_236_containsType() {
        val request = pushEventRequest(type = OPEN)

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)

        assertEquals(236, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(PUSH_EVENT_REQUEST))
        assertTrue(MSG_SUCCESS_PUSH_EVENT_HAS_TYPE, successPair.second.contains(OPEN))
    }

    /**
     * test_8: MOBILE_EVENT → success code 237; message contains request name and event name.
     */
    @Test
    fun test_8_success_MOBILE_EVENT_237_containsName() {
        val request = mobileEventRequest()

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)

        assertEquals(237, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(MOBILE_EVENT_REQUEST))
        assertTrue(MSG_SUCCESS_MOBILE_EVENT_HAS_NAME, successPair.second.contains(MOBILE_EVENT_NAME))
    }

    /**
     * test_9: PROFILE_UPDATE → success code 238; message contains request name.
     */
    @Test
    fun test_9_success_PROFILE_UPDATE_238() {
        val request = profileUpdateRequest()

        val (_, successPair) =
            PairBuilder.getRequestMessages(200, response(0, ""), request)

        assertEquals(238, successPair.first)
        assertTrue(MSG_SUCCESS_HAS_REQ, successPair.second.contains(PROFILE_UPDATE_REQUEST))
    }

    /**
     * test_10: SUBSCRIBE errors → 5xx→530, else→430; message contains http/error fields.
     */
    @Test
    fun test_10_error_SUBSCRIBE_5xx_530_else_430() {
        val req = requestWithName(RequestName.SUBSCRIBE_REQUEST)

        val (err5xx, _) = PairBuilder.getRequestMessages(502, response(123, "boom"), req)
        assertEquals(530, err5xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err5xx.second.contains("${FRAG_HTTP_CODE}502"))
        assertTrue(MSG_ERROR_HAS_ERROR, err5xx.second.contains("${FRAG_ERROR}123"))
        assertTrue(MSG_ERROR_HAS_ERR_TEXT, err5xx.second.contains("${FRAG_ERROR_TEXT}boom"))

        val (err4xx, _) = PairBuilder.getRequestMessages(400, response(400, "bad"), req)
        assertEquals(430, err4xx.first)
        assertTrue(MSG_ERROR_HAS_HTTP, err4xx.second.contains("${FRAG_HTTP_CODE}400"))
    }

    /**
     * test_11: TOKEN_UPDATE errors → 5xx→533, else→433.
     */
    @Test
    fun test_11_error_TOKEN_UPDATE_5xx_533_else_433() {
        val req = requestWithName(RequestName.TOKEN_UPDATE_REQUEST)

        val (err5xx, _) = PairBuilder.getRequestMessages(500, response(1, "srv"), req)
        assertEquals(533, err5xx.first)

        val (err4xx, _) = PairBuilder.getRequestMessages(404, response(404, "nf"), req)
        assertEquals(433, err4xx.first)
    }

    /**
     * test_12: PUSH_EVENT errors → 5xx→536, else→436; message includes event type.
     */
    @Test
    fun test_12_error_PUSH_EVENT_5xx_536_else_436_containsType() {
        val req = pushEventRequest(type = DELIVERY)

        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(536, err5xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err5xx.second.contains("${FRAG_TYPE}${DELIVERY}"))

        val (err4xx, _) = PairBuilder.getRequestMessages(401, response(16, "unauth"), req)
        assertEquals(436, err4xx.first)
        assertTrue(MSG_ERROR_PUSH_EVENT_HAS_TYPE, err4xx.second.contains("${FRAG_TYPE}${DELIVERY}"))
    }

    /**
     * test_13: MOBILE_EVENT errors → 5xx→537, else→437; message includes event name.
     */
    @Test
    fun test_13_error_MOBILE_EVENT_5xx_537_else_437_containsName() {
        val req = mobileEventRequest()

        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(537, err5xx.first)
        assertTrue(MSG_ERROR_MOBILE_EVENT_HAS_NAME, err5xx.second.contains("${FRAG_NAME}${MOBILE_EVENT_NAME}"))

        val (err4xx, _) = PairBuilder.getRequestMessages(409, response(9, "cl"), req)
        assertEquals(437, err4xx.first)
        assertTrue(MSG_ERROR_MOBILE_EVENT_HAS_NAME, err4xx.second.contains("${FRAG_NAME}${MOBILE_EVENT_NAME}"))
    }

    /**
     * test_14: PROFILE_UPDATE errors → 5xx→538, else→438.
     */
    @Test
    fun test_14_error_PROFILE_UPDATE_5xx_538_else_438() {
        val req = profileUpdateRequest()

        val (err5xx, _) = PairBuilder.getRequestMessages(503, response(7, "srv"), req)
        assertEquals(538, err5xx.first)

        val (err4xx, _) = PairBuilder.getRequestMessages(422, response(9, "cl"), req)
        assertEquals(438, err4xx.first)
    }

    /**
     * test_15: UNSUSPEND error code fixed → 434 (regardless of HTTP code).
     */
    @Test
    fun test_15_error_UNSUSPEND_fixed_434() {
        val req = requestWithName(RequestName.UNSUSPEND_REQUEST)

        val (err, _) = PairBuilder.getRequestMessages(409, response(10, "conflict"), req)
        assertEquals(434, err.first)
        assertTrue(err.second.contains(UNSUSPEND_REQUEST))
    }

    /**
     * test_16: STATUS error code fixed → 435 (regardless of HTTP code).
     */
    @Test
    fun test_16_error_STATUS_fixed_435() {
        val req = requestWithName(RequestName.STATUS_REQUEST)

        val (err, _) = PairBuilder.getRequestMessages(400, response(11, "bad"), req)
        assertEquals(435, err.first)
        assertTrue(err.second.contains(STATUS_REQUEST))
    }

    /**
     * test_17: createSetTokenEventPair() returns non-negative code; message has provider and token.
     */
    @Test
    fun test_17_createSetTokenEventPair_containsProviderAndToken() {
        val data = TokenData(provider = FCM_PROVIDER, token = TOKEN_SAMPLE)
        val (code, msg) = PairBuilder.createSetTokenEventPair(data)

        assertTrue(MSG_PAIR_CODE_NON_NEGATIVE, code >= 0)
        assertTrue(MSG_PAIR_MSG_HAS_PROVIDER, msg.contains(FCM_PROVIDER))
        assertTrue(MSG_PAIR_MSG_HAS_TOKEN, msg.contains(TOKEN_SAMPLE))
        assertTrue(msg.contains("token:"))
    }
}