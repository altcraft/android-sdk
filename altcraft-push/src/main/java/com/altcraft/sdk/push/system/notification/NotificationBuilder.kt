package com.altcraft.sdk.push.system.notification

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.R
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Preferenses.getMessageId
import com.altcraft.sdk.push.data.models.PushModels.NotificationData
import com.altcraft.sdk.push.system.notification.PushChannel.getChannelInfo
import com.altcraft.sdk.push.system.notification.PushImage.loadLargeImage
import com.altcraft.sdk.push.system.notification.PushImage.loadSmallImage
import com.altcraft.sdk.push.system.receiver.PushData
import com.altcraft.sdk.push.util.Utils.getIconColor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Builds notification data required for push notification presentation.
 */
internal object NotificationBuilder {

    /**
     * Retrieves data required for creating a notification
     * based on the provided message map.
     *
     * This function extracts and processes the necessary information
     * from the incoming [message] map to construct a [NotificationData]
     * object containing all details needed to display a notification.
     *
     * @param context Application context used for accessing resources and utilities.
     * @param message Map containing push notification data.
     * @return Notification data, or `null` if an error occurs.
     */
    suspend fun getNotificationData(
        context: Context,
        message: Map<String, String>
    ): NotificationData? {
        return try {
            val pushData = PushData(message)
            val config = Environment.create(context)
                .pushConfig()

            val icon = config.icon ?: R.drawable.icon
            val smallImage = loadSmallImage(context, pushData)
            val largeImage = loadLargeImage(context, pushData)
            val channelInfo = getChannelInfo(config, pushData)
            val color = getIconColor(pushData.color)
            val messageId = getMessageId(context)
            val extra = pushData.extra

            NotificationData(
                uid = pushData.uid,
                title = pushData.title,
                body = pushData.body,
                icon = icon,
                smallImg = smallImage,
                largeImage = largeImage,
                color = color,
                extra = extra,
                url = pushData.url,
                messageId = messageId,
                buttons = pushData.buttons,
                channelInfo = channelInfo,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getNotificationData", e)
            null
        }
    }
}
