@file:Suppress("SpellCheckingInspection")

package test.inapp.platform

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.system.presentation.JsonContentAPI
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * InAppEmitterTest
 *
 * Positive scenarios:
 *  - test_1: subscribe receives emitted JSON.
 *  - test_2: unsubscribe stops receiving emissions.
 *  - test_3: subscribe with a new subscriber replaces the old one.
 *
 * Negative scenarios:
 *  - test_4: emit with no subscriber does nothing (no crash).
 */
class InAppEmitterTest {

    @After
    fun tearDown() {
        JsonContentAPI.unsubscribe()
    }

    /** test_1: subscribed callback receives emitted JSON. */
    @Test
    fun test_1_subscribe_receivesEmittedJson() {
        val received = AtomicReference<String>()
        JsonContentAPI.subscribe { received.set(it) }
        JsonContentAPI.emit("""{"key":"value"}""")
        assertEquals("""{"key":"value"}""", received.get())
    }

    /** test_2: after unsubscribe, emissions are not received. */
    @Test
    fun test_2_unsubscribe_stopsReceiving() {
        val received = AtomicReference<String>()
        JsonContentAPI.subscribe { received.set(it) }
        JsonContentAPI.unsubscribe()
        JsonContentAPI.emit("should-not-reach")
        assertNull(received.get())
    }

    /** test_3: new subscriber replaces old one. */
    @Test
    fun test_3_newSubscriber_replacesOld() {
        val oldReceived = AtomicReference<String>()
        val newReceived = AtomicReference<String>()
        JsonContentAPI.subscribe { oldReceived.set(it) }
        JsonContentAPI.subscribe { newReceived.set(it) }
        JsonContentAPI.emit("only-new")
        assertNull("Old subscriber must not receive", oldReceived.get())
        assertEquals("only-new", newReceived.get())
    }

    /** test_4: emit without subscriber does not throw. */
    @Test
    fun test_4_emit_noSubscriber_doesNotThrow() {
        // Should not throw
        JsonContentAPI.emit("""{"any":"data"}""")
        JsonContentAPI.emit("""{"more":"data"}""")
    }
}
