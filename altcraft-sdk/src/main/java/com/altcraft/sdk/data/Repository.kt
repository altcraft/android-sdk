package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.graphics.Color
import com.altcraft.sdk.additional.StringBuilder.eventPushUrl
import com.altcraft.sdk.additional.StringBuilder.profileUrl
import com.altcraft.sdk.additional.StringBuilder.subscribeUrl
import com.altcraft.sdk.additional.StringBuilder.unSuspendUrl
import com.altcraft.sdk.additional.StringBuilder.updateUrl
import com.altcraft.sdk.auth.AuthManager.getAuthHeaderAndMatching
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.push.token.TokenManager.getCurrentToken
import com.altcraft.sdk.data.Preferenses.getMessageId
import com.altcraft.sdk.data.Preferenses.getSavedToken
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.events.EventList.authDataIsNull
import com.altcraft.sdk.events.EventList.commonDataIsNull
import com.altcraft.sdk.events.EventList.configIsNull
import com.altcraft.sdk.events.EventList.currentTokenIsNull
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.push.action.PushAction.getIntent
import com.altcraft.sdk.push.PushData
import com.altcraft.sdk.push.PushImage.loadLargeImage
import com.altcraft.sdk.push.PushImage.loadSmallImage
import java.util.UUID
import com.altcraft.altcraftsdk.R
import com.altcraft.sdk.push.PushChannel.getChannelInfo
import androidx.core.graphics.toColorInt

/**
 *  The DataFactory object is designed to convert JSON objects into Data classes,
 *  to work with the Data type of the androidx.work package.
 */
internal object Repository {

    /**
     * Retrieves common data required for the subscription process.
     *
     * This function gathers the necessary configuration, authentication, and device token data
     * needed for constructing a subscription request. If any of the required data is missing,
     * an appropriate event is logged, and `null` is returned.
     *
     * @param context The application context used for retrieving configuration and device token.
     * @return A `CommonData` object containing configuration, provider, device token,
     *         authentication header, and matching mode, or `null` if any required data is missing.
     */
    private suspend fun getCommonData(context: Context): DataClasses.CommonData? {
        return try {
            val config = getConfig(context) ?: exception(configIsNull)
            val tokenData = getCurrentToken(context) ?: exception(currentTokenIsNull)
            val authData = getAuthHeaderAndMatching(config) ?: exception(authDataIsNull)

            val savedToken = getSavedToken(context)

            DataClasses.CommonData(
                config = config,
                currentToken = tokenData,
                savedToken = savedToken,
                authHeader = authData.first,
                matchingMode = authData.second
            )
        } catch (e: Exception) {
            error("getCommonData", e)
            null
        }
    }

    /**
     * Builds a `SubscribeRequestData` object for a push notification subscription request.
     *
     * Retrieves configuration, authentication headers, and the device token from `context`
     * and `SubscribeEntity`.
     * Combines system, application-specific, and custom fields.
     * Returns `null` if required data is missing or an error occurs.
     *
     * @param context The context for accessing configuration and token data.
     * @param item The subscription details (status, custom fields, settings).
     * @return A `SubscribeRequestData` object or `null` if data retrieval fails.
     */
    suspend fun getSubscribeRequestData(
        context: Context,
        item: SubscribeEntity
    ): DataClasses.SubscribeRequestData? {
        return try {
            val commonData = getCommonData(context) ?: exception(commonDataIsNull)
            val url = subscribeUrl(commonData.config.apiUrl)

            DataClasses.SubscribeRequestData(
                url = url,
                uid = item.uid,
                time = item.time,
                rToken = commonData.config.rToken,
                authHeader = commonData.authHeader,
                matchingMode = commonData.matchingMode,
                provider = commonData.currentToken.provider,
                deviceToken = commonData.currentToken.token,
                status = item.status,
                sync = item.sync,
                profileFields = item.profileFields,
                fields = item.customFields,
                cats = item.cats,
                replace = item.replace,
                skipTriggers = item.skipTriggers
            )

        } catch (e: Exception) {
            error("getSubscribeData", e)
            null
        }
    }

    /**
     * Builds an `UpdateRequestData` object for updating a push notification subscription.
     *
     * Retrieves configuration, authentication headers, and device tokens from `context`.
     * Returns `null` if required data is missing.
     *
     * @param context The context for accessing configuration and tokens.
     * @param uid The unique request identifier.
     * @return An `UpdateRequestData` object or `null` if data retrieval fails.
     */
    suspend fun getUpdateRequestData(
        context: Context,
        uid: String,
    ): DataClasses.UpdateRequestData? {
        return try {
            val commonData = getCommonData(context) ?: exception(commonDataIsNull)
            val url = updateUrl(commonData.config.apiUrl)

            DataClasses.UpdateRequestData(
                url = url,
                uid = uid,
                authHeader = commonData.authHeader,
                oldToken = commonData.savedToken?.token,
                newToken = commonData.currentToken.token,
                oldProvider = commonData.savedToken?.provider,
                newProvider = commonData.currentToken.provider
            )
        } catch (e: Exception) {
            error("getUpdateData", e)
            null
        }
    }

