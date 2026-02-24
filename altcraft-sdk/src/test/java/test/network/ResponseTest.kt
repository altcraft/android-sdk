@file:Suppress("SpellCheckingInspection")

package test.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.MapBuilder
import com.altcraft.sdk.additional.PairBuilder
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.network.Response
import com.altcraft.sdk.sdk_events.Events
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RResponse

/**
 * ResponseTest
 *
 * Positive scenarios:
 *  - test_1: 2xx → SUCCESS → Events.event uses success pair/value.
 *  - test_2: 5xx → RETRY → Events.retry uses error pair/value.
 *  - test_3: 4xx → ERROR → Events.error uses error pair/value.
 *
 * Negative/edge scenarios:
 *  - test_4: getResponseData throws → retry with composed name "processResponse:: <requestName.value>".
 */
class ResponseTest {

    private companion object {
        private const val FUNC = "processResponse"
        private const val AUTH = "Bearer X"
        private const val URL = "https://example/api"
        private val CT_JSON = "application/json".toMediaType()
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

        every { Events.retry(any(), any()) } answers {
            val f = firstArg<String>()
            DataClasses.RetryError(f, 500, "retry", null)
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
        url = URL,
        requestId = "u1",
        time = 1L,
        rToken = null,
        authHeader = AUTH,
        matchingMode = "m",
        provider = "p",
        deviceToken = "t",
        status = SUBSCRIBED,
        sync = null,
        profileFields = JsonNull,
        fields = JsonNull,
        cats = emptyList(),
        replace = false,
        skipTriggers = false
    )

    private fun updateReq() = DataClasses.TokenUpdateRequestData(
        url = URL,
        requestId = "u2",
        oldToken = "ot",
        newToken = "nt",
        oldProvider = "op",
        newProvider = "np",
        authHeader = AUTH,
        sync = false
    )

    private fun pushEventReq() = DataClasses.PushEventRequestData(
        url = URL,
        requestId = "u5",
        time = 2L,
        type = "opened",
        uid = "e1",
        authHeader = AUTH,
        matchingMode = "m"
    )

    /** test_1: 2xx → SUCCESS → Events.event uses success pair/value. */
    @Test
    fun processResponse_success_2xx_callsEvent() {
        val body: JsonElement = buildJsonObject { put("ok", JsonPrimitive(true)) }
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

        verify(exactly = 1) { Events.event(FUNC, successPair, value) }
    }

    /** test_2: 5xx → RETRY → Events.retry uses error pair/value. */
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

        verify(exactly = 1) { Events.retry(FUNC, errorPair, value) }
    }

    /** test_3: 4xx → ERROR → Events.error uses error pair/value. */
    @Test
    fun processResponse_error_4xx_callsError() {
        val errBody = """{"error":400}""".toResponseBody(CT_JSON)
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

        verify(exactly = 1) { Events.error(FUNC, errorPair, value) }
    }

    /** test_4: getResponseData throws → retry with composed name "processResponse:: <requestName.value>". */
    @Test
    fun processResponse_whenDataNull_returnsRetryWithComposedName() {
        val body: JsonElement = buildJsonObject { put("x", JsonPrimitive(1)) }
        val resp = RResponse.success(body)

        every {
            PairBuilder.getRequestMessages(
                any(),
                any(),
                any()
            )
        } throws RuntimeException("boom")
        every { MapBuilder.createEventValue(any(), any(), any()) } returns emptyMap()

        val req = pushEventReq()
        val out = Response.processResponse(req, resp)

        assertTrue(out is DataClasses.RetryError)
        assertEquals("processResponse:: ${req.requestName.value}", out.function)
    }
}