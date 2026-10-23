@file:Suppress("SpellCheckingInspection")

package test.inapp.placements

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.data.Constants.ACID
import com.altcraft.sdk.inapp.data.Constants.DEVICE_ID
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_REQUEST
import com.altcraft.sdk.inapp.data.Constants.LAST_PUBLISH
import com.altcraft.sdk.inapp.data.Constants.PLACEMENT_RULES_CHECKSUM
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel.ResponseWithInApps
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.placements.request.PlacementsRequest
import com.altcraft.sdk.inapp.platform.placements.request.PlacementsRequestModel
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.network.request.RequestExecutor
import com.altcraft.sdk.network.response.Response.ApiResponse
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PlacementsRequestTest
 *
 * Positive scenarios:
 *  - test_1: the request model maps state fields and the endpoint path.
 *  - test_2: a null request id is replaced with a generated one.
 *  - test_3: the transport model is a JSON POST with path, empty query and JSON body.
 *  - test_4: request metadata exposes the module request name and events.
 *  - test_5: the request delegates to the request executor and returns its response.
 *
 * Negative scenarios:
 *  - test_6: an executor failure is converted into a retry response instead of a crash.
 */
class PlacementsRequestTest {

    private lateinit var context: Context
    private lateinit var state: InAppsEntity

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        state = InAppsEntity(
            acid = "acid-1",
            deviceId = "device-1",
            lastPublish = 123L,
            placementRulesChecksum = "checksum-1"
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: model maps state fields and the endpoint path. */
    @Test
    fun test_1_of_mapsStateFieldsAndUrl() {
        val model = PlacementsRequestModel.of(state, "request-1")

        assertNotNull(model)
        assertEquals("/inapp/placements", model?.url)
        assertEquals("request-1", model?.requestId)
        assertEquals("acid-1", model?.acid)
        assertEquals("device-1", model?.deviceId)
        assertEquals(123L, model?.lastPublish)
        assertEquals("checksum-1", model?.placementRulesChecksum)
    }

    /** test_2: a null request id is replaced with a generated one. */
    @Test
    fun test_2_of_nullRequestId_generatesOne() {
        val model = PlacementsRequestModel.of(state, null)

        assertNotNull(model)
        assertTrue(model?.requestId?.isNotBlank() == true)
    }

    /** test_3: the transport model is a JSON POST with path, empty query and JSON body. */
    @Test
    fun test_3_requestModel_isJsonPostWithBody() {
        val model = PlacementsRequestModel.of(state, "request-1")!!
        val request = model.requestModel

        assertTrue(request is GenericRequest.JsonPost)

        request as GenericRequest.JsonPost
        assertEquals("/inapp/placements", request.url)
        assertEquals("request-1", request.requestId)
        assertEquals(emptyMap<String, String?>(), request.query)

        assertEquals("acid-1", request.body[ACID]?.jsonPrimitive?.content)
        assertEquals("device-1", request.body[DEVICE_ID]?.jsonPrimitive?.content)
        assertEquals("123", request.body[LAST_PUBLISH]?.jsonPrimitive?.content)
        assertEquals(
            "checksum-1",
            request.body[PLACEMENT_RULES_CHECKSUM]?.jsonPrimitive?.content
        )
    }

    /** test_4: request metadata exposes the module request name and events. */
    @Test
    fun test_4_requestMetadata_exposesNameAndEvents() {
        val model = PlacementsRequestModel.of(state, "request-1")!!

        assertEquals(IN_APP_PLACEMENTS_REQUEST, model.requestName)
        assertEquals(ModuleEvents.placementsRequestSuccessful, model.successEvent)
        assertEquals(ModuleEvents.placementsRequestFailed, model.failedEvent)
    }

    /** test_5: the request delegates to the request executor and returns its response. */
    @Test
    fun test_5_inAppPlacementsRequest_returnsExecutorResponse() = runTest {
        mockkObject(RequestExecutor)
        val response = ApiResponse<ResponseWithInApps>(
            httpCode = 200,
            event = SDKEventModel.Event("placementsRequest")
        )

        coEvery {
            RequestExecutor.execute<ResponseWithInApps>(context, any())
        } returns response

        val result = PlacementsRequest.inAppPlacementsRequest(
            context, state, "request-1"
        )

        assertEquals(200, result.httpCode)
        coVerify(exactly = 1) {
            RequestExecutor.execute<ResponseWithInApps>(context, any())
        }
    }

    /** test_6: an executor failure is converted into a retry response instead of a crash. */
    @Test
    fun test_6_executorThrows_returnsRetryResponse() = runTest {
        mockkObject(RequestExecutor)
        mockkObject(Events)
        every { Events.retry(any(), any()) } returns mockk<SDKEventModel.RetryError>()

        coEvery {
            RequestExecutor.execute<ResponseWithInApps>(context, any())
        } throws RuntimeException("boom")

        val result = PlacementsRequest.inAppPlacementsRequest(
            context, state, "request-1"
        )

        assertNotNull(result.event)
        assertTrue(result.event is SDKEventModel.RetryError)
        verify(exactly = 1) {
            Events.retry("[altcraft-inapp] inAppPlacementsRequest", any())
        }
    }
}
