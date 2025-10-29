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
     * Adds interactive action buttons to the notification.
     *
     * Each button attaches a PendingIntent so that tapping the action triggers
     * the associated deep link or URL.
     *
     * @receiver The target [NotificationCompat.Builder].
     * @param context Application context used to create pending intents.
     * @param messageId Notification message ID used to distinguish intents.
     * @param buttons Optional list of action descriptors to attach; if `null`, nothing is added.
     * @param uid Unique identifier passed with each intent for tracking.
     * @return The same [NotificationCompat.Builder] with actions appended (if any).
     */
    fun NotificationCompat.Builder.addActions(
        context: Context,
        messageId: Int,
        buttons: List<DataClasses.ButtonStructure>?,
        uid: String
    ): NotificationCompat.Builder {
        buttons?.forEach {
            try {
                addAction(0, it.label, getIntent(context, messageId, it.link, uid))
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