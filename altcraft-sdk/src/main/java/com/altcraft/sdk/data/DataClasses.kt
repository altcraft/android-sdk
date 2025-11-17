package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.annotation.Keep
import androidx.work.WorkInfo
import com.altcraft.sdk.data.Constants.DB_ID
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.MATCHING_ID
import com.altcraft.sdk.data.Constants.PROVIDER
import com.altcraft.sdk.data.Constants.TOKEN
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.json.serializer.subscription.SubscriptionSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Date

typealias error = DataClasses.Error
typealias retry = DataClasses.RetryError

/**
 * Contains data classes used for various purposes within the SDK.
 */
object DataClasses {

    //public

    /**
     * A data class representing token data used for push notifications.
     *
     * @property provider The push notification provider (
     * android-firebase, android-huawei, android-rustore
     * ).
     * @property token The device-specific token used for push notifications.
     */
    @Keep
    @Serializable
    data class TokenData(
        val provider: String,
        val token: String
    ) {
        internal fun toMap(): Map<String, String> {
            return mapOf(
                PROVIDER to provider,
                TOKEN to token
            )
        }
    }

    /**
     * Holds basic metadata about the application for Firebase analytics.
     *
     * @property appID The unique Firebase App identifier.
     * @property appIID The installation identifier (Instance ID) for this app.
     * @property appVer The version string of the application.
     */
    @Keep
    @Serializable
    data class AppInfo(
        val appID: String,
        val appIID: String,
        val appVer: String
    ) {
        /**
         * Converts this [AppInfo] into a key-value map for analytics fields.
         *
         * @return A map with keys `_app_id`, `_app_iid`, and `_app_ver`
         *   mapped to their corresponding property values.
         */
        internal fun asMap(): Map<String, String> {
            return mapOf(
                "_app_id" to appID,
                "_app_iid" to appIID,
                "_app_ver" to appVer
            )
        }
    }

    /**
     * Wraps the API response together with the HTTP status code.
     *
     * @property httpCode The HTTP status code returned by the server (e.g., 200, 404).
     * @property response The parsed API response body, or `null` if unavailable.
     */
    @Keep
    data class ResponseWithHttpCode(
        val httpCode: Int?,
        val response: Response?
    )

    /**
     * Represents the response from the API.
     *
     * @property error The numeric error code.
     * @property errorText The descriptive error message.
     * @property profile The user profile data, if available.
     * Contains information about the user's profile, including status and subscriptions.
     */
    @Keep
    @Serializable
    data class Response(
        val error: Int? = null,
        @SerialName("error_text")
        val errorText: String? = null,
        val profile: ProfileData? = null
    )

