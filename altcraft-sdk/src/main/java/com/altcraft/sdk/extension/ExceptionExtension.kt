package com.altcraft.sdk.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

/**
 * `ExceptionExtension` defines SDK-specific exception handling utilities.
 *
 * Provides a custom `SDKException` class and a method to throw it from error pairs.
 */
internal object ExceptionExtension {
    /**
     * Throws an `SDKException` using the provided error pair.
     *
     * @param error A pair where the first value is the error code and the second is the error message.
     * @throws SDKException Always throws an instance of `SDKException`.
     */
    fun exception(error: Pair<Int, String>): Nothing {
        throw SDKException(error)
    }

    /**
     * Custom exception class that encapsulates an error code and a message.
     *
     * @property error A pair containing the error code and message.
     */
    class SDKException(private val error: Pair<Int, String>) : Exception(error.second) {
        /**
         * Retrieves the error pair containing the code and message.
         *
         * @return The original error pair (`Pair<String, String>`).
         */
        fun info(): Pair<Int, String> = error
    }
}