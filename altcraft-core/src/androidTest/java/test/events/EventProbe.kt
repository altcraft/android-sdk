package test.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.SDKEventModel

/**
 * EventProbe
 *
 * A thread-safe event collector for instrumented tests.
 * - Call subscribe() in @Before to start capturing events.
 * - Call unsubscribe() in @After to stop capturing.
 * - Use snapshot()/lastOrNull() for assertions.
 */
class EventProbe : (SDKEventModel.Event) -> Unit {
    private val items = mutableListOf<SDKEventModel.Event>()

    @Synchronized
    override fun invoke(e: SDKEventModel.Event) {
        items.add(e)
    }

    @Synchronized
    fun snapshot(): List<SDKEventModel.Event> = items.toList()

    @Synchronized
    fun lastOrNull(): SDKEventModel.Event? = items.lastOrNull()

    @Synchronized
    fun clear() { items.clear() }
}