package com.altcraft.sdk.push.platform.subscription.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.util.ActionFieldBuilder
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithHttpCode
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.data.Constants
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess.pushSubscribe
import com.altcraft.sdk.push.platform.subscription.status.StatusAPI
import com.altcraft.sdk.push.platform.subscription.unsuspend.request.UnSuspendRequest.unSuspendRequest
import kotlin.coroutines.cancellation.CancellationException

/**
 * Public facade for push subscription operations.
 */
@Keep
object SubscriptionAPI {

    /**
     * Push subscription status API.
     */
    @Keep
    val status = StatusAPI

    /**
     * Initiates a push subscription request.
     *
     * @param context Application context.
     * @param sync Whether the server should process the request synchronously.
     * @param profileFields Optional profile fields.
     * @param customFields Optional custom fields.
     * @param cats Optional subscription categories.
     * @param replace Whether existing subscription data should be replaced.
     * @param skipTriggers Whether server-side triggers should be skipped.
     */
    fun subscribe(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.SUBSCRIBED,
            sync = sync,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Initiates a request to suspend push notifications.
     *
     * @param context Application context.
     * @param sync Whether the server should process the request synchronously.
     * @param profileFields Optional profile fields.
     * @param customFields Optional custom fields.
     * @param cats Optional subscription categories.
     * @param replace Whether existing subscription data should be replaced.
     * @param skipTriggers Whether server-side triggers should be skipped.
     */
    fun suspend(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.SUSPENDED,
            sync = sync,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Initiates a push unsubscription request.
     *
     * @param context Application context.
     * @param sync Whether the server should process the request synchronously.
     * @param profileFields Optional profile fields.
     * @param customFields Optional custom fields.
     * @param cats Optional subscription categories.
     * @param replace Whether existing subscription data should be replaced.
     * @param skipTriggers Whether server-side triggers should be skipped.
     */
    fun unsubscribe(
        context: Context,
        sync: Boolean = true,
        profileFields: Map<String, Any?>? = null,
        customFields: Map<String, Any?>? = null,
        cats: List<CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        pushSubscribe(
            context = context,
            status = Constants.UNSUBSCRIBED,
            sync = sync,
            profileFields = profileFields,
            customFields = customFields,
            cats = cats,
            replace = replace,
            skipTriggers = skipTriggers
        )
    }

    /**
     * Switches push subscriptions between profiles.
     *
     * @param context Application context.
     * @return Server response with HTTP status code, or `null` if the request fails.
     */
    suspend fun unSuspend(
        context: Context
    ): ResponseWithHttpCode<ResponseWithProfile>? {
        return try {
            val response = unSuspendRequest(
                context
            )

            ResponseWithHttpCode(
                httpCode = response.httpCode,
                response = response.response
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("unSuspendPushSubscription", e)
            null
        }
    }

    /**
     * Creates an [ActionFieldBuilder] for the specified profile field key.
     *
     * @param key Profile field key.
     * @return Configured [ActionFieldBuilder].
     */
    fun actionField(key: String) = ActionFieldBuilder(
        key
    )
}
