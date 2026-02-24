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
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.EventList.permissionDenied
import com.altcraft.sdk.sdk_events.EventList.pushDataIsNull
import com.altcraft.sdk.sdk_events.EventList.pushIsPosted
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.NotificationExtension.addActions
import com.altcraft.sdk.extension.NotificationExtension.applyBigPictureStyle
import com.altcraft.sdk.data.Collector.getNotificationData
import com.altcraft.sdk.sdk_events.EventList.channelNotCreated
import com.altcraft.sdk.sdk_events.EventList.notificationErr
import com.altcraft.sdk.push.PushChannel.isChannelCreated
import com.altcraft.sdk.push.PushChannel.selectAndCreateChannel
import com.altcraft.sdk.push.PushChannel.versionSupportChannels
import com.altcraft.sdk.push.action.Intent.getIntent

/**
 * Manages creation and display of push notifications.
 */
internal object PushPresenter {

    /**
     * Creates and displays a push notification, configuring its appearance and behavior
     * based on the provided data map.
     *
     * @param context The context used to create and display the notification.
     * @param push A map of key-value pairs representing the push notification payload.
     */
    suspend fun showPush(context: Context, push: Map<String, String>) {
        try {
            if (!checkingNotificationPermission(context)) exception(permissionDenied)
            val data = getNotificationData(context, push) ?: exception(pushDataIsNull)
            val notify = createNotification(context, data) ?: exception(notificationErr)
            if (versionSupportChannels) selectAndCreateChannel(context, data.channelInfo)
            if (!isChannelCreated(context, data.channelInfo)) exception(channelNotCreated)
            NotificationManagerCompat.from(context).notify(data.messageId, notify)
            event("showPush", event = pushIsPosted, value = mapOf("push" to push))
        } catch (e: Exception) {
            error("showPush", e)
        }
    }

    /**
     * Creates a notification for a push message using structured notification data.
     *
     * @param context The application context used to build the notification.
     * @param data Structured notification data (already parsed and validated).
     * @return A [Notification] instance, or `null` if building fails.
     */
    private fun createNotification(
        context: Context,
        data: DataClasses.NotificationData
    ): Notification? {
        return try {
            val intent = getIntent(
                context, data.messageId, data.url, data.uid, data.extra
            )
            NotificationCompat.Builder(
                context, data.channelInfo.first
            ).apply {
                setPriority(NotificationCompat.PRIORITY_MAX)
                data.smallImg?.let { setLargeIcon(it) }
                applyBigPictureStyle(data)
                setSmallIcon(data.icon)
                setContentTitle(data.title)
                setContentText(data.body)
                setAutoCancel(true)
                setColor(data.color)
                setContentIntent(intent)
                addActions(context, data)
            }.build()
        } catch (e: Exception) {
            error("createNotification", e)
            null
        }
    }
}