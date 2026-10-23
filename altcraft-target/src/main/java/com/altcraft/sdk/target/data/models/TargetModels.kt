package com.altcraft.sdk.target.data.models

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.target.data.serializer.SubscriptionSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Models of the Altcraft Target module.
 *
 * Contains:
 * - [TargetModels.UTM] — UTM parameters for target events;
 * - [TargetModels.SubscriptionModel] — subscription models for different communication channels.
 */
@Suppress("unused")
object TargetModels {

    /**
     * UTM params for mobile events (all optional).
     *
     * @property campaign UTM Campaign.
     * @property content  UTM Content.
     * @property keyword  UTM Keyword/Term.
     * @property medium   UTM Medium.
     * @property source   UTM Source.
     * @property temp     UTM Temp.
     *
     * Usage:
     * ```kotlin
     * import com.altcraft.sdk.target.data.models.TargetModels.UTM
     *
     * AltcraftSDK.target.sendTarget(
     *     context = context,
     *     sid = "00000",
     *     eventName = "install",
     *     utm = UTM(campaign = "spring_sale", medium = "cpc", source = "google")
     * )
     * ```
     */
    @Serializable
    data class UTM(
        val campaign: String? = null,
        val content: String? = null,
        val keyword: String? = null,
        val medium: String? = null,
        val source: String? = null,
        val temp: String? = null
    )

    /**
     * Contains subscription request models used by mobile events.
     *
     * These models describe subscriptions to different communication channels:
     * - email;
     * - SMS;
     * - push;
     * - channels with `cc_data`, such as Telegram, WhatsApp, Viber, and Notify.
     */
    object SubscriptionModel {

        /**
         * Base interface for all subscription types added to a profile via mobile events.
         *
         * @property resourceId Resource identifier.
         * @property status Subscription status (optional).
         * @property priority Subscription priority (optional).
         * @property customFields Standard and custom subscription fields (optional).
         * @property cats Subscription categories (optional).
         * @property channel Channel type.
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.Subscription
         *
         * fun send(s: Subscription) {
         *     AltcraftSDK.target.sendTarget(context, "00000", "subscribe", subscription = s)
         * }
         * ```
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
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.EmailSubscription
         *
         * val subscription = EmailSubscription(resourceId = 11, email = "user@example.com")
         * AltcraftSDK.target.sendTarget(context, "00000", "subscribe", subscription = subscription)
         * ```
         */
        @Keep
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
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.SmsSubscription
         *
         * val subscription = SmsSubscription(resourceId = 12, phone = "+79160000000")
         * ```
         */
        @Keep
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
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.PushSubscription
         *
         * val subscription = PushSubscription(
         *     resourceId = 13,
         *     provider = "android-firebase",
         *     subscriptionId = "some-subscription-token"
         * )
         * ```
         */
        @Keep
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
         *
         * Usage:
         * ```kotlin
         * import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.CcDataSubscription
         * import kotlinx.serialization.json.buildJsonObject
         * import kotlinx.serialization.json.put
         *
         * val subscription = CcDataSubscription(
         *     resourceId = 14,
         *     channel = "telegram_bot",
         *     ccData = buildJsonObject { put("chat_id", 123) }
         * )
         * ```
         */
        @Keep
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
    }
}
