package test.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.MapBuilder
import com.altcraft.sdk.additional.PairBuilder
import com.altcraft.sdk.data.Constants.PUSH_EVENT_REQUEST
import com.altcraft.sdk.data.Constants.STATUS_REQUEST
import com.altcraft.sdk.data.Constants.SUBSCRIBE_REQUEST
import com.altcraft.sdk.data.Constants.UNSUSPEND_REQUEST
import com.altcraft.sdk.data.Constants.UPDATE_REQUEST
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.network.Response
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RResponse

/**
 * ResponseTest
 *
 * Positive scenarios:
 *  - test_1: 2xx → SUCCESS → Events.event uses success pair/value
 *  - test_2: 5xx → RETRY → Events.retry uses error pair/value
 *  - test_3: 4xx → ERROR → Events.error uses error pair/value
 *  - test_4: getRequestName maps all RequestData types
 *
 * Negative/edge scenarios:
 *  - test_5: non-JSON plain text body → treated as ERROR with error pair/value
 *  - test_6: HTML body → parsed as null but handled via ERROR
 *  - test_7: getResponseData throws → retry with composed name "processResponse:: <name>"
 */
class ResponseTest {

    private companion object {
        const val FUNC = "processResponse"
        const val AUTH = "Bearer X"
        const val URL = "https://example/api"
        val CT_JSON = "application/json".toMediaType()
    }

