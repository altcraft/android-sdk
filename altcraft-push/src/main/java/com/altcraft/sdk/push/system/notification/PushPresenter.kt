package com.altcraft.sdk.push.system.notification

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.altcraft.sdk.push.util.Log.event
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.system.action.Intent.getIntent
import com.altcraft.sdk.push.system.notification.NotificationBuilder.getNotificationData
import com.altcraft.sdk.push.data.Constants.PUSH
import com.altcraft.sdk.push.data.models.PushModels.NotificationData
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.system.notification.PushChannel.channelsSupportedForCurrentVersion
import com.altcraft.sdk.push.system.notification.PushChannel.isChannelCreated
import com.altcraft.sdk.push.system.notification.PushChannel.selectAndCreateChannel
import com.altcraft.sdk.push.util.Utils.hasNotificationPermission
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages creation and display of push notifications.
 */
internal object PushPresenter {

    /**
     * Displays the specified notification.
     *
     * @param context Application context.
     * @param id Notification ID.
     * @param notification Notification to display.
     */
    @SuppressLint("MissingPermission")
    private fun notify(
        context: Context, id: Int, notification: Notification
    ) = NotificationManagerCompat.from(context).notify(
        id, notification
    )

    /**
     * Creates and displays a push notification, configuring its appearance and behavior
     * based on the provided data map.
     *
     * @param context The context used to create and display the notification.
     * @param push A map of key-value pairs representing the push notification payload.
     */
    suspend fun showPush(context: Context, push: Map<String, String>) {
        try {
            if (!hasNotificationPermission(context)) exception(ModuleEvents.pushPermissionDenied)
            val data = getNotificationData(context, push) ?: exception(ModuleEvents.pushDataIsNull)
            val notify = createNotification(context, data) ?: exception(ModuleEvents.notificationErr)
            if (channelsSupportedForCurrentVersion) selectAndCreateChannel(context, data.channelInfo)
            if (!isChannelCreated(context, data.channelInfo)) exception(ModuleEvents.channelNotCreated)
            notify(context, data.messageId, notify)
            event("showPush", event = ModuleEvents.pushIsPosted, eventValue = mapOf(PUSH to push))
        } catch (e: CancellationException) {
            throw e
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
        data: NotificationData
    ): Notification? {
        return try {
            val intent = getIntent(
                context, data.messageId, data.url, data.uid, data.extra
            )
            NotificationCompat.Builder(context, data.channelInfo.first)
                .apply {
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

    /**
     * Adds action buttons to the notification.
     *
     * For each button in [NotificationData.buttons], a corresponding
     * PendingIntent is created with the button URL, message ID, UID, and extras.
     * If no buttons are provided, nothing is added.
     *
     * @receiver The target [NotificationCompat.Builder].
     * @param context Application context used to create PendingIntents.
     * @param data Notification model containing button and metadata information.
     * @return The same [NotificationCompat.Builder] instance.
     */
    private fun NotificationCompat.Builder.addActions(
        context: Context,
        data: NotificationData
    ): NotificationCompat.Builder {
        data.buttons?.forEach {
            try {
                addAction(
                    0, it.label, getIntent(
                        context,
                        data.messageId,
                        it.link,
                        data.uid,
                        data.extra
                    )
                )
            } catch (e: Exception) {
                error("addActions", e)
            }
        }
        return this
    }

    /**
     * Applies [NotificationCompat.BigPictureStyle] to show a large image in the notification.
     *
     * The style is applied only when a non-null `largeImage` is available in [data].
     * Errors are caught to avoid crashes during rendering.
     *
     * @receiver The target [NotificationCompat.Builder].
     * @param data Notification content including title and images.
     * @return The same [NotificationCompat.Builder] with the style applied if possible,
     *         or unchanged if no large image is available or an error occurs.
     */
    private fun NotificationCompat.Builder.applyBigPictureStyle(
        data: NotificationData
    ): NotificationCompat.Builder {
        try {
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(data.largeImage ?: return this)
                    .setBigContentTitle(data.title)
            )
        } catch (e: Exception) {
            error("applyBigPictureStyle", e)
        }
        return this
    }
}