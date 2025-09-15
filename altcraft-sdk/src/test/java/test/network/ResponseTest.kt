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
import com.altcraft.sdk.events.EventList.bodyIsNotJson
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.Response
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
 * ResponseUnitTest
 *
 * Positive scenarios:
 *  - test_1: 2xx → SUCCESS → Events.event uses success pair/value.
 *  - test_2: 5xx → RETRY → Events.retry uses error pair/value.
 *  - test_3: 4xx → ERROR → Events.error uses error pair/value.
 *  - test_4: getRequestName maps all RequestData types.
 *
 * Negative scenarios:
 *  - test_5: non-JSON error body → logs bodyIsNotJson and is ignored.
 *  - test_6: getResponseData==null → exception(responseDataIsNull) → retry("processResponse:: <name>").
 *
 * Notes:
 *  - Pure JVM tests (MockK). PairBuilder & MapBuilder stubbed to isolate Response.
 *  - isJsonString is an extension; we DON'T mock it — we pass bodies that naturally trigger it.
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

        // Events stubs return concrete DataClasses.* for assertions
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

    // ---- Helpers: fake RequestData instances ----

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

    /** Verifies SUCCESS path: 2xx → Events.event with success pair/value. */
    @Test
    fun processResponse_success_2xx_callsEvent() {
        val body: JsonElement = buildJsonObject { put("ok", true) } // valid JSON
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

    /** Verifies RETRY path: 5xx → Events.retry with error pair/value. */
    @Test
    fun processResponse_retry_5xx_callsRetry() {
        val errBody = """{"error":500}""".toResponseBody(CT_JSON) // valid JSON
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

    /** Verifies ERROR path: 4xx → Events.error with error pair/value. */
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

    /** Verifies getRequestName returns the right constant for each RequestData type. */
    @Test
    fun getRequestName_mapsAllTypes() {
        assertEquals(SUBSCRIBE_REQUEST, Response.getRequestName(subscribeReq()))
        assertEquals(UPDATE_REQUEST, Response.getRequestName(updateReq()))
        assertEquals(PUSH_EVENT_REQUEST, Response.getRequestName(pushEventReq()))
        assertEquals(UNSUSPEND_REQUEST, Response.getRequestName(unSuspendReq()))
        assertEquals(STATUS_REQUEST, Response.getRequestName(statusReq()))
    }

    /** Ensures non-JSON error body logs bodyIsNotJson and is ignored by parser. */
    @Test
    fun getResponseBody_nonJson_logsAndReturnsNull() {
        // Not JSON on purpose → extension isJsonString() returns false (no mocking)
        val body = "NOT_JSON".toResponseBody(CT_JSON)
        val resp = RResponse.error<JsonElement>(400, body)

        every {
            PairBuilder.getRequestMessages(
                any(),
                any(),
                any()
            )
        } returns ((430 to "e") to (230 to "ok"))
        every { MapBuilder.createEventValue(any(), any(), any()) } returns emptyMap()

        Response.processResponse(subscribeReq(), resp)

        verify {
            Events.error(
                eq("getResponseBody"),
                eq(bodyIsNotJson),
                match { it["body"] == "NOT_JSON" }
            )
        }
    }

    /** When getResponseData returns null → retry("processResponse:: <name>"). */
    @Test
    fun processResponse_whenDataNull_returnsRetryWithComposedName() {
        val body: JsonElement = buildJsonObject { put("x", 1) }
        val resp = RResponse.success(body)

        every {
            PairBuilder.getRequestMessages(
                any(),
                any(),
                any()
            )
        } throws RuntimeException("boom")
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
