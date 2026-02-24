@file:Suppress("SpellCheckingInspection")

package test.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants.LATEST_FOR_PROVIDER
import com.altcraft.sdk.data.Constants.LATEST_SUBSCRIPTION
import com.altcraft.sdk.data.Constants.MATCH_CURRENT_CONTEXT
import com.altcraft.sdk.data.Collector
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.ProfileUpdateEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.mob_events.PartsFactory
import com.altcraft.sdk.network.Api
import com.altcraft.sdk.network.Network
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.network.Response
import com.altcraft.sdk.sdk_events.Events
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RResponse

/**
 * RequestTest
 *
 * Positive scenarios:
 * - test_1: subscribeRequest — permission OK, data OK → calls Api.subscribe and returns processResponse result.
 * - test_2: updateRequest — data OK → calls Api.update and returns processResponse result.
 * - test_3: pushEventRequest — data OK → calls Api.pushEvent and returns processResponse result.
 * - test_4: unSuspendRequest — data OK → calls Api.unSuspend and returns processResponse result.
 * - test_5: statusRequest LATEST_SUBSCRIPTION — provider/token null → Api.getProfile and returns result.
 * - test_6: statusRequest MATCH_CURRENT_CONTEXT — provider/token from data → Api.getProfile and returns result.
 * - test_7: statusRequest LATEST_FOR_PROVIDER with override — uses targetProvider, token null.
 * - test_13: mobileEventRequest — data OK + parts OK → Api.mobileEvent and returns processResponse result.
 * - test_16: profileUpdateRequest — data OK → Api.profileUpdate called; returns processResponse result.
 *
 * Negative scenarios:
 * - test_8: subscribeRequest — permission denied → returns RetryError, Api not called.
 * - test_9: updateRequest — getUpdateRequestData = null → returns RetryError, Api not called.
 * - test_10: pushEventRequest — getPushEventRequestData = null → returns RetryError, Api not called.
 * - test_11: unSuspendRequest — getUnSubscribeRequestData = null → returns Error("profileRequest"), Api not called.
 * - test_12: statusRequest — getStatusRequestData = null → returns Error("profileRequest"), Api not called.
 * - test_14: mobileEventRequest — getMobileEventRequestData = null → returns RetryError, Api not called.
 * - test_15: mobileEventRequest — parts == null → returns Error("mobileEventRequest"), Api not called.
 * - test_17: profileUpdateRequest — getProfileUpdateRequestData = null → returns RetryError, Api not called.
 */
class RequestTest {

    private lateinit var ctx: Context
    private lateinit var api: Api

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        api = mockk(relaxed = true)

        mockkObject(Network)
        every { Network.getRetrofit() } returns api

        mockkObject(Response)
        mockkObject(Collector)
        mockkObject(SubFunction)
        mockkObject(Events)

        every { SubFunction.checkingNotificationPermission(any()) } returns true

