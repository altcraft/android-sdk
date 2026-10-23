package com.altcraft.sdk.inapp.platform.subscription.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithHttpCode
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.Constants
import com.altcraft.sdk.inapp.platform.subscription.status.request.StatusRequest.inAppStatusRequest
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess.inAppSubscribe
import kotlin.coroutines.cancellation.CancellationException

/**
 * Public API for In-App subscription management.
 *
 * Provides APIs for:
 * - In-App subscription;
 * - In-App unsubscription;
 * - In-App subscription status retrieval.
 */
@Keep
object SubscriptionAPI {

    /**
     * Initiates an in-app subscription request.
     *
     * status = SUBSCRIBED
     *
     * @param context The application context for the subscription request.
     * @param sync Flag that controls execution mode: `true` — synchronous, `false` — asynchronous.
     * @param profileFields Optional profile fields to include in the request.
     * @param customFields Optional custom fields to include in the request.
     * @param cats Optional list of categories to associate with the subscription.
     * @param replace Optional flag to replace an existing subscription (`true`).
     * @param skipTriggers Optional flag to skip trigger execution during subscription.
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
        inAppSubscribe(
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
     * Initiates an in-app unsubscription request, notifying the server
     * to stop sending in-app notifications for the current profile.
     *
     * status = UNSUBSCRIBED
     *
     * @param context The application context for the unsubscription request.
     * @param sync Flag that controls execution mode: `true` — synchronous, `false` — asynchronous.
     * @param profileFields Optional profile fields to include in the request.
     * @param customFields Optional custom fields to include in the request.
     * @param cats Optional list of categories to disassociate during unsubscription.
     * @param replace Optional flag to replace the current subscription (`true`).
     * @param skipTriggers Optional flag to skip trigger execution during unsubscription.
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
        inAppSubscribe(
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
     * Requests the current in-app subscription status.
     *
     * @param context Application context used for request execution.
     * @return [ResponseWithHttpCode] with the server response, or `null` on failure.
     */
    suspend fun getSubscriptionStatus(
        context: Context
    ): ResponseWithHttpCode<ResponseWithProfile>? {
        return try {
            if (!CoreEnvironmentProvider.isAuth()) exception(
                CoreEvents.notAuthenticatedError
            )

            val response = inAppStatusRequest(context)

            ResponseWithHttpCode(
                httpCode = response.httpCode,
                response = response.response
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getStatusForCurrentSubscription", e)
            null
        }
    }
}