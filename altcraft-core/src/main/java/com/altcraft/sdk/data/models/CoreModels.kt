package com.altcraft.sdk.data.models

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.altcraft.sdk.data.Constants.ERROR_KEY
import com.altcraft.sdk.data.Constants.ERROR_TEXT_KEY
import com.altcraft.sdk.data.Constants.PROFILE_KEY
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.network.response.Response.ResponseStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.Date

typealias retry = CoreModels.SDKEventModel.RetryError
typealias error = CoreModels.SDKEventModel.Error

/**
 * Models of the Altcraft SDK core.
 *
 * Contains:
 * - [CoreModels.ResponseModel] — server response models;
 * - [CoreModels.SDKEventModel] — SDK event models;
 * - [CoreModels.PropertyModel] — application properties;
 * - [CoreModels.ProfileModel] — profile, subscription, and category data.
 */
@Suppress("unused")
object CoreModels {

    /**
     * Contains parsed server response models and response processing helpers.
     *
     * Includes common response contracts, the HTTP response wrapper, async and profile
     * response bodies, and the processed response result used internally.
     */
    object ResponseModel {

        /**
         * Base marker interface for all parsed server response models.
         *
         * Every typed response body implements this interface and contributes data to
         * SDK events ([eventData]) and logs ([loggerData]).
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseData
         *
         * fun handle(response: ResponseData?) {
         *     val eventValue = response?.eventData
         *     val logValue = response?.loggerData
         * }
         * ```
         */
        interface ResponseData {
             /** Additional data used as SDK event value payload. */
            val eventData: Map<String, Any?>

            /** Additional data used as SDK log payload. */
            val loggerData: Map<String, Any?>?
        }

        /**
         * Wraps the API response together with the HTTP status code.
         *
         * @property httpCode The HTTP status code returned by the server (e.g., 200, 404).
         * @property response The parsed API response body, or `null` if unavailable.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithHttpCode
         *
         * val raw = event.eventValue?.get(RESPONSE_WITH_HTTP) as? ResponseWithHttpCode<*>
         * val httpCode: Int? = raw?.httpCode
         * val errorText: String? = (raw?.response as? BaseResponseData)?.errorText
         * ```
         */
        data class ResponseWithHttpCode<T : ResponseData>(
            val httpCode: Int?,
            val response: T?
        )

        /**
         * Base contract for API responses that may contain error information returned by the server.
         *
         * @property error Optional numeric error code.
         * @property errorText Optional error description.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ResponseModel.BaseResponseData
         *
         * val base = response as? BaseResponseData
         * if (base?.error != null && base.error > 0) logFailure(base.error, base.errorText)
         * ```
         */
        interface BaseResponseData : ResponseData {
            val error: Int?
            val errorText: String?

            override val eventData: Map<String, Any?>
                get() = mapOf(
                    ERROR_KEY to error,
                    ERROR_TEXT_KEY to errorText
                )

            override val loggerData: Map<String, Any?>
                get() = mapOf(
                    ERROR_KEY to error,
                    ERROR_TEXT_KEY to errorText
                )
        }

        /**
         * Response containing authorization data returned by the server.
         *
         * @property error optional numeric error code returned by the server.
         * @property errorText error description, if available.
         * @property acid Altcraft identifier returned by the server.
         * @property isTest indicates whether the profile is a test profile.
         */
        @Serializable
        internal data class ResponseWithAuth(
            override val error: Int? = null,
            @SerialName("error_text")
            override val errorText: String? = null,
            val acid: String? = null,
            val isTest: Boolean = false
        ) : BaseResponseData

