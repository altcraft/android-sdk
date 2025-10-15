package com.altcraft.sdk.push.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.additional.ActionFieldBuilder
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.subscribe.PushSubscribe.pushSubscribe
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.Constants.LATEST_FOR_PROVIDER
import com.altcraft.sdk.data.Constants.LATEST_SUBSCRIPTION
import com.altcraft.sdk.data.Constants.MATCH_CURRENT_CONTEXT
import com.altcraft.sdk.data.Constants.RESPONSE_WITH_HTTP_CODE
import com.altcraft.sdk.data.Constants.validProviders
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.sdk_events.EventList.invalidPushProvider
import com.altcraft.sdk.network.Request.statusRequest
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception

/**
 * The Subscribe object contains the functions required to subscribe to push notifications.
 */
@Keep
object PublicPushSubscriptionFunctions {

    /**
     * Initiates a push subscription request.
     *
     * @param context The application context for the subscription request.
     * @param sync Flag that controls execution mode: `true` — synchronous, `false` — asynchronous.
     * @param profileFields Optional profile fields to include in the request.
     * @param customFields Optional custom fields to include in the request.
     * @param cats Optional list of categories to associate with the subscription.
     * @param replace Optional flag to replace an existing subscription (`true`).
     * @param skipTriggers Optional flag to skip trigger execution during subscription.
     */
    fun pushSubscribe(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<DataClasses.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.SUBSCRIBED,
            sync = if (sync) 1 else 0,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Initiates a request to suspend push notifications for the current profile.
     * Unlike unsubscription, this temporarily pauses delivery without removing the subscription.
     *
     * @param context The application context for the suspend request.
     * @param sync Flag that controls execution mode: `true` — synchronous, `false` — asynchronous.
     * @param profileFields Optional profile fields to include in the request.
     * @param customFields Optional custom fields to include in the request.
     * @param cats Optional list of categories to associate with the subscription.
     * @param replace Optional flag to replace an existing subscription (`true`).
     * @param skipTriggers Optional flag to skip trigger execution during subscription.
     */
    fun pushSuspend(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<DataClasses.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.SUSPENDED,
            sync = if (sync) 1 else 0,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Initiates a push unsubscription request, notifying the server
     * to stop sending push notifications for the current profile.
     *
     * @param context The application context for the unsubscription request.
     * @param sync Flag that controls execution mode: `true` — synchronous, `false` — asynchronous.
     * @param profileFields Optional profile fields to include in the request.
     * @param customFields Optional custom fields to include in the request.
     * @param cats Optional list of categories to disassociate during unsubscription.
     * @param replace Optional flag to replace the current subscription (`true`).
     * @param skipTriggers Optional flag to skip trigger execution during unsubscription.
     */
    fun pushUnSubscribe(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<DataClasses.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.UNSUBSCRIBED,
            sync = if (sync) 1 else 0,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Updates push subscription statuses based on the provided matching mode.
     *
     * - Subscriptions matching the given `matching` are changed from `suspended` to `subscribed`.
     * - Other active subscriptions with the same push token are set to `suspended`.
     *
     * Executes a one-time request. Returns `null` if the operation fails.
     *
     * @param context Android [Context] for configuration and request execution.
     * @return [DataClasses.ResponseWithHttpCode] with HTTP code and response, or `null` on failure.
     */
    suspend fun unSuspendPushSubscription(context: Context): DataClasses.ResponseWithHttpCode? {
        return try {
            val eventValue = Request.unSuspendRequest(context).eventValue

            eventValue?.get(RESPONSE_WITH_HTTP_CODE) as? DataClasses.ResponseWithHttpCode
        } catch (e: Exception) {
            error("unSuspendPushSubscription", e)
            null
        }
    }

    /**
     * Returns the status of the latest subscription in profile.
     *
     * @param context Application context used for internal request resolution.
     */
    suspend fun getStatusOfLatestSubscription(
        context: Context
    ): DataClasses.ResponseWithHttpCode? {
        return try {
            val eventValue = statusRequest(context, mode = LATEST_SUBSCRIPTION).eventValue

            eventValue?.get(RESPONSE_WITH_HTTP_CODE) as? DataClasses.ResponseWithHttpCode
        } catch (e: Exception) {
            error("getStatusOfLatestSubscription", e)
            null
        }
    }

    /**
     * Returns the status of the latest subscription for a push provider.
     *
     * If [provider] is specified, queries the latest subscription for that provider.
     * If null, uses the current push provider.
     *
     * @param context Application context used for internal request resolution.
     * @param provider Optional push provider name to override the current one.
     */
    suspend fun getStatusOfLatestSubscriptionForProvider(
        context: Context,
        provider: String? = null
    ): DataClasses.ResponseWithHttpCode? {
        return try {
            provider?.let { if (it !in validProviders) exception(invalidPushProvider) }
            val eventValue = statusRequest(context, mode = LATEST_FOR_PROVIDER, provider)
                .eventValue
            eventValue?.get(RESPONSE_WITH_HTTP_CODE) as? DataClasses.ResponseWithHttpCode
        } catch (e: Exception) {
            error("getStatusOfLatestSubscriptionForProvider", e)
            null
        }
    }

    /**
     * Returns the status of a subscription matching the current push token and current push provider.
     *
     * @param context Application context used for internal request resolution.
     */
    suspend fun getStatusForCurrentSubscription(
        context: Context
    ): DataClasses.ResponseWithHttpCode? {
        return try {
            val eventValue = statusRequest(context, mode = MATCH_CURRENT_CONTEXT).eventValue

            eventValue?.get(RESPONSE_WITH_HTTP_CODE) as? DataClasses.ResponseWithHttpCode
        } catch (e: Exception) {
            error("getStatusForCurrentSubscription", e)
            null
        }
    }

    /**
     * Creates a [ActionFieldBuilder] for the given profile field key.
     *
     * @param key The key under which the action-value pair will be placed.
     * @return A builder for specifying the desired action (set, incr, etc.) and value.
     */
    @Keep
    fun actionField(key: String) = ActionFieldBuilder(key)

    /**
     * Example usage:
     *
     * Increment the "Inc" field in a profile using the functional `incr()` operation
     * via the `pushSubscribe` method.
     *
     * ```kotlin
     * AltcraftSDK.PublicPushSubscriptionFunctions.pushSubscribe(
     *     context = context,
     *     profileFields = mapOf(
     *         actionField("Inc").incr(1)
     *     )
     * )
     * ```
     *
     * Supported actions:
     * - `.set(value)`
     * - `.unset(value)`
     * - `.incr(value)`
     * - `.add(value)`
     * - `.delete(value)`
     * - `.upsert(value)`
     */
}