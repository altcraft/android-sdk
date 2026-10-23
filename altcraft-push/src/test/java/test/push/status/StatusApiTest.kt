@file:Suppress("SpellCheckingInspection")

package test.push.status

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.push.platform.subscription.status.StatusAPI
import com.altcraft.sdk.push.platform.subscription.status.request.StatusMode
import com.altcraft.sdk.push.platform.subscription.status.request.StatusRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * StatusApiTest
 *
 * Verifies that the public status API issues the expected lookup modes
 * and rejects unsupported providers.
 */
class StatusApiTest {

    private val context: Context = mockk(relaxed = true)

    private fun response() = ApiResponse<ResponseModel.ResponseWithProfile>(
        httpCode = 200,
        response = null,
        event = SDKEventModel.Event("push.status")
    )

    @Before
    fun setUp() {
        mockkObject(StatusRequest)

        mockkObject(Events)
        every { Events.error(any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** latest issues a LATEST_SUBSCRIPTION lookup. */
    @Test
    fun latest_issuesLatestSubscriptionLookup() = runTest {
        coEvery {
            StatusRequest.pushStatusRequest(any(), StatusMode.LATEST_SUBSCRIPTION, null)
        } returns response()

        val result = StatusAPI.latest(context)

        assertEquals(200, result!!.httpCode)
        coVerify(exactly = 1) {
            StatusRequest.pushStatusRequest(context, StatusMode.LATEST_SUBSCRIPTION, null)
        }
    }

    /** current issues a MATCH_CURRENT_CONTEXT lookup. */
    @Test
    fun current_issuesMatchCurrentContextLookup() = runTest {
        coEvery {
            StatusRequest.pushStatusRequest(any(), StatusMode.MATCH_CURRENT_CONTEXT, null)
        } returns response()

        val result = StatusAPI.current(context)

        assertEquals(200, result!!.httpCode)
        coVerify(exactly = 1) {
            StatusRequest.pushStatusRequest(context, StatusMode.MATCH_CURRENT_CONTEXT, null)
        }
    }

    /** latestForProvider issues a LATEST_FOR_PROVIDER lookup with the provider. */
    @Test
    fun latestForProvider_issuesProviderLookup() = runTest {
        coEvery {
            StatusRequest.pushStatusRequest(any(), StatusMode.LATEST_FOR_PROVIDER, any())
        } returns response()

        val result = StatusAPI.latestForProvider(context, "android-huawei")

        assertEquals(200, result!!.httpCode)
        coVerify(exactly = 1) {
            StatusRequest.pushStatusRequest(context, StatusMode.LATEST_FOR_PROVIDER, "android-huawei")
        }
    }

    /** An unsupported provider identifier is rejected with an SDK exception. */
    @Test
    fun latestForProvider_invalidProviderThrows() = runTest {
        assertThrows(SDKException.SDKException::class.java) {
            kotlinx.coroutines.runBlocking {
                StatusAPI.latestForProvider(context, "google")
            }
        }
    }

    /** A failing request is downgraded to null without propagating the error. */
    @Test
    fun latest_requestFailureReturnsNull() = runTest {
        coEvery {
            StatusRequest.pushStatusRequest(any(), StatusMode.LATEST_SUBSCRIPTION, null)
        } throws IllegalStateException("boom")

        val result = StatusAPI.latest(context)

        assertNull(result)
    }
}