        every { Events.retry(any(), any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.RetryError(func, 500, "retry", null)
        }
        every { Events.retry(any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.RetryError(func, 500, "retry", null)
        }
        every { Events.error(any(), any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.Error(func, 400, "error", null)
        }
        every { Events.error(any(), any()) } answers {
            val func = firstArg<String>()
            DataClasses.Error(func, 400, "error", null)
        }
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: subscribeRequest — permission OK, data OK → Api.subscribe called; returns processResponse result. */
    @Test
    fun pushSubscribeRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val item = mockk<SubscribeEntity>(relaxed = true)

        val req = DataClasses.SubscribeRequestData(
            url = "https://api/subscribe",
            requestId = "uid-1",
            time = 123L,
            rToken = "rT",
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
        coEvery { Collector.getSubscribeRequestData(ctx, item) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonObject(emptyMap()))
        coEvery {
            api.subscribe(
                req.url,
                req.authHeader,
                req.requestId,
                req.provider,
                req.matchingMode,
                req.sync,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.pushSubscribeRequest(ctx, item)
        assertSame(expected, out)
    }

    /** - test_8: subscribeRequest — permission denied → returns RetryError, Api not called. */
    @Test
    fun pushSubscribeRequest_permissionDenied_returnsRetry_noApiCall() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns false
        val item = mockk<SubscribeEntity>(relaxed = true)

        val out = Request.pushSubscribeRequest(ctx, item)
        assertTrue(out is DataClasses.RetryError)
    }

    /** - test_2: tokenUpdateRequest — data OK → Api.tokenUpdate called; returns processResponse result. */
    @Test
    fun tokenUpdateRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val req = DataClasses.TokenUpdateRequestData(
            url = "https://api/update",
            requestId = "uid-2",
            oldToken = "ot",
            newToken = "nt",
            oldProvider = "android-firebase",
            newProvider = "android-huawei",
            authHeader = "Bearer X",
            sync = false
        )
        coEvery { Collector.getTokenUpdateRequestData(ctx, "rid") } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.tokenUpdate(
                req.url,
                req.authHeader,
                req.requestId,
                req.newProvider,
                req.oldToken,
                req.sync,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.tokenUpdateRequest(ctx, "rid")
        assertSame(expected, out)
    }

    /** - test_9: tokenUpdateRequest — getTokenUpdateRequestData = null → returns RetryError, Api not called. */
    @Test
    fun tokenUpdateRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        coEvery { Collector.getTokenUpdateRequestData(ctx, any()) } returns null

        val out = Request.tokenUpdateRequest(ctx, "x")
        assertTrue(out is DataClasses.RetryError)
    }

    /** - test_3: pushEventRequest — data OK → Api.pushEvent called; returns processResponse result. */
    @Test
    fun pushEventRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val ev = PushEventEntity(uid = "e1", type = "opened")
        val req = DataClasses.PushEventRequestData(
            url = "https://api/event",
            requestId = "rid-1",
            time = 999L,
            type = ev.type,
            uid = ev.uid,
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Collector.getPushEventRequestData(ctx, ev) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.pushEvent(
                req.url,
                req.authHeader,
                req.requestId,
                req.matchingMode,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.pushEventRequest(ctx, ev)
        assertSame(expected, out)
    }

    /** - test_10: pushEventRequest — getPushEventRequestData = null → returns RetryError; Api not called. */
    @Test
    fun pushEventRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        coEvery { Collector.getPushEventRequestData(ctx, any()) } returns null

        val out = Request.pushEventRequest(ctx, PushEventEntity(uid = "e", type = "opened"))
        assertTrue(out is DataClasses.RetryError)
    }

    /** - test_4: unSuspendRequest — data OK → Api.unSuspend called; returns processResponse result. */
    @Test
    fun unSuspendRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val req = DataClasses.UnSuspendRequestData(
            url = "https://api/unsuspend",
            requestId = "uid-3",
            provider = "android-firebase",
            token = "tkn",
            authHeader = "Bearer X",
            matchingMode = "device"
        )
        coEvery { Collector.getUnSuspendRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonObject(emptyMap()))
        coEvery {
            api.unSuspend(
                req.url,
                req.authHeader,
                req.requestId,
                req.provider,
                req.token,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.unSuspendRequest(ctx)
        assertSame(expected, out)
    }

    /** - test_11: unSuspendRequest — getUnSuspendRequestData = null → returns Error("profileRequest"); Api not called. */
    @Test
    fun unSuspendRequest_nullData_returnsError_noApiCall() = runBlocking {
        coEvery { Collector.getUnSuspendRequestData(ctx) } returns null

        val out = Request.unSuspendRequest(ctx)
        assertTrue(out is DataClasses.Error)
    }

    /** - test_5: statusRequest — LATEST_SUBSCRIPTION → provider/token null. */
    @Test
    fun statusRequest_latestSubscription_callsApi_withNulls() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            requestId = "uid-4",
            authHeader = "Bearer X",
            matchingMode = "device",
            provider = "android-firebase",
            token = "tkn"
        )
        coEvery { Collector.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(
                req.url,
                req.authHeader,
                req.requestId,
                req.matchingMode,
                null,
                null
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, LATEST_SUBSCRIPTION, null)
        assertSame(expected, out)
    }

    /** - test_6: statusRequest — MATCH_CURRENT_CONTEXT → uses provider/token from data. */
    @Test
    fun statusRequest_matchCurrentContext_usesTokenAndProvider() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            requestId = "uid-5",
            authHeader = "Bearer X",
            matchingMode = "device",
            provider = "android-huawei",
            token = "zzz"
        )
        coEvery { Collector.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(
                req.url,
                req.authHeader,
                req.requestId,
                req.matchingMode,
                "android-huawei",
                "zzz"
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, MATCH_CURRENT_CONTEXT, null)
        assertSame(expected, out)
    }

    /** - test_7: statusRequest — LATEST_FOR_PROVIDER with override → uses targetProvider, token null. */
    @Test
    fun statusRequest_latestForProvider_withOverride() = runBlocking {
        val req = DataClasses.StatusRequestData(
            url = "https://api/profile",
            requestId = "uid-6",
            authHeader = "Bearer X",
            matchingMode = "device",
            provider = "android-firebase",
            token = "abc"
        )
        coEvery { Collector.getStatusRequestData(ctx) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.getProfile(
                req.url,
                req.authHeader,
                req.requestId,
                req.matchingMode,
                "android-huawei",
                null
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.statusRequest(ctx, LATEST_FOR_PROVIDER, "android-huawei")
        assertSame(expected, out)
    }

    /** - test_12: statusRequest — getStatusRequestData = null → returns Error("profileRequest"); Api not called. */
    @Test
    fun statusRequest_nullData_returnsError_noApiCall() = runBlocking {
        coEvery { Collector.getStatusRequestData(ctx) } returns null

        val out = Request.statusRequest(ctx, LATEST_SUBSCRIPTION, null)
        assertTrue(out is DataClasses.Error)
    }

    /** - test_13: mobileEventRequest — data OK + parts OK → Api.mobileEvent called; returns processResponse result. */
    @Test
    fun mobileEventRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val entity = mockk<MobileEventEntity>(relaxed = true)
        val req = DataClasses.MobileEventRequestData(
            url = "https://api/mobile",
            requestId = "uid-mobile-1",
            sid = "sid-1",
            name = "purchase",
            authHeader = "Bearer X"
        )

        mockkObject(PartsFactory)
        coEvery { Collector.getMobileEventRequestData(ctx, entity) } returns req
        every { PartsFactory.createMobileEventParts(entity) } returns listOf(
            MultipartBody.Part.createFormData("x", "y")
        )

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery { api.mobileEvent(any(), any(), any(), any(), any(), any(), any(), any()) } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.mobileEventRequest(ctx, entity)
        assertSame(expected, out)
    }

    /** - test_14: mobileEventRequest — getMobileEventRequestData = null → returns RetryError; Api not called. */
    @Test
    fun mobileEventRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        val entity = mockk<MobileEventEntity>(relaxed = true)
        coEvery { Collector.getMobileEventRequestData(ctx, entity) } returns null

        val out = Request.mobileEventRequest(ctx, entity)
        assertTrue(out is DataClasses.RetryError)
    }

    /** - test_15: mobileEventRequest — parts == null → returns Error("mobileEventRequest"); Api not called. */
    @Test
    fun mobileEventRequest_partsNull_returnsError_noApiCall() = runBlocking {
        val entity = mockk<MobileEventEntity>(relaxed = true)
        val req = DataClasses.MobileEventRequestData(
            url = "https://api/mobile",
            requestId = "uid-mobile-2",
            sid = "s",
            name = "e",
            authHeader = "Bearer X"
        )

        mockkObject(PartsFactory)
        coEvery { Collector.getMobileEventRequestData(ctx, entity) } returns req
        every { PartsFactory.createMobileEventParts(entity) } returns null

        val out = Request.mobileEventRequest(ctx, entity)
        assertTrue(out is DataClasses.Error)
    }

    /** - test_16: profileUpdateRequest — data OK → Api.profileUpdate called; returns processResponse result. */
    @Test
    fun profileUpdateRequest_success_callsApi_andReturnsProcessResult() = runBlocking {
        val entity = mockk<ProfileUpdateEntity>(relaxed = true)

        val req = DataClasses.ProfileUpdateRequestData(
            url = "https://api/profile/update",
            requestId = "uid-prof-1",
            authHeader = "Bearer X",
            profileFields = JsonNull,
            skipTriggers = true
        )
        coEvery { Collector.getProfileUpdateRequestData(ctx, entity) } returns req

        val retrofitResp = RResponse.success<JsonElement>(JsonNull)
        coEvery {
            api.profileUpdate(
                req.url,
                req.authHeader,
                req.requestId,
                any()
            )
        } returns retrofitResp

        val expected = DataClasses.Event("process", 200, "ok", null)
        every { Response.processResponse(req, retrofitResp) } returns expected

        val out = Request.profileUpdateRequest(ctx, entity)
        assertSame(expected, out)
    }

    /** - test_17: profileUpdateRequest — getProfileUpdateRequestData = null → returns RetryError; Api not called. */
    @Test
    fun profileUpdateRequest_nullData_returnsRetry_noApiCall() = runBlocking {
        val entity = mockk<ProfileUpdateEntity>(relaxed = true)
        coEvery { Collector.getProfileUpdateRequestData(ctx, entity) } returns null

        val out = Request.profileUpdateRequest(ctx, entity)
        assertTrue(out is DataClasses.RetryError)
    }
}
