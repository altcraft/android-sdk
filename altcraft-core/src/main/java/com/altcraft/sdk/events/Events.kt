package com.altcraft.sdk.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Logger.log
import com.altcraft.sdk.util.StringBuilder.eventLogBuilder
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Error
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.RetryError
import kotlin.jvm.Volatile

/**
 * Provides public access to SDK events and event subscriptions.
 */
@Keep
object SDKEvents {

    /**
     * Provides access to SDK event subscription management.
     */
    val subscription = Events.Subscription

    /**
     * Provides the list of core SDK events.
     */
    val moduleEvents = CoreEvents
}

/**
 * Handles SDK event emission, logging, and error reporting with a single active subscriber.
 */
@Keep
object Events {

    /**
     * The single event subscriber.
     * Only one subscriber can receive events at a time.
     */
    @Volatile
    private var subscriber: ((Event) -> Unit)? = null

    /**
     * Manages subscription to SDK events.
     *
     * Only one subscriber can be active at a time.
     * A new subscription replaces the existing one.
     */
    object Subscription {
        /**
         * Subscribes to event notifications, replacing any existing subscriber.
         *
         * @param newSubscriber Lambda invoked for each emitted event.
         */
        fun subscribe(newSubscriber: (Event) -> Unit) {
            subscriber = newSubscriber
        }

        /**
         * Unsubscribes the current subscriber.
         */
        fun unsubscribe() {
            subscriber = null
        }
    }

    /**
     * Emits an SDK event and logs it.
     *
     * @param function Name of the function emitting the event.
     * @param event SDK or module event.
     * @param eventValue Optional payload attached to the event.
     * @param logValue Optional values included only in the log message.
     * @return The emitted [Event].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun event(
        function: String,
        event: SDKEvent,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): Event {
        val evMessage = eventLogBuilder(event.message, logValue)

        log(buildString { append("$function():"); append(" $evMessage") })

        return Event(function, event, evMessage, eventValue).also {
            try {
                subscriber?.invoke(it)
            } catch (e: Throwable) {
                log("events subscriber error: ${e.message}")
            }
        }
    }

    /**
     * Emits a non-retryable error and logs it.
     *
     * @param function Name of the function emitting the error.
     * @param error Error object, message, exception, or SDK event.
     * @param eventValue Optional payload attached to the error.
     * @param logValue Optional values included only in the log message.
     * @return The emitted [Error].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun error(
        function: String,
        error: Any?,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): Error {
        val (event, message) = extractErrorDetails(error)
        val evMessage = eventLogBuilder(message, logValue)

        log(buildString { append("$function():"); append(" $evMessage") })

        return Error(function, event, evMessage, eventValue).also {
            try {
                subscriber?.invoke(it)
            } catch (e: Throwable) {
                log("events subscriber error: ${e.message}")
            }
        }
    }

    /**
     * Emits a retryable error and logs it.
     *
     * @param function Name of the function emitting the error.
     * @param error Error object, message, exception, or SDK event.
     * @param eventValue Optional payload attached to the error.
     * @param logValue Optional values included only in the log message.
     * @return The emitted [RetryError].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun retry(
        function: String,
        error: Any?,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): RetryError {
        val (event, message) = extractErrorDetails(error)
        val evMessage = eventLogBuilder(message, logValue)

        log(buildString { append("$function():"); append(" $evMessage") })

        return RetryError(function, event, evMessage, eventValue).also {
            try {
                subscriber?.invoke(it)
            } catch (e: Throwable) {
                log("events subscriber error: ${e.message}")
            }
        }
    }

    /**
     * Extracts an SDK event and message from an error object.
     *
     * @param err Error object, message, exception, or SDK event.
     * @return SDK event and resolved message.
     */
    private fun extractErrorDetails(err: Any?): Pair<SDKEvent?, String> {
        return when (err) {
            is SDKException.SDKException -> {
                val event = err.info()
                event to event.message
            }

            is Exception ->
                null to (err.localizedMessage ?: err.toString())

            is SDKEvent ->
                err to err.message

            null ->
                null to "unknown error"

            else ->
                null to err.toString()
        }
    }
}