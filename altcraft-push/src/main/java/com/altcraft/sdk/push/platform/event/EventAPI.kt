package com.altcraft.sdk.push.platform.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.push.data.Constants.UID
import com.altcraft.sdk.push.platform.event.EventProcess.sendPushEvent
import com.altcraft.sdk.push.util.Utils.altcraftPush

/**
 * Public API for reporting Altcraft push events such as delivery and open.
 */
@Keep
object EventAPI {

    /**
     * Reports that an Altcraft push notification was delivered to the device.
     *
     * @param context Application context used to send the event.
     * @param message Push notification data containing the message UID.
     * @param messageUID Explicit message UID. Takes priority over the UID from [message].
     */
    suspend fun deliveryEvent(
        context: Context,
        message: Map<String, String>? = null,
        messageUID: String? = null
    ) {
        val type = EventType.DELIVERY
        if (message != null && !altcraftPush(message)) return
        sendPushEvent(
            context = context, type = type, uid = messageUID ?: message?.get(
                UID
            )
        )
    }

    /**
     * Reports that an Altcraft push notification was opened by the user.
     *
     * @param context Application context used to send the event.
     * @param message Push notification data containing the message UID.
     * @param messageUID Explicit message UID. Takes priority over the UID from [message].
     */
    suspend fun openEvent(
        context: Context,
        message: Map<String, String>? = null,
        messageUID: String? = null
    ) {
        val type = EventType.OPEN
        if (message != null && !altcraftPush(message)) return
        sendPushEvent(
            context = context, type = type, uid = messageUID ?: message?.get(
                UID
            )
        )
    }

    /**
     * Represents push event types.
     */
    @Keep
    internal enum class EventType(
        val value: String
    ) {
        DELIVERY("delivery"),
        OPEN("open")
    }
}
