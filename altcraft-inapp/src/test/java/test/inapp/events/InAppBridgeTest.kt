@file:Suppress("SpellCheckingInspection")

package test.inapp.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.system.presentation.bridge.InAppBridge
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * InAppBridgeTest
 *
 * Positive scenarios:
 *  - test_1: sendEvent invokes the callback with the provided JSON string.
 *
 * Negative scenarios:
 *  - test_2: sendEvent with empty string invokes callback with empty string.
 */
class InAppBridgeTest {

    /** test_1: sendEvent delivers JSON to callback. */
    @Test
    fun test_1_sendEvent_deliversJsonToCallback() {
        val received = AtomicReference<String>()
        val bridge = InAppBridge { received.set(it) }
        bridge.sendEvent("""{"event":"click"}""")
        assertEquals("""{"event":"click"}""", received.get())
    }

    /** test_2: sendEvent with empty string calls callback with empty. */
    @Test
    fun test_2_sendEvent_emptyString() {
        val received = AtomicReference<String>()
        val bridge = InAppBridge { received.set(it) }
        bridge.sendEvent("")
        assertEquals("", received.get())
    }
}
