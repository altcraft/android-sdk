package com.altcraft.sdk.sdk_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.StringBuilder.eventLogBuilder
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.extension.ExceptionExtension

/**
 * `Events` handles SDK event emission, logging, and error reporting with a single active subscriber.
 */
object Events {
    /**
     * The single event subscriber.
     * Only one subscriber can receive events at a time.
     */
    private var subscriber: ((DataClasses.Event) -> Unit)? = null

    /**
     * Subscribes to event notifications, replacing any existing subscriber.
     *
     * @param newSubscriber The lambda function that will receive events.
     */
    fun subscribe(newSubscriber: (DataClasses.Event) -> Unit) {
        subscriber = newSubscriber
    }

    /**
     * Unsubscribes the current subscriber.
     */
    fun unsubscribe() {
        subscriber = null
    }

    /**
     * Emits a general event and logs it.
     *
     * @param function Pair of function name and event ID.
     * @param event Optional event message.
     * @param value Optional event data.
     */
    internal fun event(
        function: String,
        event: Pair<Int, String>,
        value: Map<String, Any?>? = null,
    ): DataClasses.Event {
        logger(eventLogBuilder(function, event.second))

        return DataClasses.Event(function, event.first, event.second, value).also {
            subscriber?.invoke(it)
        }
    }

    /**
     * Emits an error event and logs it.
     *
     * @param function Pair of function name and event ID.
     * @param error Error object or message.
     * @param value Optional error data.
     */
    internal fun error(
        function: String,
        error: Any?,
        value: Map<String, Any?>? = null
    ): DataClasses.Error {
        val (code, message) = extractErrorDetails(error, false)

        logger(eventLogBuilder(function, message))

        return DataClasses.Error(function, code, message, value).also {
            subscriber?.invoke(it)
        }
    }

    /**
     * Emits a retryable error event and logs it.
     *
     * @param function Pair of function name and event ID.
     * @param error Error object or message.
     * @param value Optional retry data.
     */
    internal fun retry(
        function: String,
        error: Any?,
        value: Map<String, Any?>? = null,
    ): DataClasses.RetryError {
        val (code, message) = extractErrorDetails(error, true)

        logger(eventLogBuilder(function, message))

        return DataClasses.RetryError(function, code, message, value).also {
            subscriber?.invoke(it)
        }
    }

    /**
     * Extracts error code and message from the given error object.
     *
     * @param err Can be a `String`, `SDKException`, `Exception`, or any object.
     * @return A pair where the first is the error code (nullable), and the second is the message.
     */
    private fun extractErrorDetails(err: Any?, retry: Boolean): Pair<Int?, String> {
        return when (err) {
            is ExceptionExtension.SDKException -> err.info()
            is Pair<*, *> -> ((err.first as? Int) ?: 0) to (err.second?.toString() ?: "")
            is Exception -> (if (retry) 500 else 400) to (err.localizedMessage ?: err.toString())
            else -> 0 to err.toString()
        }
    }
}