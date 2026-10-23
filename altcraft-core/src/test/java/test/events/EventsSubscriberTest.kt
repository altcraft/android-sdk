@file:Suppress("SpellCheckingInspection")

package test.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.Events
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * EventsSubscriberTest
 *
 * Verifies that a subscriber throwing Error (app code) never propagates
 * out of event emission and therefore cannot kill SDK threads such as
 * command queue consumers.
 *
 * Negative scenarios:
 *  - test_1: Events.event() with throwing subscriber does not throw.
 *  - test_2: Events.error() with throwing subscriber does not throw.
 *  - test_3: Events.retry() with throwing subscriber does not throw.
 */
class EventsSubscriberTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        Events.Subscription.unsubscribe()
        unmockkAll()
    }

    /** - test_1: Events.event() with throwing subscriber does not throw. */
    @Test
    fun event_subscriberError_doesNotPropagate() {
        Events.Subscription.subscribe { throw NoClassDefFoundError("app bug") }

        Events.event("fn", CoreEvents.configIsSet)
    }

    /** - test_2: Events.error() with throwing subscriber does not throw. */
    @Test
    fun error_subscriberError_doesNotPropagate() {
        Events.Subscription.subscribe { throw NoClassDefFoundError("app bug") }

        Events.error("fn", RuntimeException("boom"))
    }

    /** - test_3: Events.retry() with throwing subscriber does not throw. */
    @Test
    fun retry_subscriberError_doesNotPropagate() {
        Events.Subscription.subscribe { throw NoClassDefFoundError("app bug") }

        Events.retry("fn", RuntimeException("boom"))
    }
}
