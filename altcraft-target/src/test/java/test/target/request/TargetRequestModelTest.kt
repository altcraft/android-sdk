@file:Suppress("SpellCheckingInspection")

package test.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.data.models.TargetModels.UTM
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.platform.target.request.TargetRequestModel
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TargetRequestModelTest
 *
 * Verifies target request data construction.
 *
 * Positive scenarios:
 *  - `of()` builds a multipart request model with the fixed query and parts.
 *
 * Resilience:
 *  - `of()` still builds a valid request when the UTM payload is malformed.
 */
class TargetRequestModelTest {

    private val utmJson = Json.encodeToString(
        UTM(
            campaign = "campaign", content = "content", keyword = "keyword",
            medium = "medium", source = "source", temp = "temp"
        )
    )

    private val entity = Entity(
        userTag = "user-123",
        timeZone = 180,
        sid = "pixel-1",
        altcraftClientID = "client-id",
        eventName = "onAppOpen",
        payload = """{"a":1}""",
        matching = """{"m":1}""",
        matchingType = "1",
        profileFields = """{"p":1}""",
        subscription = """{"s":1}""",
        sendMessageId = "mid-1",
        utmTags = utmJson
    )

    @Before
    fun setUp() {
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
        every { Events.retry(any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** `of()` builds a complete multipart request with the fixed query. */
    @Test
    fun of_buildsMultipartRequestData() {
        val model = TargetRequestModel.of(entity)

        assertNotNull(model)
        assertEquals("/event/post", model!!.url)
        assertEquals(entity.requestID, model.requestId)
        assertEquals("pixel-1", model.sid)
        assertEquals("onAppOpen", model.name)

        val request = model.requestModel
        assertTrue(request is GenericRequest.MultipartPost)
        request as GenericRequest.MultipartPost
        assertEquals(entity.requestID, request.requestId)
        assertEquals(
            mapOf("i" to "pixel-1", "tr" to "px", "t" to "open", "v" to "2"),
            request.query
        )
        assertEquals(16, request.parts.size)
    }

    /** `of()` still builds a valid request when UTM JSON is malformed. */
    @Test
    fun of_stillBuildsRequest_whenUtmMalformed() {
        val broken = entity.copy(utmTags = "not-a-json")

        val model = TargetRequestModel.of(broken)

        assertNotNull("Malformed UTM must not abort request building", model)
        assertEquals("pixel-1", model!!.sid)
        assertTrue(model.requestModel is GenericRequest.MultipartPost)
        // All six UTM parts are dropped; the remaining ten are still present.
        assertEquals(10, (model.requestModel as GenericRequest.MultipartPost).parts.size)
    }
}