    /**
     * Retrieves the necessary data for sending a push event to the server.
     *
     * This function constructs the full request URL and gathers required authentication details.
     * If some values are missing, they are excluded from the final request.
     *
     * @param context The application context.
     * @param event The push event entity containing event details.
     * @return A PushEventRequestData object with all required data, or null if an error occurs.
     */
    suspend fun getPushEventRequestData(
        context: Context,
        event: PushEventEntity
    ): DataClasses.PushEventRequestData? {
        return try {
            val commonData = getCommonData(context) ?: exception(commonDataIsNull)
            val url = eventPushUrl(commonData.config.apiUrl, event.type)

            DataClasses.PushEventRequestData(
                url = url,
                uid = event.uid,
                time = event.time,
                type = event.type,
                authHeader = commonData.authHeader,
                matchingMode = commonData.matchingMode
            )
        } catch (e: Exception) {
            error("getPushEventData", e)
            null
        }
    }

    /**
     * Builds request data for the unSuspend request.
     *
     * @param context The application context.
     * @return A [DataClasses.UnSuspendRequestData] object or `null` if an error occurs.
     */
    suspend fun getUnSubscribeRequestData(
        context: Context,
    ): DataClasses.UnSuspendRequestData? {
        return try {
            val commonData = getCommonData(context) ?: exception(commonDataIsNull)
            val url = unSuspendUrl(commonData.config.apiUrl)
            val uid = UUID.randomUUID().toString()

            DataClasses.UnSuspendRequestData(
                url = url,
                uid = uid,
                provider = commonData.currentToken.provider,
                token = commonData.currentToken.token,
                authHeader = commonData.authHeader,
                matchingMode = commonData.matchingMode
            )
        } catch (e: Exception) {
            error("getProfileData", e)
            null
        }
    }

    /**
     * Gathers data to create a `ProfileRequestData` object.
     *
     * Retrieves configuration, authentication headers, device token, and other required data from
     * the `context`. Returns `ProfileRequestData` or `null` if any required data is missing.
     *
     * @param context The context for accessing configuration and tokens.
     * @return A `ProfileRequestData` object or `null` on failure.
     */
    suspend fun getStatusRequestData(
        context: Context,
    ): DataClasses.StatusRequestData? {
        return try {
            val commonData = getCommonData(context) ?: exception(commonDataIsNull)
            val url = profileUrl(commonData.config.apiUrl)
            val tokenData = getToken(context, commonData)
            val uid = UUID.randomUUID().toString()

            DataClasses.StatusRequestData(
                url = url,
                uid = uid,
                provider = tokenData?.provider,
                token = tokenData?.token,
                authHeader = commonData.authHeader,
                matchingMode = commonData.matchingMode
            )
        } catch (e: Exception) {
            error("getProfileData", e)
            null
        }
    }

    /**
     * Retrieves data required for creating a notification based on the provided message map.
     *
     * This function extracts and processes the necessary information from the incoming `message`
     * map to construct a `NotificationData` object, which contains all the details needed to
     * display a notification.
     *
     * @param context The application context used for accessing resources and utilities.
     * @param message A map of key-value pairs containing push notification data.
     * @return A `NotificationData` object containing the notification details, or `null` if an
     * exception occurs.
     */
    suspend fun getNotificationData(
        context: Context,
        message: Map<String, String>
    ): DataClasses.NotificationData? {
        return try {
            val pushData = PushData(message)
            val config = getConfig(context)

            val icon = config?.icon ?: R.drawable.icon
            val smallImage = loadSmallImage(context, pushData)
            val largeImage = loadLargeImage(context, pushData)

            val messageId = getMessageId(context)
            val channelInfo = getChannelInfo(config, pushData)
            val intent = getIntent(context, pushData.url, pushData.uid)

            val color = runCatching { pushData.color.toColorInt() }
                .getOrElse { Color.BLACK }

            DataClasses.NotificationData(
                uid = pushData.uid,
                title = pushData.title,
                body = pushData.body,
                icon = icon,
                messageId = messageId,
                channelInfo = channelInfo,
                smallImg = smallImage,
                largeImage = largeImage,
                color = color,
                pendingIntent = intent,
                buttons = pushData.buttons
            )
        } catch (e: Exception) {
            error("getNotificationData", e)
            null
        }
    }

    /**
     * Resolves the push token to be used in requests.
     *
     * Falls back to `null` in case of any unexpected exception.
     *
     * @param context The Android context used to access saved token storage.
     * @param data The common data object containing config and device token.
     * @return A resolved token string or `null` if unavailable or on error.
     */
    private fun getToken(context: Context, data: DataClasses.CommonData): DataClasses.TokenData? {
        return try {
            if (data.config.rToken != null) {
                data.savedToken ?: data.currentToken
            } else getSavedToken(context)
        } catch (_: Exception) {
            null
        }
    }
}