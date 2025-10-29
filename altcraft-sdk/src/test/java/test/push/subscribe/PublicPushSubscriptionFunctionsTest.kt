@file:Suppress("SpellCheckingInspection")

package test.push.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.LATEST_FOR_PROVIDER
import com.altcraft.sdk.data.Constants.LATEST_SUBSCRIPTION
import com.altcraft.sdk.data.Constants.MATCH_CURRENT_CONTEXT
import com.altcraft.sdk.data.Constants.RESPONSE_WITH_HTTP_CODE
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.Constants.SUSPENDED
import com.altcraft.sdk.data.Constants.UNSUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.subscribe.PushSubscribe
import com.altcraft.sdk.push.subscribe.PublicPushSubscriptionFunctions
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PublicPushSubscriptionFunctionsTest
 *
 * Positive scenarios:
 *  - test_1: pushSubscribe() routes to PushSubscribe.pushSubscribe with SUBSCRIBED and sync=1
 *  - test_2: pushSuspend() routes with SUSPENDED and sync=0
 *  - test_3: pushUnSubscribe() routes with UNSUBSCRIBED and sync=1
 *  - test_4: unSuspendPushSubscription() returns ResponseWithHttpCode on success
 *  - test_5: getStatusOfLatestSubscription() returns ResponseWithHttpCode on success
 *  - test_6: getStatusOfLatestSubscriptionForProvider() returns ResponseWithHttpCode for valid provider
 *  - test_7: getStatusForCurrentSubscription() returns ResponseWithHttpCode on success
 *  - test_8: actionField() returns a non-null builder
 *
 * Negative scenarios:
 *  - test_9: getStatusOfLatestSubscriptionForProvider() with invalid provider returns null and does
 *  not call statusRequest
 *  - test_10: unSuspendPushSubscription() returns null on exception
 *  - test_11: getStatusOfLatestSubscription() returns null on exception
 *  - test_12: getStatusForCurrentSubscription() returns null on exception
 */
class PublicPushSubscriptionFunctionsTest {

