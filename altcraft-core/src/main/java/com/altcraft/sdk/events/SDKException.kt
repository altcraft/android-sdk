package com.altcraft.sdk.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * `SDKException` defines SDK-specific exception handling utilities.
 *
 * Provides a custom `SDKException` class and a method to throw it from SDK events.
 */
object SDKException {

    /**
     * Throws an `SDKException` using the provided SDK event.
     *
     * @param error SDK event.
     * @throws SDKException Always throws an instance of `SDKException`.
     */
    fun exception(error: SDKEvent): Nothing {
        throw SDKException(error)
    }

    /**
     * Custom exception class that encapsulates an SDK event.
     *
     * @property error SDK event.
     */
    class SDKException(private val error: SDKEvent) : Exception(
        error.message
    ) {

        /**
         * Retrieves the original SDK event.
         *
         * @return The original [CoreEvents].
         */
        internal fun info(): SDKEvent = error
    }
}
