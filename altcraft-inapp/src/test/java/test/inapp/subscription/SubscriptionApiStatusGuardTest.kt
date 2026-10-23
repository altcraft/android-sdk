@file:Suppress("SpellCheckingInspection")

package test.inapp.subscription

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.platform.subscription.status.request.StatusRequest
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionAPI
import com.altcraft.sdk.network.response.Response.ApiResponse
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SubscriptionApiStatusGuardTest
 *
 * Verifies the auth guard of [SubscriptionAPI] does not crash when the
 * profile is not authenticated and returns data when it is.
 *
 * Positive scenarios:
 *  - test_3: an authenticated profile receives the subscription status.
 *
 * Negative scenarios:
 *  - test_1: a non-authenticated profile receives `null` instead of a crash.
 *  - test_2: a non-authenticated profile skips the status network request.
 *  - test_4: a failed status request returns `null` instead of a crash.
 */
class SubscriptionApiStatusGuardTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: not authenticated returns null and emits an auth error instead of crashing. */
    @Test
    fun test_1_notAuthenticated_returnsNull() = runTest {
        mockkObject(CoreEnvironmentProvider)
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk()

        coEvery { CoreEnvironmentProvider.isAuth() } returns false

        val result = SubscriptionAPI.getSubscriptionStatus(context)

        assertNull(result)
        verify(exactly = 1) {
            Events.error("[altcraft-inapp] getStatusForCurrentSubscription", any())
        }
    }

    /** test_2: not authenticated never reaches the status network request. */
    @Test
    fun test_2_notAuthenticated_skipsStatusRequest() = runTest {
        mockkObject(CoreEnvironmentProvider)
        mockkObject(StatusRequest)
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk()

        coEvery { CoreEnvironmentProvider.isAuth() } returns false

        SubscriptionAPI.getSubscriptionStatus(context)

        coVerify(exactly = 0) {
            StatusRequest.inAppStatusRequest(context)
        }
    }

    /** test_3: authenticated returns the status data from the server. */
    @Test
    fun test_3_authenticated_returnsData() = runTest {
        mockkObject(CoreEnvironmentProvider)
        mockkObject(StatusRequest)

        coEvery { CoreEnvironmentProvider.isAuth() } returns true
        coEvery {
            StatusRequest.inAppStatusRequest(context)
        } returns ApiResponse(
            httpCode = 200,
            response = ResponseWithProfile(),
            event = SDKEventModel.Event("statusRequest")
        )

        val result = SubscriptionAPI.getSubscriptionStatus(context)

        assertNotNull(result)
        assertEquals(200, result?.httpCode)
        assertNotNull(result?.response)

        coVerify(exactly = 1) {
            StatusRequest.inAppStatusRequest(context)
        }
    }

    /** test_4: a failed status request returns null instead of crashing. */
    @Test
    fun test_4_statusRequestThrows_returnsNull() = runTest {
        mockkObject(CoreEnvironmentProvider)
        mockkObject(StatusRequest)
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk()

        coEvery { CoreEnvironmentProvider.isAuth() } returns true
        coEvery {
            StatusRequest.inAppStatusRequest(context)
        } throws RuntimeException("boom")

        val result = SubscriptionAPI.getSubscriptionStatus(context)

        assertNull(result)
        verify(exactly = 1) {
            Events.error("[altcraft-inapp] getStatusForCurrentSubscription", any())
        }
    }
}
