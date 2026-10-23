@file:Suppress("SpellCheckingInspection")

package test.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.network.response.Response.ApiResponse
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.events.ModuleEvents
import com.altcraft.sdk.target.platform.target.request.TargetRequest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.altcraft.sdk.target.data.models.TargetModels.UTM as TargetUTM

/**
 * TargetRequestTest
 *
 * Verifies the target request execution path.
 *
 * Positive scenarios:
 *  - a valid entity performs a network request and returns its resulting event.
 *
 * Negative scenarios:
 *  - an entity whose request data cannot be built degrades to a retryable error.
 */
class TargetRequestTest {

    private val context: Context = mockk(relaxed = true)

    private val utmJson = Json.encodeToString(
        TargetUTM(campaign = "campaign", source = "source", medium = "medium")
    )

    @Before
    fun setUp() {
        mockkObject(Events)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun entity() = Entity(
        userTag = "user-123",
        timeZone = 180,
        sid = "pixel-1",
        altcraftClientID = "client-id",
        eventName = "onAppOpen",
        payload = null,
        matching = null,
        matchingType = "1",
        profileFields = null,
        subscription = null,
        sendMessageId = null,
        utmTags = utmJson
    )

    /** A valid entity is sent through the request executor and returns its event. */
    @Test
    fun targetRequest_validEntity_returnsExecutorEvent() = runTest {
        val apiResponse = ApiResponse<ResponseModel.AsyncResponse>(
            event = SDKEventModel.Event(
                function = "target",
                event = ModuleEvents.requestSuccessful
            )
        )

        mockkObject(RequestExecutor)
        coEvery {
            RequestExecutor.execute<ResponseModel.AsyncResponse>(any(), any())
        } returns apiResponse

        val result = TargetRequest.targetRequest(context, entity())

        assertEquals(ModuleEvents.requestSuccessful, result.event)
    }

    /** Unbuildable request data is downgraded to a retryable error, never thrown. */
    @Test
    fun targetRequest_unbuildableRequestData_returnsRetryableError() = runTest {
        val broken = entity().copy(utmTags = "not-a-json")

        val result = TargetRequest.targetRequest(context, broken)

        assertTrue("Expected a retryable error for broken request data", result is SDKEventModel.RetryError)
    }
}
