package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.validProviders
import com.altcraft.sdk.data.room.ConfigurationEntity

/**
 * Utility for building API URLs, auth headers, and log/error messages used across the SDK.
 */
internal object StringBuilder {

    /**
     * Generates the URL for push notification subscription requests.
     *
     * @param apiUrl The base API URL.
     * @return The complete URL for subscribing to push notifications.
     */
    fun subscribeUrl(apiUrl: String) = "$apiUrl/subscription/push/subscribe/"

    /**
     * Generates a URL for updating the push token of subscriptions.
     *
     * @param apiUrl Returns the base API URL.
     * @returns the complete URL for updating push subscriptions.
     */
    fun updateUrl(apiUrl: String) = "$apiUrl/subscription/push/update/"

    /**
     * Generates the URL for retrieving push notification subscription status.
     *
     * @param apiUrl The base API URL.
     * @return The complete URL for fetching push subscription status.
     */
    fun statusUrl(apiUrl: String) = "$apiUrl/subscription/push/status/"

    /**
     * Generates the URL for unSuspend request.
     *
     * @param apiUrl The base API URL.
     * @return The complete URL for fetching push subscription status.
     */
    @Suppress("SpellCheckingInspection")
    fun unSuspendUrl(apiUrl: String) = "$apiUrl/subscription/push/unsuspend/"

    /**
     * Generates the URL for push event tracking.
     *
     * @param apiUrl The base API URL.
     * @param type The type of the push event (e.g., "open", "delivery").
     * @return The full URL with event type appended.
     */
    fun eventPushUrl(apiUrl: String, type: String) = "$apiUrl/event/push/$type"

    /**
     * Generates the URL for mobile event tracking.
     *
     * @param apiUrl The base API URL.
     * @return The full URL with event type appended.
     */
    fun eventMobileUrl(apiUrl: String) = "$apiUrl/event/post"

    /**
     * Builds a Bearer authorization header using the given JWT token.
     *
     * @param jwtToken The JWT token to format.
     * @return A string in the format "Bearer <jwtToken>".
     */
    fun bearerJwtToken(jwtToken: String?) = "Bearer $jwtToken"

    /**
     * Constructs an authorization string in the format: "Bearer rtoken@<token>".
     *
     * @param rToken The R-token to format.
     * @return A formatted bearer token string for use in authorization headers.
     */
    fun bearerRToken(rToken: String) = "Bearer rtoken@$rToken"

    /**
     * Generates a message about deleted push events.
     *
     * @param totalCount The total number of events before deletion.
     * @return A summary message indicating deletion count.
     */
    fun deletedPushEventsMsg(totalCount: Int) =
        "Deleted 100 oldest push events. Total count before: $totalCount"

    /**
     * Generates a message about deleted mobile events.
     *
     * @param totalCount The total number of events before deletion.
     * @return A summary message indicating deletion count.
     */
    fun deletedMobileEventsMsg(totalCount: Int) =
        "Deleted 100 oldest mobile events. Total count before: $totalCount"

    /**
     * Generates a message about deleted subscriptions.
     *
     * @param totalCount The total number of subscriptions before deletion.
     * @return A summary message indicating deletion count.
     */
    fun deletedSubscriptionsMsg(totalCount: Int) =
        "Deleted 100 oldest subscriptions. Total count before: $totalCount"

    /**
     * Creates a message for an event indicating the presence of invalid fields that are objects in
     * mobile event parameters.
     *
     * @param eventName The name of the mobile event that contains invalid fields
     * @return Error message string indicating that the mobile event payload contains non-primitive
     * values
     */
    fun mobileEventPayloadInvalid(eventName: String) =
        "invalid mobile event payload: not all values are primitives. Event name: $eventName"

    /**
     * Formats an exception message for a specific service.
     *
     * @param serviceName The name of the service where the exception occurred.
     * @param exception The exception that was thrown.
     * @return A formatted string describing the exception.
     */
    fun serviceExceptionMessage(serviceName: String, exception: Exception) =
        "$serviceName exception: $exception"

    /**
     * Generates an error message indicating an unsupported entity type.
     *
     * @param entity The name of the entity that caused the error (nullable).
     * @return A formatted error message describing the unsupported entity.
     */
    fun errorEntityType(entity: String?) = "Unsupported entity type: $entity"

    /**
     * Builds a formatted log string for an event.
     *
     * @param function The name of the function where the event occurred.
     * @param message An optional message providing additional details about the event.
     * @return A formatted string in the format "<function>(): <message>".
     */
    fun eventLogBuilder(function: String, message: String? = null): String {
        return buildString {
            append("${function}():")
            message?.let { append(" $it") }
        }
    }

    /**
     * Returns a description of an invalid configuration or an error message.
     *
     * @param config Configuration to validate.
     * @return Description of the issue or exception message.
     */
    fun invalidConfigMsg(config: ConfigurationEntity): String {
        val errors = mutableListOf<String>()

        if (config.apiUrl.isEmpty()) errors += "apiUrl is empty"

        val invalidList = config.providerPriorityList?.minus(validProviders)

        if (!invalidList.isNullOrEmpty()) {
            errors += invalidProvidersMsg(invalidList, validProviders)
        }
        return "invalid config: ${errors.joinToString("; ")}"
    }

    /**
     * Returns a message listing invalid providers.
     *
     * @param list Providers to check.
     * @param valid Allowed providers.
     */
    private fun invalidProvidersMsg(list: List<String>, valid: Set<String>) =
        "providerPriorityList contains invalid values:$list. Allowed: $valid"
}