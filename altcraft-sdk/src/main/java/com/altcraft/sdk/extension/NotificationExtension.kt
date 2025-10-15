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
 * `NotificationExtension` provides utilities to enhance notifications
 * with actions and styles like BigPicture.
 */
internal object NotificationExtension {

    /**
     * Adds interactive action buttons to the notification.
     *
     * Each button triggers an intent when clicked, allowing
     * the notification to perform specific actions.
     *
     * @param context Used to create pending intents.
     * @param buttons Optional list of actions to attach.
     * @param uid Unique ID passed with each intent.
     * @return Notification builder with actions added.
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
     * Applies BigPictureStyle to show a large image in the notification.
     *
     * If an image is available, the style is set with the given title.
     * Errors are caught to prevent crashes during rendering.
     *
     * @param data Notification content including title and image.
     * @return Notification builder with style applied if possible.
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