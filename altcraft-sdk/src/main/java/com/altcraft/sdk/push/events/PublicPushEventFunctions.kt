package com.altcraft.sdk.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.additional.SubFunction.altcraftPush
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Public API for reporting Altcraft push events such as delivery and open.
 *
 * Sends events asynchronously. Non-Altcraft messages are ignored.
 */
@Keep
object PublicPushEventFunctions {

    /**
     * Reports that an Altcraft push notification was delivered to the device.
     *
     * @param context application context
     * @param message optional push data
     * @param messageUID optional Altcraft notification UID; taken in message["_uid"]
     */
    @Suppress("unused")
    fun deliveryEvent(
        context: Context,
        message: Map<String, String>? = null,
        messageUID: String? = null
    ) {
        if (message != null && !altcraftPush(message)) return

        CoroutineScope(Dispatchers.IO).launch {
            sendPushEvent(context, DELIVERY, message?.get(UID_KEY) ?: messageUID)
        }
    }

    /**
     * Reports that an Altcraft push notification was opened by the user.
     *
     * @param context application context
     * @param message optional push data
     * @param messageUID optional Altcraft notification UID; taken in message["_uid"]
     */
    @Suppress("unused")
    fun openEvent(
        context: Context,
        message: Map<String, String>? = null,
        messageUID: String? = null
    ) {
        if (message != null && !altcraftPush(message)) return

        CoroutineScope(Dispatchers.IO).launch {
            sendPushEvent(context, OPEN, message?.get(UID_KEY) ?: messageUID)
        }
    }
}