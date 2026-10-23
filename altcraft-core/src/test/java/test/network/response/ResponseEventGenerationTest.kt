package test.network.response

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.ResponseModel
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.AsyncResponse
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseEventValue
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.response.Response
import com.altcraft.sdk.network.response.Response.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

/**
 * ResponseEventGenerationTest
 *
 * Verifies that [Response.eventGeneration] applies the request module tag
 * to the generated response event function name.
 */
class ResponseEventGenerationTest {

    @After
    fun tearDown() = unmockkAll()

    private fun result(
        status: ResponseStatus,
        moduleLogPrefix: String,
    ) = ResponseModel.ResponseResult(
        status = status,
        responseData = AsyncResponse(),
        httpCode = 200,
        value = ResponseEventValue(
            eventValue = null,
            loggerValue = null,
        ),
        moduleLogPrefix = moduleLogPrefix,
        errorEvent = CoreEvents.configIsNull,
        successEvent = CoreEvents.configIsSet,
    )

    @Test
    fun `eventGeneration prefixes success with module tag`() {
        mockkObject(Events)
        every { Events.event(any(), any()) } returns mockk(relaxed = true)

        Response.eventGeneration(
            result(ResponseStatus.SUCCESS, "[altcraft-inapp] ")
        )

        verify(exactly = 1) {
            Events.event(eq("[altcraft-inapp] processResponse"), eq(CoreEvents.configIsSet))
        }
    }

    @Test
    fun `eventGeneration defaults to core prefix`() {
        mockkObject(Events)
        every { Events.event(any(), any()) } returns mockk(relaxed = true)

        Response.eventGeneration(
            result(ResponseStatus.SUCCESS, "[altcraft-core] ")
        )

        verify(exactly = 1) {
            Events.event(eq("[altcraft-core] processResponse"), eq(CoreEvents.configIsSet))
        }
    }

    @Test
    fun `eventGeneration prefixes retry with module tag and error event`() {
        mockkObject(Events)
        every { Events.retry(any(), any()) } returns mockk(relaxed = true)

        Response.eventGeneration(
            result(ResponseStatus.RETRY, "[altcraft-push] ")
        )

        verify(exactly = 1) {
            Events.retry(eq("[altcraft-push] processResponse"), eq(CoreEvents.configIsNull))
        }
    }
}
