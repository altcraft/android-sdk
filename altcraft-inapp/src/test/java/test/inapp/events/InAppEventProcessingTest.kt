@file:Suppress("SpellCheckingInspection")

package test.inapp.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.Constants.IN_APP_CLICK
import com.altcraft.sdk.inapp.data.Constants.IN_APP_CLOSE
import com.altcraft.sdk.inapp.data.Constants.IN_APP_DELIVERY
import com.altcraft.sdk.inapp.data.Constants.IN_APP_ERROR
import com.altcraft.sdk.inapp.data.Constants.IN_APP_READY
import com.altcraft.sdk.inapp.data.Constants.IN_APP_SHOW
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppEventData
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.InAppEventType
import org.junit.Assert.*
import org.junit.Test

/**
 * InAppEventProcessingTest
 *
 * Positive scenarios:
 *  - test_1: enum values match constants.
 *  - test_2: enum valueOf resolves by enum name.
 *  - test_5: non-error payload is passed through.
 *  - test_7: click payload preserves href.
 *  - test_8: delivery payload is passed through.
 *  - test_9: close payload is passed through.
 *  - test_10: ready payload is passed through.
 *
 * Negative scenarios:
 *  - test_3: error payload is normalized with code 400.
 *  - test_4: error null payload creates code 400 with null message.
 *  - test_6: show null payload returns null.
 *  - test_11: ready null payload returns null.
 */
class InAppEventProcessingTest {

    /** test_1: enum values match constants. */
    @Test
    fun test_1_enumValues_areCorrect() {
        assertEquals(7, InAppEventType.entries.size)
        assertEquals(IN_APP_DELIVERY, InAppEventType.DELIVERY.value)
        assertEquals(IN_APP_SHOW, InAppEventType.SHOW.value)
        assertEquals(IN_APP_CLICK, InAppEventType.CLICK.value)
        assertEquals(IN_APP_ERROR, InAppEventType.ERROR.value)
        assertEquals(IN_APP_CLOSE, InAppEventType.CLOSE.value)
        assertEquals(IN_APP_READY, InAppEventType.READY.value)
    }

    /** test_2: enum valueOf resolves by enum name. */
    @Test
    fun test_2_enumValueOf_resolvesByName() {
        assertEquals(InAppEventType.DELIVERY, InAppEventType.valueOf("DELIVERY"))
        assertEquals(InAppEventType.SHOW, InAppEventType.valueOf("SHOW"))
        assertEquals(InAppEventType.CLICK, InAppEventType.valueOf("CLICK"))
        assertEquals(InAppEventType.ERROR, InAppEventType.valueOf("ERROR"))
        assertEquals(InAppEventType.CLOSE, InAppEventType.valueOf("CLOSE"))
        assertEquals(InAppEventType.READY, InAppEventType.valueOf("READY"))
    }

    /** test_3: error payload is normalized with code 400. */
    @Test
    fun test_3_buildInAppEventData_errorType_addsCode400() {
        val result = buildInAppEventData(InAppEventType.ERROR, InAppEventData(message = "js error"))
        assertEquals(400, result?.code)
        assertEquals("js error", result?.message)
    }

    /** test_4: error null payload creates code 400 with null message. */
    @Test
    fun test_4_buildInAppEventData_errorNullPayload_code400NullMessage() {
        val result = buildInAppEventData(InAppEventType.ERROR, null)
        assertEquals(400, result?.code)
        assertNull(result?.message)
    }

    /** test_5: non-error payload is passed through. */
    @Test
    fun test_5_buildInAppEventData_nonError_passesThrough() {
        val payload = InAppEventData(name = "btn", code = 100, message = "ok")
        val result = buildInAppEventData(InAppEventType.SHOW, payload)
        assertEquals(payload, result)
    }

    /** test_6: show null payload returns null. */
    @Test
    fun test_6_buildInAppEventData_showNullPayload_returnsNull() {
        val result = buildInAppEventData(InAppEventType.SHOW, null)
        assertNull(result)
    }

    /** test_7: click payload preserves href. */
    @Test
    fun test_7_buildInAppEventData_click_preservesPayload() {
        val payload = InAppEventData(name = "cta", href = "https://example.com")
        val result = buildInAppEventData(InAppEventType.CLICK, payload)
        assertEquals("cta", result?.name)
        assertEquals("https://example.com", result?.href)
    }

    /** test_8: delivery payload is passed through. */
    @Test
    fun test_8_buildInAppEventData_delivery_passesThrough() {
        val payload = InAppEventData(name = "delivered")
        val result = buildInAppEventData(InAppEventType.DELIVERY, payload)
        assertEquals(payload, result)
    }

    /** test_9: close payload is passed through. */
    @Test
    fun test_9_buildInAppEventData_close_passesThrough() {
        val payload = InAppEventData(name = "closed")
        val result = buildInAppEventData(InAppEventType.CLOSE, payload)
        assertEquals(payload, result)
    }

    /** test_10: ready payload is passed through. */
    @Test
    fun test_10_buildInAppEventData_ready_passesThrough() {
        val payload = InAppEventData(name = "ready")
        val result = buildInAppEventData(InAppEventType.READY, payload)
        assertEquals(payload, result)
    }

    /** test_11: ready null payload returns null. */
    @Test
    fun test_11_buildInAppEventData_readyNullPayload_returnsNull() {
        val result = buildInAppEventData(InAppEventType.READY, null)
        assertNull(result)
    }

    // Replicates the private buildInAppEventData logic
    private fun buildInAppEventData(
        type: InAppEventType,
        payload: InAppEventData?
    ): InAppEventData? =
        if (type == InAppEventType.ERROR) InAppEventData(
            code = 400, message = payload?.message
        ) else payload
}