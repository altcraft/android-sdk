package com.altcraft.sdk.push.system.receiver

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.util.Log.event
import com.altcraft.sdk.push.PushReceiver
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants.MESSAGE
import com.altcraft.sdk.push.data.Constants.PUSH_RECEIVER
import com.altcraft.sdk.push.data.Constants.RECEIVER
import com.altcraft.sdk.push.data.Constants.UID
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.event.EventAPI.deliveryEvent
import com.altcraft.sdk.push.util.Utils.altcraftPush
import com.altcraft.sdk.coordination.SDKScope
import com.altcraft.sdk.push.data.Constants.UID_KEY
import com.altcraft.sdk.push.events.ModuleEvents.duplicatePushMessage
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles incoming push notifications.
 */
internal object IncomingPushManager {

    /**
     * Processes an incoming push notification synchronously.
     *
     * @param context Application context.
     * @param message Push notification payload.
     */
    internal fun handlePush(
        context: Context,
        message: Map<String, String>
    ) = SDKScope.io.launch {
        try {
            if (!checkAcPush(message = message)) return@launch
            if (!isMessageUnique(context, message)) return@launch
            sendToAllRecipients(context = context, message = message)
            deliveryEvent(context = context, messageUID = message[UID])
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("handlePush", e)
        }
    }

    /**
     * Checks whether the message belongs to Altcraft
     * and sends the corresponding SDK event.
     *
     * @param message Push notification payload.
     * @return `true` if the message belongs to Altcraft; otherwise `false`.
     */
    private fun checkAcPush(message: Map<String, String>): Boolean {
        return altcraftPush(message).also {
            event(
                function = "handlePush", eventValue = mapOf(MESSAGE to message),
                event = if (it) ModuleEvents.acPush else ModuleEvents.notAcPush
            )
        }
    }

    /**
     * Checks whether the push message UID is unique.
     *
     * @return `true` if the UID is new, otherwise `false`.
     */
    private suspend fun isMessageUnique(
        context: Context,
        message: Map<String, String>
    ): Boolean {
        val uid = message[UID] ?: return false

        return Environment.create(context)
            .room.dao.message
            .isMessageUnique(uid)
            .also {
                if (!it) event(
                    "isMessageUnique",
                    duplicatePushMessage,
                    mapOf(UID_KEY to uid)
                )
            }
    }

    /**
     * Sends an Altcraft push to all configured receivers
     * or uses the default receiver if none are found.
     *
     * @param context Application context.
     * @param message Push notification payload.
     */
    internal suspend fun sendToAllRecipients(
        context: Context,
        message: Map<String, String>
    ) {
        val func = "sendToAllRecipients"

        try {
            val packages = Environment
                .create(context)
                .pushConfig()
                .receiverPackages

            val recipients = getAllRecipient(
                context = context,
                packages = packages
            )

            if (recipients.isNotEmpty()) {
                recipients.forEach { receiver ->
                    receiver.pushHandler(
                        context = context,
                        message = message
                    )

                    event(
                        function = func,
                        event = ModuleEvents.receiverRedefined,
                        eventValue = mapOf(
                            RECEIVER to receiver::class.java.name
                        )
                    )
                }
            } else PushReceiver().pushHandler(context = context, message = message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error(func, e)
        }
    }

    /**
     * Finds and creates configured [PushReceiver] implementations.
     *
     * @param context Application context.
     * @param packages Packages containing receiver implementations.
     * @return Available push receivers.
     */
    private fun getAllRecipient(
        context: Context,
        packages: List<String>
    ): List<PushReceiver> {
        return packages.mapNotNull {
            try {
                val clazz = Class.forName("$it$PUSH_RECEIVER", false, context.classLoader)
                clazz.getDeclaredConstructor().newInstance() as? PushReceiver
            } catch (_: ClassNotFoundException) {
                null
            } catch (e: Exception) {
                error("getAllRecipient", e)
                null
            }
        }
    }
}