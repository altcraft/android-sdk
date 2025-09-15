package test.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.LATEST_FOR_PROVIDER
import com.altcraft.sdk.data.Constants.LATEST_SUBSCRIPTION
import com.altcraft.sdk.data.Constants.MATCH_CURRENT_CONTEXT
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Repository
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.Api
import com.altcraft.sdk.network.Network
import com.altcraft.sdk.network.Response
import com.altcraft.sdk.network.Request
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RResponse

/**
 * RequestUnitTest
 *
 * Positive scenarios:
 *  - test_1: subscribeRequest → permission OK, data OK → calls Api.subscribe and returns processResponse result.
 *  - test_2: updateRequest → data OK → calls Api.update and returns processResponse result.
 *  - test_3: pushEventRequest → data OK → calls Api.pushEvent and returns processResponse result.
 *  - test_4: unSuspendRequest → data OK → calls Api.unSuspend and returns processResponse result.
 *  - test_5: statusRequest LATEST_SUBSCRIPTION → provider/token null → calls Api.getProfile and returns result.
 *  - test_6: statusRequest MATCH_CURRENT_CONTEXT → provider/token from data → calls Api.getProfile and returns result.
 *  - test_7: statusRequest LATEST_FOR_PROVIDER with override → uses targetProvider, token null.
 *
 * Negative scenarios:
 *  - test_8: subscribeRequest → permission denied → returns RetryError, Api not called.
 *  - test_9: updateRequest → getUpdateRequestData = null → returns RetryError, Api not called.
 *  - test_10: pushEventRequest → getPushEventRequestData = null → returns RetryError, Api not called.
 *  - test_11: unSuspendRequest → getUnSubscribeRequestData = null → returns Error("profileRequest"), Api not called.
 *  - test_12: statusRequest → getStatusRequestData = null → returns Error("profileRequest"), Api not called.
 *
 * Notes:
 *  - Pure JVM unit tests with MockK; Retrofit calls mocked.
 *  - We stub Response.processResponse(...) to return a canned Event per test.
 */
class RequestTest {

    private lateinit var ctx: Context
    private lateinit var api: Api

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        api = mockk(relaxed = true)

        // Mock singletons/objects we call
        mockkObject(Network)
        every { Network.getRetrofit() } returns api

        mockkObject(Response) // processResponse(...)
        mockkObject(Repository)
        mockkObject(SubFunction)
        mockkObject(Events)

        // Default: permission granted
        every { SubFunction.checkingNotificationPermission(any()) } returns true

        // Default Events.error/retry fallbacks (used in negative branches we assert by type/func)
        every { Events.retry(any(), any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.RetryError(func, 500, "retry", null)
        }
        every { Events.error(any(), any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.Error(func, 400, "error", null)
        }
    }

    @After
    fun tearDown() = unmockkAll()

    // ------------------------ subscribeRequest ------------------------

    /** Verifies subscribeRequest: permission OK, data OK → Api.subscribe called; returns processResponse result. */
    @Test
    fun subscribeRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val item = mockk<SubscribeEntity>(relaxed = true)

        val req = DataClasses.SubscribeRequestData(
            url = "https://api/subscribe",
            time = 123L,
            rToken = "rT",
            uid = "uid-1",
            authHeader = "Bearer X",
            matchingMode = "device",
            provider = "android-firebase",
            deviceToken = "tkn",
            status = "subscribed",
            sync = null,
            profileFields = JsonNull,
            fields = JsonNull,
            cats = emptyList(),
            replace = true,
            skipTriggers = false
        )
        coEvery { Repository.getSubscribeRequestData(ctx, item) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonObject(mapOf()))
        coEvery {
            api.subscribe(
                req.url,
                req.authHeader,
                req.uid,
                req.provider,
                req.matchingMode,
                req.sync,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.subscribeRequest(ctx, item)
        assertSame(expected, out)

        coVerify(exactly = 1) {
            api.subscribe(
                req.url,
                req.authHeader,
                req.uid,
                req.provider,
                req.matchingMode,
                req.sync,
                any()
            )
        }
        verify(exactly = 1) { Response.processResponse(req, retrofitResp) }
    }

    /** Verifies subscribeRequest: permission denied → returns RetryError; Api not called. */
    @Test
    fun subscribeRequest_permissionDenied_returnsRetry_noApiCall() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns false

        val item = mockk<SubscribeEntity>(relaxed = true)
        val result = Request.subscribeRequest(ctx, item)

        assertTrue(result is DataClasses.RetryError)
        assertEquals("subscribeRequest", result.function)
        coVerify(exactly = 0) { api.subscribe(any(), any(), any(), any(), any(), any(), any()) }
    }

    // ------------------------ updateRequest ------------------------

    /** Verifies updateRequest: data OK → Api.update called; returns processResponse result. */
    @Test
    fun updateRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val req = DataClasses.UpdateRequestData(
            url = "https://api/update",
            uid = "uid-2",
            oldToken = "ot",
            newToken = "nt",
            oldProvider = "android-firebase",
            newProvider = "android-huawei",
            authHeader = "Bearer X"
        )
        coEvery { Repository.getUpdateRequestData(ctx, "rid") } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.update(req.url, req.authHeader, req.uid, req.newProvider, req.oldToken, any())
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.updateRequest(ctx, "rid")
        assertSame(expected, out)

        coVerify(exactly = 1) {
            api.update(
                req.url,
                req.authHeader,
                req.uid,
                req.newProvider,
                req.oldToken,
                any()
            )
        }
        verify(exactly = 1) { Response.processResponse(req, retrofitResp) }
    }