        /**
         * Represents a server response containing profile data from the Altcraft platform.
         *
         * @property error Optional numeric error code returned by the server.
         * @property errorText Error description, if available.
         * @property profile Profile data in the Altcraft platform.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithProfile
         * import com.altcraft.sdk.data.models.CoreModels.ProfileModel.ProfileData
         *
         * val result: ResponseWithProfile = statusApi.latest(context)?.response ?: return
         * val profile: ProfileData? = result.profile
         * val errorText: String? = result.errorText
         * ```
         */
        @Serializable
        data class ResponseWithProfile(
            override val error: Int? = null,
            @SerialName("error_text")
            override val errorText: String? = null,
            val profile: ProfileModel.ProfileData? = null
        ) : BaseResponseData {
            override val eventData: Map<String, Any?>
                get() = super.eventData + (PROFILE_KEY to profile)
        }

        /**
         * Represents a basic server response without additional payload data.
         *
         * @property error Optional numeric error code returned by the server.
         * @property errorText Error description, if available.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ResponseModel.AsyncResponse
         *
         * // async request responses are typed with AsyncResponse:
         * val response: AsyncResponse = event.eventValue?.get(RESPONSE) as? AsyncResponse ?: return
         * ```
         */
        @Serializable
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class AsyncResponse(
            override val error: Int? = null,
            @SerialName("error_text")
            override val errorText: String? = null,
        ) : BaseResponseData

        /**
         * Holds additional payload data for SDK event reporting and logging.
         *
         * @property eventValue Additional data used as SDK event value payload.
         * @property loggerValue Additional data used as SDK log payload.
         */
        internal data class ResponseEventValue(
            val eventValue: Map<String, Any?>?,
            val loggerValue: Map<String, Any?>?,
        )

        /**
         * Holds processed response data together with SDK event context.
         *
         * @param T Type of the parsed response model.
         *
         * @property status Resulting response status (SUCCESS, RETRY, or ERROR).
         * @property responseData Parsed response body, or `null` if unavailable.
         * @property httpCode HTTP status code returned by the server.
         * @property value Additional event payload associated with the response.
         * @property errorEvent SDK event used when the request fails.
         * @property successEvent SDK event used when the request succeeds.
         */
        internal data class ResponseResult<T : ResponseData>(
            val status: ResponseStatus,
            val responseData: T?,
            val httpCode: Int?,
            val value: ResponseEventValue,
            val moduleLogPrefix: String,
            val errorEvent: SDKEvent,
            val successEvent: SDKEvent,
        )
    }

    /**
     * Contains SDK event models used for reporting regular events, non-retryable errors,
     * and retryable errors.
     */
    object SDKEventModel {

        /**
         * Represents a general SDK event with associated details.
         *
         * @property function The name or identifier of the function where the event occurred.
         * @property event The SDK event type.
         * @property eventMessage A human-readable message describing the event.
         * @property eventValue Additional event-related metadata.
         * @property date The date and time when the event occurred.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
         *
         * AltcraftSDK.SDKEvents.subscribe { event: Event ->
         *     val function = event.function
         *     val message = event.eventMessage
         *     val value = event.eventValue
         * }
         * ```
         */
        @Keep
        open class Event(
            val function: String,
            val event: SDKEvent? = null,
            val eventMessage: String? = null,
            val eventValue: Map<String, Any?>? = null,
            val date: Date = Date(),
            val internal: Boolean = false
        )

        /**
         * Represents a non-retryable error event.
         *
         * @param function The name or identifier of the function where the error occurred.
         * @param event The SDK error type.
         * @param eventMessage A human-readable message describing the error.
         * @param eventValue Additional error-related metadata.
         * @param date The date and time when the error occurred.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Error
         *
         * if (event.event is Error) showFatalError(event.eventMessage)
         * ```
         */
        @Keep
        open class Error(
            function: String,
            event: SDKEvent? = null,
            eventMessage: String? = null,
            eventValue: Map<String, Any?>? = null,
            date: Date = Date(),
        ) : Event(function, event, eventMessage, eventValue, date)

