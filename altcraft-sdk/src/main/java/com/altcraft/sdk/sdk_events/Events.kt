package com.altcraft.sdk.sdk_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.additional.StringBuilder.eventLogBuilder
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.extension.ExceptionExtension

/**
 * Handles SDK event emission, logging, and error reporting with a single active subscriber.
 */
@Keep
object Events {

    /**
     * The single event subscriber.
     * Only one subscriber can receive events at a time.
     */
    private var subscriber: ((DataClasses.Event) -> Unit)? = null

    /**
     * Subscribes to event notifications, replacing any existing subscriber.
     *
     * @param newSubscriber Lambda invoked for each emitted event (success, error, retry).
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
     * @param function Name of the function emitting the event.
     * @param event Pair of (code, message).
     * @param value Optional payload to attach to the event.
     * @return The emitted [DataClasses.Event].
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
     * Emits a non-retryable error event and logs it.
     *
     * @param function Name of the function emitting the error.
     * @param error Error object or message (String, SDKException, Exception, Pair<Int, String>).
     * @param value Optional payload to attach to the error.
     * @return The emitted [DataClasses.Error].
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
     * @param function Name of the function emitting the error.
     * @param error Error object or message (String, SDKException, Exception, Pair<Int, String>).
     * @param value Optional payload to attach to the error.
     * @return The emitted [DataClasses.RetryError].
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
     * Extracts an error code and message from the given error object.
     *
     * @param err May be a String, SDKException, Exception, Pair<Int, String>, or any object.
     * @param retry If true, generic exceptions default to code 500; otherwise 400.
     * @return A pair where the first is the error code (nullable; may be 0 for unknown),
     * and the second is the message.
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