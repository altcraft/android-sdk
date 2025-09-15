package test.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.extension.ExceptionExtension
import org.junit.*
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * EventsInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: event() delivers DataClasses.Event to subscriber with correct fields.
 *  - test_2: error() delivers DataClasses.Error with Exception.
 *  - test_3: retry() delivers DataClasses.RetryError with retry code.
 *  - test_5: error() with Pair<Int, String> handled correctly.
 *  - test_6: error() with SDKException handled correctly.
 *
 * Negative scenarios:
 *  - test_4: unsubscribe() prevents further events from reaching subscriber.
 *
 * Notes:
 *  - Runs on Android (instrumented test).
 *  - Uses EventProbe as thread-safe collector for emitted events.
 */
@RunWith(AndroidJUnit4::class)
class EventsInstrumentedTest {

    private lateinit var probe: EventProbe

    @Before
    fun setUp() {
        probe = EventProbe()
        Events.subscribe(probe)
    }

    @After
    fun tearDown() {
        Events.unsubscribe()
    }

    /** test_1: event() delivers DataClasses.Event */
    @Test
    fun event_deliversToSubscriber() {
        val ev = Events.event("fn", 100 to "ok", mapOf("x" to 1))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertEquals("fn", captured?.function)
        assertEquals(100, captured?.eventCode)
        assertEquals("ok", captured?.eventMessage)
        assertEquals(1, captured?.eventValue?.get("x"))
        assertTrue(captured is DataClasses.Event)
        assertEquals(ev.eventCode, captured?.eventCode)
    }

    /** test_2: error() delivers DataClasses.Error with Exception */
    @Test
    fun error_deliversErrorEvent() {
        val err = Events.error("fn", IllegalStateException("boom"))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is DataClasses.Error)
        assertEquals("fn", captured?.function)
        assertEquals(400, captured?.eventCode)
        assertTrue(captured?.eventMessage?.contains("boom") == true)
        assertEquals(err.eventMessage, captured?.eventMessage)
    }

    /** test_3: retry() delivers DataClasses.RetryError */
    @Test
    fun retry_deliversRetryErrorEvent() {
        val err = Events.retry("fn", IllegalArgumentException("retry"))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is DataClasses.RetryError)
        assertEquals("fn", captured?.function)
        assertEquals(500, captured?.eventCode)
        assertTrue(captured?.eventMessage?.contains("retry") == true)
        assertEquals(err.eventMessage, captured?.eventMessage)
    }

    /** test_4: unsubscribe() prevents delivery */
    @Test
    fun unsubscribe_stopsReceivingEvents() {
        Events.unsubscribe()
        Events.event("fn", 200 to "nope")

        val captured = probe.lastOrNull()
        assertEquals(null, captured)
    }

    /** test_5: error() with Pair<Int, String> */
    @Test
    fun error_withPairHandledCorrectly() {
        val err = Events.error("fn", 123 to "pair-msg")
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is DataClasses.Error)
        assertEquals(123, captured?.eventCode)
        assertEquals("pair-msg", captured?.eventMessage)
        assertEquals(err.eventCode, captured?.eventCode)
    }

    /** test_6: error() with SDKException */
    @Test
    fun error_withSdkExceptionHandledCorrectly() {
        val sdkEx = ExceptionExtension.SDKException(321 to "boom-sdk")
        val err = Events.error("fn", sdkEx)
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is DataClasses.Error)
        assertEquals(321, captured?.eventCode)
        assertEquals("boom-sdk", captured?.eventMessage)
        assertEquals(err.eventCode, captured?.eventCode)
    }
}
