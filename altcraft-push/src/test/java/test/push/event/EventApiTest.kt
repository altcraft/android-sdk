@file:Suppress("SpellCheckingInspection")

package test.push.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.platform.event.EventAPI
import com.altcraft.sdk.push.platform.event.EventProcess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * EventApiTest
 *
 * Verifies delivery/open event reporting through the public push event API.
 *
 * Positive scenarios:
 *  - Altcraft messages report the expected event type and UID.
 *  - An explicit UID takes priority over the message payload.
 *
 * Negative scenarios:
 *  - Messages without the Altcraft marker are ignored.
 */
class EventApiTest {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(EventProcess)
        coEvery { EventProcess.sendPushEvent(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun altcraftMessage(uid: String) = mapOf(
        "_ac_push" to "1",
        "_uid" to uid
    )

    /** Altcraft messages report a delivery event with the payload UID. */
    @Test
    fun deliveryEvent_reportsDeliveryWithUid() = runTest {
        EventAPI.deliveryEvent(context, altcraftMessage("msg-111"))

        coVerify(exactly = 1) {
            EventProcess.sendPushEvent(context, EventAPI.EventType.DELIVERY, "msg-111")
        }
    }

    /** Altcraft messages report an open event with the payload UID. */
    @Test
    fun openEvent_reportsOpenWithUid() = runTest {
        EventAPI.openEvent(context, altcraftMessage("msg-222"))

        coVerify(exactly = 1) {
            EventProcess.sendPushEvent(context, EventAPI.EventType.OPEN, "msg-222")
        }
    }

    /** The explicit UID takes priority over the payload UID. */
    @Test
    fun deliveryEvent_explicitUidOverridesPayload() = runTest {
        EventAPI.deliveryEvent(context, altcraftMessage("msg-payload"), messageUID = "msg-explicit")

        coVerify(exactly = 1) {
            EventProcess.sendPushEvent(context, EventAPI.EventType.DELIVERY, "msg-explicit")
        }
    }

    /** A null message with an explicit UID still reports the event. */
    @Test
    fun deliveryEvent_nullMessageUsesExplicitUid() = runTest {
        EventAPI.deliveryEvent(context, message = null, messageUID = "msg-only")

        coVerify(exactly = 1) {
            EventProcess.sendPushEvent(context, EventAPI.EventType.DELIVERY, "msg-only")
        }
    }

    /** Messages without the Altcraft marker are ignored for delivery. */
    @Test
    fun deliveryEvent_nonAltcraftMessageIsIgnored() = runTest {
        EventAPI.deliveryEvent(context, mapOf("_uid" to "foreign-msg"))

        coVerify(exactly = 0) { EventProcess.sendPushEvent(any(), any(), any()) }
    }

    /** Messages without the Altcraft marker are ignored for opens. */
    @Test
    fun openEvent_nonAltcraftMessageIsIgnored() = runTest {
        EventAPI.openEvent(context, mapOf("_uid" to "foreign-msg"))

        coVerify(exactly = 0) { EventProcess.sendPushEvent(any(), any(), any()) }
    }
}