    private companion object {
        private const val ANY_PROVIDER_VALID = FCM_PROVIDER
        private const val ANY_PROVIDER_INVALID = "NOT_A_PROVIDER"
        private const val DUMMY_FUNCTION = "fn"
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        mockkObject(PushSubscribe)
        mockkObject(Request)

        coEvery { Request.unSuspendRequest(any()) } returns dummyEventWithResponse()
        coEvery { Request.statusRequest(any(), any(), any()) } returns dummyEventWithResponse()

        every {
            PushSubscribe.pushSubscribe(
                context = any(),
                sync = any(),
                status = any(),
                customFields = any(),
                profileFields = any(),
                cats = any(),
                replace = any(),
                skipTriggers = any()
            )
        } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Log::class)
    }

    /** Builds Event with eventValue[RESPONSE_WITH_HTTP_CODE] = mocked ResponseWithHttpCode. */
    private fun dummyEventWithResponse(): DataClasses.Event {
        val httpResp = mockk<DataClasses.ResponseWithHttpCode>(relaxed = true)
        val value = mapOf(RESPONSE_WITH_HTTP_CODE to httpResp)
        return DataClasses.Event(DUMMY_FUNCTION, null, null, value)
    }

    /** - test_1: pushSubscribe routes internally with SUBSCRIBED and sync=1. */
    @Test
    fun test_pushSubscribe_routesToInternal_withSubscribed_andSyncOne() {
        val profile = mapOf("name" to "Alice")
        val custom = mapOf("k1" to "v1")
        val cats = listOf(DataClasses.CategoryData(name = "news", active = true))
        val replace = true
        val skipTriggers = false

        PublicPushSubscriptionFunctions.pushSubscribe(
            context = ctx,
            sync = true,
            profileFields = profile,
            customFields = custom,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )

        verify(exactly = 1) {
            PushSubscribe.pushSubscribe(
                context = ctx,
                sync = 1,
                status = SUBSCRIBED,
                profileFields = profile,
                customFields = custom,
                cats = cats,
                replace = replace,
                skipTriggers = skipTriggers
            )
        }
        confirmVerified(PushSubscribe)
    }

    /** - test_2: pushSuspend routes internally with SUSPENDED and sync=0. */
    @Test
    fun test_pushSuspend_routesToInternal_withSuspended_andSyncZero() {
        val profile = mapOf("age" to 30)
        val custom = mapOf("tier" to "gold")

        PublicPushSubscriptionFunctions.pushSuspend(
            context = ctx,
            sync = false,
            profileFields = profile,
            customFields = custom,
            cats = null,
            replace = null,
            skipTriggers = null
        )

        verify(exactly = 1) {
            PushSubscribe.pushSubscribe(
                context = ctx,
                sync = 0,
                status = SUSPENDED,
                profileFields = profile,
                customFields = custom,
                cats = null,
                replace = null,
                skipTriggers = null
            )
        }
        confirmVerified(PushSubscribe)
    }

    /** - test_3: pushUnSubscribe routes internally with UNSUBSCRIBED and sync=1. */
    @Test
    fun test_pushUnSubscribe_routesToInternal_withUnsubscribed_andSyncOne() {
        PublicPushSubscriptionFunctions.pushUnSubscribe(
            context = ctx,
            sync = true,
            profileFields = null,
            customFields = null,
            cats = null,
            replace = null,
            skipTriggers = null
        )

        verify(exactly = 1) {
            PushSubscribe.pushSubscribe(
                context = ctx,
                sync = 1,
                status = UNSUBSCRIBED,
                profileFields = null,
                customFields = null,
                cats = null,
                replace = null,
                skipTriggers = null
            )
        }
        confirmVerified(PushSubscribe)
    }

    /** - test_4: unSuspendPushSubscription returns ResponseWithHttpCode on success. */
    @Test
    fun test_unSuspendPushSubscription_returnsResponseOnSuccess() = runBlocking {
        val res = PublicPushSubscriptionFunctions.unSuspendPushSubscription(ctx)
        assertNotNull(res)
    }

    /** - test_10: unSuspendPushSubscription returns null on exception. */
    @Test
    fun test_unSuspendPushSubscription_returnsNullOnException() = runBlocking {
        coEvery { Request.unSuspendRequest(any()) } throws RuntimeException("boom")

        val res = PublicPushSubscriptionFunctions.unSuspendPushSubscription(ctx)
        assertNull(res)
    }

    /** - test_5: getStatusOfLatestSubscription returns ResponseWithHttpCode on success. */
    @Test
    fun test_getStatusOfLatestSubscription_returnsResponse() = runBlocking {
        val res = PublicPushSubscriptionFunctions.getStatusOfLatestSubscription(ctx)
        assertNotNull(res)

        coVerify(exactly = 1) { Request.statusRequest(ctx, mode = LATEST_SUBSCRIPTION) }
    }

    /** - test_11: getStatusOfLatestSubscription returns null on exception. */
    @Test
    fun test_getStatusOfLatestSubscription_returnsNullOnException() = runBlocking {
        coEvery { Request.statusRequest(any(), any(), any()) } throws IllegalStateException("err")

        val res = PublicPushSubscriptionFunctions.getStatusOfLatestSubscription(ctx)
        assertNull(res)
    }

    /** - test_6: getStatusOfLatestSubscriptionForProvider returns ResponseWithHttpCode for a valid provider. */
    @Test
    fun test_getStatusOfLatestSubscriptionForProvider_validProvider_returnsResponse() = runBlocking {
        val res = PublicPushSubscriptionFunctions.getStatusOfLatestSubscriptionForProvider(
            context = ctx,
            provider = ANY_PROVIDER_VALID
        )
        assertNotNull(res)

        coVerify(exactly = 1) {
            Request.statusRequest(
                ctx,
                mode = LATEST_FOR_PROVIDER,
                targetProvider = ANY_PROVIDER_VALID
            )
        }
    }

    /** - test_9: getStatusOfLatestSubscriptionForProvider with invalid provider returns null and does not call statusRequest. */
    @Test
    fun test_getStatusOfLatestSubscriptionForProvider_invalidProvider_returnsNull_andNoRequest() = runBlocking {
        val res = PublicPushSubscriptionFunctions.getStatusOfLatestSubscriptionForProvider(
            context = ctx,
            provider = ANY_PROVIDER_INVALID
        )
        assertNull(res)

        coVerify(exactly = 0) { Request.statusRequest(any(), any(), any()) }
    }

    /** - test_7: getStatusForCurrentSubscription returns ResponseWithHttpCode on success. */
    @Test
    fun test_getStatusForCurrentSubscription_returnsResponse() = runBlocking {
        val res = PublicPushSubscriptionFunctions.getStatusForCurrentSubscription(ctx)
        assertNotNull(res)

        coVerify(exactly = 1) { Request.statusRequest(ctx, mode = MATCH_CURRENT_CONTEXT) }
    }

    /** - test_12: getStatusForCurrentSubscription returns null on exception. */
    @Test
    fun test_getStatusForCurrentSubscription_returnsNullOnException() = runBlocking {
        coEvery { Request.statusRequest(any(), any(), any()) } throws RuntimeException("fail")

        val res = PublicPushSubscriptionFunctions.getStatusForCurrentSubscription(ctx)
        assertNull(res)
    }

    /** - test_8: actionField returns a non-null builder. */
    @Test
    fun test_actionField_returnsBuilder() {
        val builder = PublicPushSubscriptionFunctions.actionField("Inc")
        assertNotNull(builder)
    }
}