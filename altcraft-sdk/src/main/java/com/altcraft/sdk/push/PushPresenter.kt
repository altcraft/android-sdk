package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.altcraft.sdk.additional.SubFunction.checkingNotificationPermission
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.events.EventList.permissionDenied
import com.altcraft.sdk.events.EventList.pushDataIsNull
import com.altcraft.sdk.events.EventList.pushIsPosted
import com.altcraft.sdk.events.Events.event
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.NotificationExtension.addActions
import com.altcraft.sdk.extension.NotificationExtension.applyBigPictureStyle
import com.altcraft.sdk.data.Repository.getNotificationData
import com.altcraft.sdk.events.EventList.channelNotCreated
import com.altcraft.sdk.events.EventList.notificationErr
import com.altcraft.sdk.push.PushChannel.isChannelCreated
import com.altcraft.sdk.push.PushChannel.selectAndCreateChannel
import com.altcraft.sdk.push.PushChannel.versionsSupportChannels

/**
 * Manages push notifications and channels within the application.
 */
internal object PushPresenter {

    /**
     * Creates and displays a push notification, configuring its appearance and behavior based on
     * the provided `RemoteMessage`.
     *
     * @param context The context used to create and display the notification.
     * @param push The `RemoteMessage` object containing the data for the notification.
     */
    suspend fun showPush(context: Context, push: Map<String, String>) {
        try {
            if (!checkingNotificationPermission(context)) exception(permissionDenied)
            val data = getNotificationData(context, push) ?: exception(pushDataIsNull)
            val notify = createNotification(context, data) ?: exception(notificationErr)
            if (versionsSupportChannels) selectAndCreateChannel(context, data.channelInfo)
            if (!isChannelCreated(context, data.channelInfo)) exception(channelNotCreated)
            NotificationManagerCompat.from(context).notify(data.messageId, notify)
            event("showPush", pushIsPosted)
        } catch (e: Exception) {
            error("showPush", e)
        }
    }

    /**
     * Creates a notification for push using the provided message data.
     *
     * @param context The application context used to create the notification.
     * @param data The data map containing notification details.
     * @return A `Notification` object or `null` if an error occurs.
     */
    private fun createNotification(
        context: Context,
        data: DataClasses.NotificationData
    ): Notification? {
        return try {
            NotificationCompat.Builder(
                context, data.channelInfo.first
            ).apply {
                data.smallImg?.let { setLargeIcon(it) }
                applyBigPictureStyle(data)
                setSmallIcon(data.icon)
                setContentTitle(data.title)
                setContentText(data.body)
                setAutoCancel(true)
                setContentIntent(data.pendingIntent)
                addActions(context, data.buttons, data.uid)
                setPriority(NotificationCompat.PRIORITY_MAX)
                setColor(data.color)
            }.build()
        } catch (e: Exception) {
            error("createNotification", e)
            null
        }
    }

    /**
     * Creates a notification for foreground services and workers using ForegroundInfo.
     *
     * @param context The context used to build the notification.
     * @param channelId The ID of the notification channel.
     * @param body The text content of the notification.
     * @param icon The resource ID of the small icon to display.
     * @return A constructed [Notification] instance.
     */
    internal fun createNotification(
        context: Context,
        channelId: String,
        body: String,
        icon: Int
    ): Notification? {
        return try {
            NotificationCompat.Builder(context, channelId)
                .setContentText(body)
                .setSmallIcon(icon)
                .build()
        } catch (e: Exception) {
            error("createNotification", e)
            null
        }
    }
}