package test.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.DataClasses

/**
 * EventProbe
 *
 * A thread-safe event collector for instrumented tests.
 * - Call subscribe() in @Before to start capturing events.
 * - Call unsubscribe() in @After to stop capturing.
 * - Use snapshot()/lastOrNull() for assertions.
 */
class EventProbe : (DataClasses.Event) -> Unit {
    private val items = mutableListOf<DataClasses.Event>()

    @Synchronized
    override fun invoke(e: DataClasses.Event) {
        items.add(e)
    }

    @Synchronized
    fun snapshot(): List<DataClasses.Event> = items.toList()

    @Synchronized
    fun lastOrNull(): DataClasses.Event? = items.lastOrNull()

    @Synchronized
    fun clear() { items.clear() }
}