package com.altcraft.sdk.push.data.models

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.altcraft.sdk.push.data.Constants.PROVIDER
import com.altcraft.sdk.push.data.Constants.TOKEN
import kotlinx.serialization.Serializable

/**
 * Models of the Altcraft Push module.
 *
 * Contains:
 * - [PushModels.NotificationData] — notification rendering data;
 * - [PushModels.ButtonStructure] — notification action button data;
 * - [PushModels.TokenData] — push provider token data.
 */
@Suppress("unused")
object PushModels {

    /**
     * Immutable data required to construct and display a notification.
     *
     * @property uid Unique push message identifier.
     * @property title Notification title.
     * @property body Notification body text.
     * @property icon Resource ID of the notification icon.
     * @property smallImg Small notification image, or `null` if not provided.
     * @property largeImage Large notification image, or `null` if not provided.
     * @property color Notification accent color.
     * @property url URL opened when the notification is clicked.
     * @property extra Additional payload forwarded to the application.
     * @property messageId Local notification identifier.
     * @property buttons Notification action buttons, or `null` if not provided.
     * @property channelInfo Notification channel ID and name.
     */
    internal data class NotificationData(
        val uid: String,
        val title: String,
        val body: String,
        val icon: Int,
        val smallImg: Bitmap?,
        val largeImage: Bitmap?,
        val color: Int,
        val url: String,
        val extra: String,
        val messageId: Int,
        val buttons: List<ButtonStructure>?,
        val channelInfo: Pair<String, String>,
    )

    /**
     * Represents an action button displayed in a push notification.
     *
     * @property label Text displayed on the button.
     * @property link Link opened when the button is clicked.
     */
    @Serializable
    internal data class ButtonStructure(
        val label: String,
        val link: String,
    )

    /**
     * Contains push provider token data.
     *
     * @property provider Push notification provider identifier.
     * @property token Push token received from the provider.
     *
     * Usage:
     * ```kotlin
     * import com.altcraft.sdk.push.data.models.PushModels.TokenData
     *
     * val tokenData = TokenData(
     *     provider = "android-firebase",
     *     token = "fcm-token"
     * )
     * ```
     */
    @Keep
    @Serializable
    data class TokenData(
        val provider: String,
        val token: String,
    ) {

        /** Converts token data to a key-value map. */
        internal fun toMap() = mapOf(
            PROVIDER to provider,
            TOKEN to token,
        )
    }
}