        /**
         * Represents a retryable error event.
         *
         * @param function The name or identifier of the function where the retryable error occurred.
         * @param event The SDK error type.
         * @param eventMessage A human-readable message describing the retryable error.
         * @param eventValue Additional error-related metadata.
         * @param date The date and time when the retryable error occurred.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.RetryError
         *
         * when (event) {
         *     is RetryError -> showRetryingIndicator()
         *     is Error -> showFatalError(event.eventMessage)
         * }
         * ```
         */
        @Keep
        class RetryError(
            function: String,
            event: SDKEvent? = null,
            eventMessage: String? = null,
            eventValue: Map<String, Any?>? = null,
            date: Date = Date(),
        ) : Error(function, event, eventMessage, eventValue, date)
    }

    /**
     * Contains property models used for SDK function parameters and request data.
     */
    object PropertyModel {

        /**
         * Holds basic metadata about the application (e.g. for analytics).
         *
         * @property appID The unique application identifier.
         * @property appIID The installation identifier (Instance ID) for this app.
         * @property appVer The version string of the application.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.PropertyModel.AppInfo
         * import com.altcraft.sdk.config.AltcraftConfiguration
         *
         * val configuration = AltcraftConfiguration.Builder(
         *     apiUrl = "https://example.com",
         *     rToken = "role-token",
         *     appInfo = AppInfo(appID = "com.example.app", appIID = "iid", appVer = "1.0.0")
         * ).build()
         * ```
         */
        @Keep
        @Serializable
        data class AppInfo(
            val appID: String = "",
            val appIID: String = "",
            val appVer: String = ""
        ) {
            /**
             * Converts this [AppInfo] into a key-value map for analytics fields.
             *
             * @return A map with keys `_app_id`, `_app_iid`, and `_app_ver`.
             */
            internal fun asMap(): Map<String, String> {
                return mapOf(
                    "_app_id" to appID,
                    "_app_iid" to appIID,
                    "_app_ver" to appVer
                )
            }
        }
    }

    /**
     * Contains profile-related models returned by the platform.
     */
    object ProfileModel {

        /**
         * Represents user profile data, including the ID, status, and subscription details.
         *
         * @property id The unique profile identifier.
         * @property status The profile status as a string.
         * @property isTest Indicates whether the profile is a test profile.
         * @property subscription Subscription details for the profile (optional).
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ProfileModel.ProfileData
         *
         * val profile: ProfileData? = withProfile?.profile
         * if (profile?.isTest == true) showTestBadge(profile.id)
         * ```
         */
        @Keep
        @Serializable
        data class ProfileData(
            val id: String? = null,
            val status: String? = null,
            val acid: String? = null,
            @SerialName("is_test")
            val isTest: Boolean? = null,
            val subscription: SubscriptionData? = null
        )

        /**
         * Represents a subscription with its identifiers, status, provider, fields, and categories.
         *
         * @property subscriptionId The subscription token.
         * @property hashId The unique subscription identifier.
         * @property provider The subscription provider (e.g., "android-firebase").
         * @property status The subscription status as a string.
         * @property fields The field map (optional).
         * @property cats A list of subscription categories.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ProfileModel.SubscriptionData
         * import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
         *
         * val subscription: SubscriptionData? = profile?.subscription
         * val categories: List<CategoryData>? = subscription?.cats
         * ```
         */
        @Keep
        @Serializable
        data class SubscriptionData(
            @SerialName("subscription_id")
            val subscriptionId: String? = null,
            @SerialName("hash_id")
            val hashId: String? = null,
            val provider: String? = null,
            val status: String? = null,
            val fields: Map<String, JsonElement>? = null,
            val cats: List<CategoryData>? = null
        )

        /**
         * Represents the details of a subscription category.
         *
         * @property name The category identifier.
         * @property title The category name.
         * @property steady Indicates whether the category is locked for modification.
         * @property active Indicates whether the category is active.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
         *
         * val activeCategories = (subscription?.cats ?: emptyList()).filter { it.active == true }
         * ```
         */
        @Keep
        @Serializable
        data class CategoryData(
            val name: String?,
            val title: String? = null,
            val steady: Boolean? = null,
            val active: Boolean?
        )
    }
}