    @Before
    fun setUp() {
        mockkObject(Events, PairBuilder, MapBuilder)

        every { Events.event(any(), any(), any()) } answers {
            val f = firstArg<String>()
            val p = secondArg<Pair<Int, String>>()
            DataClasses.Event(f, p.first, p.second, thirdArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            val f = firstArg<String>()
            val p = secondArg<Pair<Int, String>>()
            DataClasses.RetryError(f, p.first, p.second, thirdArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            val f = firstArg<String>()
            val second = secondArg<Any?>()
            val (code, msg) = when (second) {
                is Pair<*, *> -> (second.first as? Int) to (second.second?.toString() ?: "err")
                else -> 400 to "err"
            }
            DataClasses.Error(f, code, msg, thirdArg())
        }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun subscribeReq() = DataClasses.SubscribeRequestData(
        url = URL, time = 1L, rToken = null, uid = "u1", authHeader = AUTH,
        matchingMode = "m", provider = "p", deviceToken = "t", status = "subscribed",
        sync = null, profileFields = JsonNull, fields = JsonNull, cats = emptyList(),
        replace = false, skipTriggers = false
    )

    private fun updateReq() = DataClasses.UpdateRequestData(
        url = URL, uid = "u2", oldToken = "ot", newToken = "nt",
        oldProvider = "op", newProvider = "np", authHeader = AUTH
    )

    private fun pushEventReq() = DataClasses.PushEventRequestData(
        url = URL, time = 2L, type = "opened", uid = "e1", authHeader = AUTH, matchingMode = "m"
    )

    private fun unSuspendReq() = DataClasses.UnSuspendRequestData(
        url = URL, uid = "u3", provider = "p", token = "t", authHeader = AUTH, matchingMode = "m"
    )

    private fun statusReq() = DataClasses.StatusRequestData(
        url = URL, uid = "u4", authHeader = AUTH, matchingMode = "m", provider = "p", token = "t"
    )

    /** - test_1: 2xx → SUCCESS → Events.event uses success pair/value. */
    @Test
    fun processResponse_success_2xx_callsEvent() {
        val body: JsonElement = buildJsonObject { put("ok", true) }
        val resp = RResponse.success(body)
        val req = subscribeReq()

        val errorPair = 430 to "err"
        val successPair = 230 to "ok"
        val value = mapOf("code" to 200)

        every { PairBuilder.getRequestMessages(200, any(), req) } returns (errorPair to successPair)
        every { MapBuilder.createEventValue(200, any(), req) } returns value

        val out = Response.processResponse(req, resp)

        assertEquals(FUNC, out.function)
        assertEquals(230, out.eventCode)
        assertEquals("ok", out.eventMessage)
        assertEquals(value, out.eventValue)
    }

    /** - test_2: 5xx → RETRY → Events.retry uses error pair/value. */
    @Test
    fun processResponse_retry_5xx_callsRetry() {
        val errBody = """{"error":500}""".toResponseBody(CT_JSON)
        val resp = RResponse.error<JsonElement>(500, errBody)
        val req = updateReq()

        val errorPair = 531 to "server-err"
        val successPair = 231 to "ok"
        val value = mapOf("code" to 500)

        every { PairBuilder.getRequestMessages(500, any(), req) } returns (errorPair to successPair)
        every { MapBuilder.createEventValue(500, any(), req) } returns value

        val out = Response.processResponse(req, resp)

        assertTrue(out is DataClasses.RetryError)
        assertEquals(FUNC, out.function)
        assertEquals(531, out.eventCode)
        assertEquals("server-err", out.eventMessage)
        assertEquals(value, out.eventValue)
    }

    /** - test_3: 4xx → ERROR → Events.error uses error pair/value. */
    @Test
    fun processResponse_error_4xx_callsError() {
        val errBody = """{"error":400,"errorText":"bad"}""".toResponseBody(CT_JSON)
        val resp = RResponse.error<JsonElement>(400, errBody)
        val req = pushEventReq()

        val errorPair = 432 to "client-err"
        val successPair = 232 to "ok"
        val value = mapOf("code" to 400)

        every { PairBuilder.getRequestMessages(400, any(), req) } returns (errorPair to successPair)
        every { MapBuilder.createEventValue(400, any(), req) } returns value

        val out = Response.processResponse(req, resp)

        assertTrue(out is DataClasses.Error)
        assertEquals(FUNC, out.function)
        assertEquals(432, out.eventCode)
        assertEquals("client-err", out.eventMessage)
        assertEquals(value, out.eventValue)
    }

    /** - test_4: getRequestName maps all RequestData types. */
    @Test
    fun getRequestName_mapsAllTypes() {
        assertEquals(SUBSCRIBE_REQUEST, Response.getRequestName(subscribeReq()))
        assertEquals(UPDATE_REQUEST, Response.getRequestName(updateReq()))
        assertEquals(PUSH_EVENT_REQUEST, Response.getRequestName(pushEventReq()))
        assertEquals(UNSUSPEND_REQUEST, Response.getRequestName(unSuspendReq()))
        assertEquals(STATUS_REQUEST, Response.getRequestName(statusReq()))
    }

    /** - test_5: non-JSON plain text body → treated as ERROR with error pair/value. */
    @Test
    fun processResponse_nonJsonTextBody_treatedAsError() {
        val body = "NOT_JSON".toResponseBody(CT_JSON)
        val resp = RResponse.error<JsonElement>(400, body)
        val req = subscribeReq()

        val errorPair = 433 to "non-json"
        val successPair = 233 to "ok"
        val value = mapOf("code" to 400)

        every { PairBuilder.getRequestMessages(400, any(), req) } returns (errorPair to successPair)
        every { MapBuilder.createEventValue(400, any(), req) } returns value

        val out = Response.processResponse(req, resp)

        assertTrue(out is DataClasses.Error)
        assertEquals(FUNC, out.function)
        assertEquals(433, out.eventCode)
        assertEquals("non-json", out.eventMessage)
        assertEquals(value, out.eventValue)
    }

    /** - test_6: HTML body → parsed as null but handled via ERROR. */
    @Test
    fun processResponse_htmlBody_treatedAsNullButHandled() {
        val body = "<html><body>Oops</body></html>".toResponseBody(CT_JSON)
        val resp = RResponse.error<JsonElement>(400, body)
        val req = updateReq()

        val errorPair = 434 to "html-body"
        val successPair = 234 to "ok"
        val value = emptyMap<String, Any?>()

        every { PairBuilder.getRequestMessages(400, any(), req) } returns (errorPair to successPair)
        every { MapBuilder.createEventValue(400, any(), req) } returns value

        val out = Response.processResponse(req, resp)

        assertTrue(out is DataClasses.Error)
        assertEquals(FUNC, out.function)
        assertEquals(434, out.eventCode)
        assertEquals("html-body", out.eventMessage)
        assertEquals(value, out.eventValue)
    }

    /** - test_7: getResponseData throws → retry with composed name "processResponse:: <name>". */
    @Test
    fun processResponse_whenDataNull_returnsRetryWithComposedName() {
        val body: JsonElement = buildJsonObject { put("x", 1) }
        val resp = RResponse.success(body)

        every { PairBuilder.getRequestMessages(any(), any(), any()) } throws RuntimeException("boom")
        every { MapBuilder.createEventValue(any(), any(), any()) } returns emptyMap()

        every { Events.retry(any(), any(), any()) } answers {
            val f = firstArg<String>()
            DataClasses.RetryError(f, 500, "retry", null)
        }

        val out = Response.processResponse(pushEventReq(), resp)
        assertTrue(out is DataClasses.RetryError)
        assertEquals("processResponse:: $PUSH_EVENT_REQUEST", out.function)
    }
}