    /**
     * Represents user profile data, including the ID, status, and subscription details.
     *
     * @property id The unique profile identifier.
     * @property status The profile status as a string.
     * @property isTest Indicates whether the profile is a test profile.
     * @property subscription Subscription details for the profile (optional).
     */
    @Keep
    @Serializable
    data class ProfileData(
        val id: String? = null,
        val status: String? = null,
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
     */
    @Keep
    @Serializable
    data class CategoryData(
        val name: String?,
        val title: String? = null,
        val steady: Boolean? = null,
        val active: Boolean?
    )

    /**
     * Base interface for all subscription types added to a profile via mobile events.
     *
     * @property resourceId Resource identifier.
     * @property status Subscription status (optional).
     * @property priority Subscription priority (optional).
     * @property customFields Standard and custom subscription fields (optional).
     * @property cats Subscription categories (optional).
     * @property channel Channel type.
     */
    @Keep
    @Serializable(with = SubscriptionSerializer::class)
    sealed interface Subscription {
        @SerialName("resource_id")
        val resourceId: Int
        val status: String?
        val priority: Int?
        @SerialName("custom_fields")
        val customFields: Map<String, @Contextual Any?>?
        val cats: List<String>?
        val channel: String
    }

    /**
     * Email channel subscription.
     *
     * @property resourceId Resource identifier.
     * @property email Email address.
     * @property status Subscription status (optional).
     * @property priority Subscription priority (optional).
     * @property customFields Standard and custom subscription fields (optional).
     * @property cats Subscription categories (optional).
     * @property channel Channel type, always `"email"`.
     */
    @Keep
    @Suppress("unused")
    @Serializable
    data class EmailSubscription(
        @SerialName("resource_id")
        override val resourceId: Int,
        val email: String,
        override val status: String? = null,
        override val priority: Int? = null,
        @SerialName("custom_fields")
        override val customFields: Map<String, @Contextual Any?>? = null,
        override val cats: List<String>? = null,
    ) : Subscription {
        @SerialName("channel")
        override val channel: String = "email"
    }

    /**
     * SMS channel subscription.
     *
     * @property resourceId Resource identifier.
     * @property phone Phone number.
     * @property status Subscription status (optional).
     * @property priority Subscription priority (optional).
     * @property customFields Standard and custom subscription fields (optional).
     * @property cats Subscription categories (optional).
     * @property channel Channel type, always `"sms"`.
     */
    @Keep
    @Suppress("unused")
    @Serializable
    data class SmsSubscription(
        @SerialName("resource_id")
        override val resourceId: Int,
        val phone: String,
        override val status: String? = null,
        override val priority: Int? = null,
        @SerialName("custom_fields")
        override val customFields: Map<String, @Contextual Any?>? = null,
        override val cats: List<String>? = null,
    ) : Subscription {
        @SerialName("channel")
        override val channel: String = "sms"
    }

    /**
     * Push channel subscription.
     *
     * @property resourceId Resource identifier.
     * @property provider Provider type (e.g., `"android-firebase"`).
     * @property subscriptionId Unique subscription identifier.
     * @property status Subscription status (optional).
     * @property priority Subscription priority (optional).
     * @property customFields Standard and custom subscription fields (optional).
     * @property cats Subscription categories (optional).
     * @property channel Channel type, always `"push"`.
     */
    @Keep
    @Suppress("unused")
    @Serializable
    data class PushSubscription(
        @SerialName("resource_id")
        override val resourceId: Int,
        val provider: String,
        @SerialName("subscription_id")
        val subscriptionId: String,
        override val status: String? = null,
        override val priority: Int? = null,
        @SerialName("custom_fields")
        override val customFields: Map<String, @Contextual Any?>? = null,
        override val cats: List<String>? = null,
    ) : Subscription {
        @SerialName("channel")
        override val channel: String = "push"
    }

    /**
     * Subscription with `cc_data`, used for Telegram, WhatsApp, Viber, and Notify channels.
     *
     * @property resourceId Resource identifier.
     * @property channel Channel type: `"telegram_bot"`, `"whatsapp"`, `"viber"`, or `"notify"`.
     * @property ccData Channel-specific data (e.g., chat ID or phone number).
     * @property status Subscription status (optional).
     * @property priority Subscription priority (optional).
     * @property customFields Standard and custom subscription fields (optional).
     * @property cats Subscription categories (optional).
     */
    @Keep
    @Suppress("unused")
    @Serializable
    data class CcDataSubscription(
        @SerialName("resource_id")
        override val resourceId: Int,
        @SerialName("channel")
        override val channel: String,
        @SerialName("cc_data")
        val ccData: JsonObject,
        override val status: String? = null,
        override val priority: Int? = null,
        @SerialName("custom_fields")
        override val customFields: Map<String, @Contextual Any?>? = null,
        override val cats: List<String>? = null,
    ) : Subscription

    /**
     * UTM params for mobile events (all optional).
     *
     * @property campaign UTM Campaign
     * @property content  UTM Content
     * @property keyword  UTM Keyword/Term
     * @property medium   UTM Medium
     * @property source   UTM Source
     * @property temp     UTM Temp
     */
    @Serializable
    data class UTM (
        val campaign: String? = null,
        val content: String? = null,
        val keyword: String? = null,
        val medium: String? = null,
        val source: String? = null,
        val temp: String? = null
    )

    /**
     * Represents a general event with associated details.
     *
     * @property function The name or identifier of the function where the event occurred.
     * @property eventCode An optional code identifying the event type.
     * @property eventMessage An optional message providing additional details about the event.
     * @property eventValue An optional value associated with the event.
     * @property date The date and time when the event occurred.
     */
    @Keep
    @Suppress("MemberVisibilityCanBePrivate")
    open class Event(
        val function: String,
        val eventCode: Int? = null,
        val eventMessage: String? = null,
        val eventValue: Map<String, Any?>? = null,
        val date: Date = Date(),
    )

    /**
     * Represents an error event, extending the general event class.
     * Used for storing information about errors.
     *
     * @param function The name or identifier of the function where the error occurred.
     * @param eventCode An optional code identifying the error type.
     * @param eventMessage An optional message providing details about the error.
     * @param eventValue An optional value associated with the error.
     * @param date The date and time when the error occurred.
     */
    @Keep
    open class Error(
        function: String,
        eventCode: Int? = 0,
        eventMessage: String? = null,
        eventValue: Map<String, Any?>? = null,
        date: Date = Date(),
    ) : Event(function, eventCode, eventMessage, eventValue, date)

    /**
     * Represents a retryable error event.
     * This class extends `Error` and is used for errors that support retry mechanisms.
     *
     * @param function The name or identifier of the function where the retryable error occurred.
     * @param eventCode An optional code identifying the retryable error type.
     * @param eventMessage An optional message providing details about the retryable error.
     * @param eventValue An optional value associated with the retryable error.
     * @param date The date and time when the retryable error occurred.
     */
    @Keep
    class RetryError(
        function: String,
        eventCode: Int? = 0,
        eventMessage: String? = null,
        eventValue: Map<String, Any?>? = null,
        date: Date = Date(),
    ) : Error(function, eventCode, eventMessage, eventValue, date)

    //internal
    /**
     * Data model for the JWT "matching" claim.
     *
     * Represents identifiers that can be used to match a profile in the platform.
     *
     * @param dbId ID of the database used for matching.
     * @param matching Matching method.
     * @param email Matching identifier: profile email.
     * @param phone Matching identifier: profile phone number.
     * @param profileId Matching identifier: unique profile ID in the platform.
     * @param fieldName Matching identifier: custom field name (for custom matching).
     * @param fieldValue Matching identifier: custom field value (string or number).
     * @param provider Matching identifier: provider code (if the matching requires it).
     * @param subscriptionId Matching identifier: subscription id.
     */
    @Serializable
    internal data class JWTMatching(
        @SerialName("db_id") val dbId: Int,
        @SerialName("matching") val matching: String,
        @SerialName("email") val email: String? = null,
        @SerialName("phone") val phone: String? = null,
        @SerialName("profile_id") val profileId: String? = null,
        @SerialName("field_name") val fieldName: String? = null,
        @SerialName("field_value") val fieldValue: JsonPrimitive? = null,
        @SerialName("provider") val provider: String? = null,
        @SerialName("subscription_id") val subscriptionId: String? = null,
    ) {
        /**
         * Builds a matching string for JWT payload.
         *
         * Order: email / phone / profileId / fieldName / fieldValue / provider / subscriptionId.
         *
         * @return Combined string if at least one matching field is present,
         *         otherwise `null` (no matching identifiers provided).
         */
        fun asString(): String? {
            val parts = buildList {
                fun addIfNotBlank(v: String?) {
                    if (!v.isNullOrBlank()) add(v)
                }

                addIfNotBlank(email)
                addIfNotBlank(phone)
                addIfNotBlank(profileId)
                addIfNotBlank(fieldName)
                fieldValue?.let { add(it.content) }
                addIfNotBlank(provider)
                addIfNotBlank(subscriptionId)
            }

            if (parts.isEmpty()) return null

            val matchingValue = parts.joinToString(separator = "/")

            return """{
            "$DB_ID": "$dbId",
            "$MATCHING": "$matching",
            "$MATCHING_ID": "$matchingValue"
        }""".trimIndent()
        }
    }

    /**
     * Holds the common data needed to construct SDK network requests.
     *
     * @property config SDK configuration containing API endpoints and rToken.
     * @property auth Pair of (authorization header, matching mode) for secure API calls.
     */
    internal data class RequestData(
        val config: ConfigurationEntity? = null,
        val auth: Pair<String, String>? = null
    )

    /**
     * Represents the data required to create a subscription request.
     *
     * @property url API endpoint URL.
     * @property time Request timestamp (epoch millis).
     * @property rToken Resource token (optional).
     * @property uid Unique request ID.
     * @property authHeader Authorization header.
     * @property matchingMode Matching type for profile search.
     * @property provider Push provider (e.g., android-firebase).
     * @property deviceToken Current device token.
     * @property status Subscription status.
     * @property sync `1` for sync, `0` for async (optional).
     * @property profileFields Profile fields payload (optional).
     * @property fields Extra subscription fields payload (optional).
     * @property cats List of category objects (optional).
     * @property replace If `true`, replaces existing subscription.
     * @property skipTriggers If `true`, skips trigger execution.
     */
    internal data class SubscribeRequestData(
        val url: String,
        val time: Long,
        val rToken: String?,
        val uid: String,
        val authHeader: String,
        val matchingMode: String,
        val provider: String,
        val deviceToken: String,
        val status: String,
        val sync: Int?,
        val profileFields: JsonElement?,
        val fields: JsonElement?,
        val cats: List<CategoryData>?,
        val replace: Boolean?,
        val skipTriggers: Boolean?,
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Contains the data required to send a token update request.
     *
     * This class holds all necessary parameters for replacing an existing token
     * and identifying the request during processing.
     *
     * @property url The endpoint to which the update request is sent.
     * @property uid The unique identifier associated with the request.
     * @property oldToken The token to be replaced.
     * @property newToken The new token to be sent.
     * @property oldProvider The provider of the old token (e.g., android-firebase).
     * @property newProvider The provider of the new token.
     * @property authHeader The authorization header used to authenticate the request.
     */
    internal data class UpdateRequestData(
        val url: String,
        val uid: String,
        val oldToken: String?,
        val newToken: String,
        val oldProvider: String?,
        val newProvider: String,
        val authHeader: String
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Data class representing the necessary data for sending a push event request.
     *
     * @property url The full API endpoint for the push event.
     * @property time The event timestamp (epoch millis).
     * @property type The push event type ("delivery", "open").
     * @property uid The unique request identifier (matches uid in PushEventEntity).
     * @property authHeader The authorization header (Bearer token).
     * @property matchingMode Matching type for profile search.
     */
    internal data class PushEventRequestData(
        val url: String,
        val time: Long,
        val type: String,
        val uid: String,
        val authHeader: String,
        val matchingMode: String
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Data class representing the necessary data for sending a mobile event request.
     *
     * @property url The full API endpoint for the mobile event.
     * @property sid The string ID of the pixel.
     * @property name The event name.
     * @property authHeader The authorization header (Bearer token).
     */
    internal data class MobileEventRequestData(
        val url: String,
        val sid: String,
        val name: String,
        val authHeader: String
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Represents the data required for a profile status request.
     *
     * @property url The full API endpoint.
     * @property uid The unique request identifier.
     * @property authHeader The authorization header (Bearer token).
     * @property matchingMode Matching type for profile search.
     * @property provider The provider identifier (optional).
     * @property token The token associated with the provider (optional).
     */
    internal data class StatusRequestData(
        val url: String,
        val uid: String,
        val authHeader: String,
        val matchingMode: String,
        val provider: String?,
        val token: String?
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Data model for the unSuspend request payload.
     *
     * @property url The full API endpoint.
     * @property uid The unique request identifier.
     * @property provider Push notification provider name.
     * @property token Device push token.
     * @property authHeader The authorization header (Bearer token).
     * @property matchingMode Matching type for profile search.
     */
    internal data class UnSuspendRequestData(
        val url: String,
        val uid: String,
        val provider: String,
        val token: String,
        val authHeader: String,
        val matchingMode: String,
    ) : com.altcraft.sdk.interfaces.RequestData

    /**
     * Holds all contextual data extracted from a processed response.
     *
     * @property status The result status (SUCCESS, RETRY, or ERROR).
     * @property errorPair A generated message describing the error or failure.
     * @property successPair A generated message for a successful result.
     * @property eventValue Map with additional event-related metadata.
     */
    internal data class ResponseResult(
        val status: com.altcraft.sdk.network.Response.ResponseStatus,
        val httpCode: Int,
        val error: Int?,
        val errorPair: Pair<Int, String>,
        val successPair: Pair<Int, String>,
        val eventValue: Map<String, Any?>,
        val response: Response?
    )

    /**
     * Notification data used to build and display a notification.
     *
     * @property uid Unique identifier for the notification.
     * @property title Notification title.
     * @property body Notification content text.
     * @property icon Icon resource ID.
     * @property messageId Internal message ID.
     * @property channelInfo Notification channel name and description.
     * @property smallImg Small image (icon/logo), nullable.
     * @property largeImage Large image (banner), nullable.
     * @property color Accent color.
     * @property pendingIntent Click action.
     * @property buttons Optional action buttons.
     */
    internal data class NotificationData(
        val uid: String,
        val title: String,
        val body: String,
        val icon: Int,
        val messageId: Int,
        val channelInfo: Pair<String, String>,
        val smallImg: Bitmap?,
        val largeImage: Bitmap?,
        val color: Int,
        val pendingIntent: PendingIntent,
        val buttons: List<ButtonStructure>?
    )

    /**
     * Represents the structure of buttons in push notifications.
     *
     * @property label The label or text displayed on the button.
     * @property link The URL or deep link to navigate to a specific section of the application
     *                after clicking the button.
     */
    @Serializable
    internal data class ButtonStructure(val label: String, val link: String)

    /**
     * Represents the execution states of periodic workers related to **push notifications**.
     *
     * @property subscribeWorkState The state of the push subscription worker.
     * @property updateWorkState The state of the push token/update worker.
     * @property pushEventWorkState The state of the push event worker.
     */
    internal data class PushWorkersState(
        val subscribeWorkState: WorkInfo.State?,
        val updateWorkState: WorkInfo.State?,
        val pushEventWorkState: WorkInfo.State?
    )
}