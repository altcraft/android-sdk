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
     * @return The full URL for subscribing to push notifications.
     */
    fun subscribeUrl(apiUrl: String) = "$apiUrl/subscription/push/subscribe/"

    /**
     * Generates a URL for updating the push token for subscriptions.
     *
     * @param apiUrl The base API URL.
     * @return The full URL for updating push subscriptions.
     */
    fun tokenUpdateUrl(apiUrl: String) = "$apiUrl/subscription/push/update/"

    /**
     * Generates the URL for retrieving push notification subscription status.
     *
     * @param apiUrl The base API URL.
     * @return The full URL for fetching push subscription status.
     */
    fun statusUrl(apiUrl: String) = "$apiUrl/subscription/push/status/"

    /**
     * Generates the URL for an unsuspend request.
     *
     * @param apiUrl The base API URL.
     * @return The full URL for unsuspending a push subscription.
     */
    @Suppress("SpellCheckingInspection")
    fun unSuspendUrl(apiUrl: String) = "$apiUrl/subscription/push/unsuspend/"

    /**
     * Generates the URL for push event tracking.
     *
     * @param apiUrl The base API URL.
     * @param type The type of the push event (e.g., "open", "delivery").
     * @return The full URL with the event type appended.
     */
    fun eventPushUrl(apiUrl: String, type: String) = "$apiUrl/event/push/$type"

    /**
     * Generates the URL for mobile event tracking.
     *
     * @param apiUrl The base API URL.
     * @return The full URL for posting a mobile event.
     */
    fun eventMobileUrl(apiUrl: String) = "$apiUrl/event/post"

    /**
     * Generates the URL for profile update requests.
     *
     * @param apiUrl The base API URL.
     * @return The full URL for updating a profile.
     */
    fun profileUpdateUrl(apiUrl: String) = "$apiUrl/profile/update"

    /**
     * Builds a Bearer authorization header using the given JWT token.
     *
     * @param jwtToken The JWT token to format.
     * @return A string in the format "Bearer <jwtToken>".
     */
    fun bearerJwtToken(jwtToken: String) = "Bearer $jwtToken"

    /**
     * Constructs an authorization string in the format: "Bearer rtoken@<token>".
     *
     * @param rToken The R-token to format.
     * @return A formatted bearer token string for use in authorization headers.
     */
    fun bearerRToken(rToken: String) = "Bearer rtoken@$rToken"

    /**
     * Builds a message for cleanup when the push event storage overflows.
     *
     * Removes 100 oldest push events to reduce stored items.
     *
     * @param totalCount Total number of push events before cleanup.
     * @return Cleanup summary message.
     */
    fun deletedPushEventsMsg(totalCount: Int) =
        "Deleted 100 oldest push events. Total count before: $totalCount"

    /**
     * Builds a message for cleanup when the mobile event storage overflows.
     *
     * Removes 100 oldest mobile events to reduce stored items.
     *
     * @param totalCount Total number of mobile events before cleanup.
     * @return Cleanup summary message.
     */
    fun deletedMobileEventsMsg(totalCount: Int) =
        "Deleted 100 oldest mobile events. Total count before: $totalCount"

    /**
     * Builds a message for cleanup when the subscription storage overflows.
     *
     * Removes 100 oldest subscriptions to reduce stored items.
     *
     * @param totalCount Total number of subscriptions before cleanup.
     * @return Cleanup summary message.
     */
    fun deletedSubscriptionsMsg(totalCount: Int) =
        "Deleted 100 oldest subscriptions. Total count before: $totalCount"

    /**
     * Builds a message for cleanup when the profile update storage overflows.
     *
     * Removes 100 oldest profile updates to reduce stored items.
     *
     * @param totalCount Total number of profile updates before cleanup.
     * @return Cleanup summary message.
     */
    fun deletedProfileUpdatesMsg(totalCount: Int) =
        "Deleted 100 oldest profile updates. Total count before: $totalCount"

    /**
     * Creates a message indicating invalid (non-primitive) values in mobile event parameters.
     *
     * @param eventName The name of the mobile event that contains invalid fields.
     * @return An error message describing the invalid mobile event payload.
     */
    fun mobileEventPayloadInvalid(eventName: String) =
        "invalid mobile event payload: not all values are primitives. Event name: $eventName"

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
     * Builds a description of an invalid configuration.
     *
     * @param config Configuration to validate.
     * @return A message describing all detected configuration issues.
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
     * @return A message describing invalid providers and the allowed set.
     */
    private fun invalidProvidersMsg(list: List<String>, valid: Set<String>) =
        "providerPriorityList contains invalid values:$list. Allowed: $valid"
}