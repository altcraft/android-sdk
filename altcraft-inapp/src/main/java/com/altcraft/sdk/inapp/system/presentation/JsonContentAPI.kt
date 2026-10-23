package com.altcraft.sdk.inapp.system.presentation

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import kotlin.jvm.Volatile

/**
 * Provides subscription to in-app placements with JSON content.
 */
@Keep
object JsonContentAPI {

    /**
     * The single active subscriber for in-app placements with JSON content.
     *
     * @Volatile → written from the app's thread, read from SDK coroutines.
     */
    @Volatile
    private var subscriber: ((String) -> Unit)? = null

    /**
     * Registers a subscriber that will receive JSON content
     * from in-app placements.
     *
     * A new subscriber replaces the previous one.
     *
     * @param newSubscriber callback invoked when JSON content is received.
     */
    fun subscribe(newSubscriber: (String) -> Unit) {
        subscriber = newSubscriber
    }

    /**
     * Removes the current subscriber.
     */
    fun unsubscribe() {
        subscriber = null
    }

    /**
     * Delivers JSON content from an in-app placement to the current subscriber.
     *
     * @param json JSON content received from the in-app placement.
     */
    internal fun emit(json: String) = subscriber?.invoke(
        json
    )
}