    /** Verifies updateRequest: getUpdateRequestData = null → returns RetryError; Api not called. */
    @Test
    fun updateRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        coEvery { Repository.getUpdateRequestData(ctx, any()) } returns null
        val out = Request.updateRequest(ctx, "x")
        assertTrue(out is DataClasses.RetryError)
        assertEquals("updateRequest", out.function)
        coVerify(exactly = 0) { api.update(any(), any(), any(), any(), any(), any()) }
    }

    // ------------------------ pushEventRequest ------------------------

    /** Verifies pushEventRequest: data OK → Api.pushEvent called; returns processResponse result. */
    @Test
    fun pushEventRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val ev = PushEventEntity(uid = "e1", type = "opened")
        val req = DataClasses.PushEventRequestData(
            url = "https://api/event",
            uid = ev.uid,
            time = 999L,
            type = ev.type,
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Repository.getPushEventRequestData(ctx, ev) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.pushEvent(req.url, req.authHeader, req.uid, req.matchingMode, any())
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.pushEventRequest(ctx, ev)
        assertSame(expected, out)
        coVerify(exactly = 1) {
            api.pushEvent(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                any()
            )
        }
        verify(exactly = 1) { Response.processResponse(req, retrofitResp) }
    }

    /** Verifies pushEventRequest: getPushEventRequestData = null → returns RetryError; Api not called. */
    @Test
    fun pushEventRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        coEvery { Repository.getPushEventRequestData(ctx, any()) } returns null
        val out = Request.pushEventRequest(ctx, PushEventEntity(uid = "e", type = "opened"))
        assertTrue(out is DataClasses.RetryError)
        assertEquals("eventRequest", out.function)
        coVerify(exactly = 0) { api.pushEvent(any(), any(), any(), any(), any()) }
    }

    // ------------------------ unSuspendRequest ------------------------

    /** Verifies unSuspendRequest: data OK → Api.unSuspend called; returns processResponse result. */
    @Test
    fun unSuspendRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val req = DataClasses.UnSuspendRequestData(
            url = "https://api/unsuspend",
            uid = "uid-3",
            provider = "android-firebase",
            token = "tkn",
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Repository.getUnSubscribeRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonObject(mapOf()))
        coEvery {
            api.unSuspend(req.url, req.authHeader, req.uid, req.provider, req.token, any())
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.unSuspendRequest(ctx)
        assertSame(expected, out)

        coVerify(exactly = 1) {
            api.unSuspend(
                req.url,
                req.authHeader,
                req.uid,
                req.provider,
                req.token,
                any()
            )
        }
        verify(exactly = 1) { Response.processResponse(req, retrofitResp) }
    }

    /** Verifies unSuspendRequest: getUnSubscribeRequestData = null → returns Error("profileRequest"); Api not called. */
    @Test
    fun unSuspendRequest_nullData_returnsError_noApiCall() = runBlocking {
        coEvery { Repository.getUnSubscribeRequestData(ctx) } returns null
        every { Events.error(any(), any(), any()) } returns DataClasses.Error(
            "profileRequest",
            400,
            "err",
            null
        )

        val out = Request.unSuspendRequest(ctx)
        assertTrue(out is DataClasses.Error)
        assertEquals("profileRequest", out.function)
        coVerify(exactly = 0) { api.unSuspend(any(), any(), any(), any(), any(), any()) }
    }

    // ------------------------ statusRequest ------------------------

    /** Verifies statusRequest: LATEST_SUBSCRIPTION → provider/token null. */
    @Test
    fun statusRequest_latestSubscription_callsApi_withNulls() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            uid = "uid-4",
            provider = "android-firebase", // should be ignored in this mode
            token = "tkn",                  // should be ignored in this mode
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Repository.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(req.url, req.authHeader, req.uid, req.matchingMode, null, null)
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, LATEST_SUBSCRIPTION, null)
        assertSame(expected, out)

        coVerify(exactly = 1) {
            api.getProfile(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                null,
                null
            )
        }
    }

    /** Verifies statusRequest: MATCH_CURRENT_CONTEXT → uses provider/token from data. */
    @Test
    fun statusRequest_matchCurrentContext_usesTokenAndProvider() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            uid = "uid-5",
            provider = "android-huawei",
            token = "zzz",
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Repository.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                "android-huawei",
                "zzz"
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, MATCH_CURRENT_CONTEXT, null)
        assertSame(expected, out)
        coVerify(exactly = 1) {
            api.getProfile(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                "android-huawei",
                "zzz"
            )
        }
    }

    /** Verifies statusRequest: LATEST_FOR_PROVIDER + override → uses targetProvider, token null. */
    @Test
    fun statusRequest_latestForProvider_withOverride() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            uid = "uid-6",
            provider = "android-firebase",
            token = "abc",
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Repository.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                "android-huawei",
                null
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, LATEST_FOR_PROVIDER, "android-huawei")
        assertSame(expected, out)
        coVerify(exactly = 1) {
            api.getProfile(
                req.url,
                req.authHeader,
                req.uid,
                req.matchingMode,
                "android-huawei",
                null
            )
        }
    }

    /** Verifies statusRequest: getStatusRequestData = null → returns Error("profileRequest"); Api not called. */
    @Test
    fun statusRequest_nullData_returnsError_noApiCall() = runBlocking {
        coEvery { Repository.getStatusRequestData(ctx) } returns null
        every { Events.error(any(), any(), any()) } returns DataClasses.Error(
            "profileRequest",
            400,
            "err",
            null
        )

        val out = Request.statusRequest(ctx, LATEST_SUBSCRIPTION, null)
        assertTrue(out is DataClasses.Error)
        assertEquals("profileRequest", out.function)
        coVerify(exactly = 0) { api.getProfile(any(), any(), any(), any(), any(), any()) }
    }
}
