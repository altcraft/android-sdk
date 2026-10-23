@file:Suppress("SpellCheckingInspection")

package test.profile.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.network.request.GenericRequest
import com.altcraft.sdk.profile.data.room.Entity
import com.altcraft.sdk.profile.platform.updateFields.request.UpdateFieldsRequestModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UpdateFieldsRequestModelTest
 *
 * Verifies profile update request model construction.
 *
 * Positive scenarios:
 *  - `of()` builds a JSON POST request carrying `profile_fields` and `skip_triggers`.
 *  - present profile fields are wrapped into a JSON request body.
 *
 * Negative scenarios:
 *  - `of()` returns `null` when the request data cannot be built.
 */
class UpdateFieldsRequestModelTest {

    private val entity = Entity(
        userTag = "user-123",
        profileFields = Json.parseToJsonElement("""{"a":1,"b":"x"}""").jsonObject,
        skipTriggers = true
    )

    /** `of()` builds a JSON POST request with the expected body. */
    @Test
    fun of_buildsJsonPostRequest() {
        val model = UpdateFieldsRequestModel.of(entity)

        assertNotNull(model)
        assertEquals("/profile/update", model!!.url)
        assertEquals(entity.requestID, model.requestId)
        assertEquals(entity.profileFields, model.profileFields)
        assertEquals(true, model.skipTriggers)

        val request = model.requestModel
        assertTrue(request is GenericRequest.JsonPost)
        request as GenericRequest.JsonPost
        assertEquals("/profile/update", request.url)
        assertEquals(entity.requestID, request.requestId)

        val body = request.body
        assertEquals("""{"a":1,"b":"x"}""", body["profile_fields"].toString())
        assertEquals(true, body["skip_triggers"]?.jsonPrimitive?.booleanOrNull)
    }

    /** `of()` carries null profile fields into the request body. */
    @Test
    fun of_roundTripsNullProfileFields() {
        val nullable = Entity(
            userTag = "user-123",
            profileFields = null,
            skipTriggers = null
        )

        val model = UpdateFieldsRequestModel.of(nullable)

        assertNotNull(model)
        assertTrue(model!!.requestModel is GenericRequest.JsonPost)
        val body = (model.requestModel as GenericRequest.JsonPost).body
        assertEquals("null", body["profile_fields"].toString())
        assertEquals("null", body["skip_triggers"]!!.jsonPrimitive.content)
    }
}
