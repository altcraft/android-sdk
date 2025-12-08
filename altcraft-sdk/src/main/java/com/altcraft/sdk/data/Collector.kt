package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.StringBuilder.eventPushUrl
import com.altcraft.sdk.additional.StringBuilder.statusUrl
import com.altcraft.sdk.additional.StringBuilder.subscribeUrl
import com.altcraft.sdk.additional.StringBuilder.unSuspendUrl
import com.altcraft.sdk.additional.StringBuilder.updateUrl
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.push.token.TokenManager.getCurrentPushToken
import com.altcraft.sdk.data.Preferenses.getMessageId
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.push.PushData
import com.altcraft.sdk.push.PushImage.loadLargeImage
import com.altcraft.sdk.push.PushImage.loadSmallImage
import java.util.UUID
import com.altcraft.altcraftsdk.R
import com.altcraft.sdk.additional.StringBuilder.eventMobileUrl
import com.altcraft.sdk.push.PushChannel.getChannelInfo
import com.altcraft.sdk.additional.SubFunction.getIconColor
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.push.action.Intent.getIntent

/**
 * Builds request data for SDK network calls and composes notification payloads.
 *
 * Centralizes retrieval of config/auth/token data and assembles strongly-typed
 * request models for subscriptions, updates, push events, mobile events, and status checks.
 */
internal object Collector {

    /**
     * Builds a `SubscribeRequestData` object for a push notification subscription request.
     *
     * Retrieves configuration, authentication headers, and the device token from `context`
     * and `SubscribeEntity`.
     * Combines system, application-specific, and custom fields.
     * Returns `null` if required data is missing or an error occurs.
     *
     * @param context The context for accessing configuration and token data.
     * @param subscription The subscription details (status, custom fields, settings).
     * @return A `SubscribeRequestData` object or `null` if data retrieval fails.
     */
    suspend fun getSubscribeRequestData(
        context: Context,
        subscription: SubscribeEntity
    ): DataClasses.SubscribeRequestData? {
        return try {
            val env = Environment.create(context)

            DataClasses.SubscribeRequestData(
                url = subscribeUrl(env.config().apiUrl),
                time = subscription.time,
                rToken = env.config().rToken,
                uid = subscription.uid,
                authHeader = env.auth().first,
                matchingMode = env.auth().second,
                provider = env.token().provider,
                deviceToken = env.token().token,
                status = subscription.status,
                sync = subscription.sync,
                profileFields = subscription.profileFields,
                fields = subscription.customFields,
                cats = subscription.cats,
                replace = subscription.replace,
                skipTriggers = subscription.skipTriggers
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
            val env = Environment.create(context)

            DataClasses.UpdateRequestData(
                url = updateUrl(env.config().apiUrl),
                uid = uid,
                oldToken = env.savedToken?.token,
                newToken = env.token().token,
                oldProvider = env.savedToken?.provider,
                newProvider = env.token().provider,
                authHeader = env.auth().first
            )
        } catch (e: Exception) {
            error("getUpdateData", e)
            null
        }
    }


    /**
     * Builds request data for the unSuspend request.
     *
     * @param context The application context.
     * @return A [DataClasses.UnSuspendRequestData] object or `null` if an error occurs.
     */
    suspend fun getUnSuspendRequestData(
        context: Context,
    ): DataClasses.UnSuspendRequestData? {
        return try {
            val env = Environment.create(context)
            val uid = UUID.randomUUID().toString()

            DataClasses.UnSuspendRequestData(
                url = unSuspendUrl(env.config().apiUrl),
                uid = uid,
                provider = env.token().provider,
                token = env.token().token,
                authHeader = env.auth().first,
                matchingMode = env.auth().second,
            )
        } catch (e: Exception) {
            error("getUnSuspendRequestData", e)
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
            val env = Environment.create(context)

            DataClasses.PushEventRequestData(
                url = eventPushUrl(
                    env.config().apiUrl, event.type
                ),
                uid = event.uid,
                time = event.time,
                type = event.type,
                authHeader = env.auth().first,
                matchingMode = env.auth().second
            )
        } catch (e: Exception) {
            error("getPushEventData", e)
            null
        }
    }
    /**
     * Retrieves the necessary data for sending a mobile event to the server.
     *
     * This function constructs the full request URL and gathers required authentication details.
     * It also builds multipart/form-data parts from the provided entity. If some values are missing,
     * they are excluded from the final request body by `buildMobileEventParts`.
     *
     * @param context The application context.
     * @param event   The mobile event entity containing event details.
     * @return A [DataClasses.MobileEventRequestData] object with all required data,
     * or null if an error occurs.
     */
    suspend fun getMobileEventRequestData(
        context: Context,
        event: MobileEventEntity
    ): DataClasses.MobileEventRequestData? {
        return try {
            val env = Environment.create(context)

            DataClasses.MobileEventRequestData(
                url = eventMobileUrl(env.config().apiUrl),
                sid = event.sid,
                name = event.eventName,
                authHeader = env.auth().first
            )
        } catch (e: Exception) {
            error("getMobileEventRequestData", e)
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
            val env = Environment.create(context)

            val currentToken = getCurrentPushToken(context)
            val pushToken = env.savedToken ?: currentToken
            val uid = UUID.randomUUID().toString()

            DataClasses.StatusRequestData(
                url = statusUrl(env.config().apiUrl),
                uid = uid,
                provider = pushToken?.provider,
                token = pushToken?.token,
                authHeader = env.auth().first,
                matchingMode = env.auth().second
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
            val channelInfo = getChannelInfo(config, pushData)
            val color = getIconColor(pushData.color)
            val messageId = getMessageId(context)

            val intent = getIntent(context, messageId, pushData.url, pushData.uid)

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
}