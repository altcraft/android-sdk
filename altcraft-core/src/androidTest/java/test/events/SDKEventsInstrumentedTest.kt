package test.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.SDKException
import org.junit.*
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * EventsInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: event() delivers SDKEventModel.Event to subscriber with correct fields.
 *  - test_2: error() delivers SDKEventModel.Error with Exception.
 *  - test_3: retry() delivers SDKEventModel.RetryError.
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
        Events.Subscription.subscribe(probe)
    }

    @After
    fun tearDown() {
        Events.Subscription.unsubscribe()
    }

    /** test_1: event() delivers SDKEventModel.Event */
    @Test
    fun event_deliversToSubscriber() {
        val ev = Events.event("fn", CoreEvents.configIsSet, eventValue = mapOf("x" to 1))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertEquals("fn", captured?.function)
        assertEquals(CoreEvents.configIsSet, captured?.event)
        assertEquals(CoreEvents.configIsSet.message, captured?.eventMessage)
        assertEquals(1, captured?.eventValue?.get("x"))
        assertTrue(captured is SDKEventModel.Event)
        assertEquals(ev.event, captured?.event)
    }

    /** test_2: error() delivers SDKEventModel.Error with Exception */
    @Test
    fun error_deliversErrorEvent() {
        val err = Events.error("fn", IllegalStateException("boom"))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is SDKEventModel.Error)
        assertEquals("fn", captured?.function)
        assertNull(captured?.event)
        assertTrue(captured?.eventMessage?.contains("boom") == true)
        assertEquals(err.eventMessage, captured?.eventMessage)
    }

    /** test_3: retry() delivers SDKEventModel.RetryError */
    @Test
    fun retry_deliversRetryErrorEvent() {
        val err = Events.retry("fn", IllegalArgumentException("retry"))
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is SDKEventModel.RetryError)
        assertEquals("fn", captured?.function)
        assertNull(captured?.event)
        assertTrue(captured?.eventMessage?.contains("retry") == true)
        assertEquals(err.eventMessage, captured?.eventMessage)
    }

    /** test_4: unsubscribe() prevents delivery */
    @Test
    fun unsubscribe_stopsReceivingEvents() {
        Events.Subscription.unsubscribe()
        Events.event("fn", CoreEvents.configIsSet)

        val captured = probe.lastOrNull()
        assertEquals(null, captured)
    }

    /** test_5: error() with Pair<Int, String> */
    @Test
    fun error_withPairHandledCorrectly() {
        val err = Events.error("fn", 123 to "pair-msg")
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is SDKEventModel.Error)
        assertNull(captured?.event)
        assertTrue(captured?.eventMessage?.contains("123") == true)
        assertTrue(captured?.eventMessage?.contains("pair-msg") == true)
    }

    /** test_6: error() with SDKException */
    @Test
    fun error_withSdkExceptionHandledCorrectly() {
        val sdkEx = SDKException.SDKException(CoreEvents.configIsNotSet)
        val err = Events.error("fn", sdkEx)
        val captured = probe.lastOrNull()

        assertNotNull(captured)
        assertTrue(captured is SDKEventModel.Error)
        assertEquals(CoreEvents.configIsNotSet, captured?.event)
        assertEquals(CoreEvents.configIsNotSet.message, captured?.eventMessage)
        assertEquals(err.event, captured?.event)
    }
}
