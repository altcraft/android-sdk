package com.altcraft.sdk.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.core.app.NotificationCompat
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.push.action.Intent.getIntent

/**
 * Utilities to enrich [NotificationCompat.Builder] with actions and styles,
 * including applying [NotificationCompat.BigPictureStyle].
 */
internal object NotificationExtension {

    /**
     * Adds action buttons to the notification.
     *
     * For each button in [DataClasses.NotificationData.buttons], a corresponding
     * PendingIntent is created with the button URL, message ID, UID, and extras.
     * If no buttons are provided, nothing is added.
     *
     * @receiver The target [NotificationCompat.Builder].
     * @param context Application context used to create PendingIntents.
     * @param data Notification model containing button and metadata information.
     * @return The same [NotificationCompat.Builder] instance.
     */
    fun NotificationCompat.Builder.addActions(
        context: Context,
        data: DataClasses.NotificationData
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
    fun NotificationCompat.Builder.applyBigPictureStyle(
        data: DataClasses.NotificationData
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