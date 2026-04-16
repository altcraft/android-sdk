package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.additional.SubFunction.altcraftPush
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.PUSH_RECEIVER
import com.altcraft.sdk.data.Constants.MESSAGE
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import com.altcraft.sdk.sdk_events.EventList.acPush
import com.altcraft.sdk.sdk_events.EventList.notAcPush
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.sdk_events.Message.RECEIVER_REDEFINED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Contains the functions necessary to receive incoming push notifications.
 */
internal object IncomingPushManager {

    /**
     * Processes an incoming push payload asynchronously.
     *
     * For Altcraft pushes, dispatches the message to all available receivers
     * and sends the DELIVERY event. Non-Altcraft pushes are only logged
     * and are not processed further.
     *
     * @param context Application context used for push handling.
     * @param message Push payload.
     */
    fun handlePush(context: Context, message: Map<String, String>) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isAltcraftPush = altcraftPush(message)

                event(
                    "takePush",
                    if (isAltcraftPush) acPush else notAcPush,
                    mapOf(MESSAGE to message)
                )

                if (!isAltcraftPush) return@launch

                sendToAllRecipients(context, message)
                sendPushEvent(context, DELIVERY, message[UID_KEY])
            } catch (e: Exception) {
                error("takePush", e)
            }
        }

    /**
     * Sends a push message to all available `AltcraftPushReceiver` classes.
     * Falls back to default receiver if none found.
     *
     * @param context App context for class loading.
     * @param message Push message payload.
     */
    internal suspend fun sendToAllRecipients(context: Context, message: Map<String, String>) {
        val func = "sendToAllRecipients"
        try {
            val packages = getConfig(context)?.pushReceiverModules.orEmpty()
            val recipients = getAllRecipient(context, packages)

            if (recipients.isEmpty()) AltcraftSDK.PushReceiver().pushHandler(context, message)
            else {
                recipients.forEach {
                    it.pushHandler(context, message).also { _ ->
                        event(func, 220 to "$RECEIVER_REDEFINED ${it::class.java.name}")
                    }
                }
            }
        } catch (e: Exception) {
            error(func, e)
        }
    }

    /**
     * Loads and instantiates all available [AltcraftSDK.PushReceiver] implementations
     * from the given list of package names.
     *
     * @param context The application context used to obtain the class loader.
     * @param packages List of package name prefixes where push receivers may be located.
     * @return A list of successfully instantiated [AltcraftSDK.PushReceiver] objects.
     */
    private fun getAllRecipient(
        context: Context,
        packages: List<String>
    ): List<AltcraftSDK.PushReceiver> {
        return packages.mapNotNull {
            try {
                val clazz = Class.forName("$it$PUSH_RECEIVER", false, context.classLoader)
                clazz.getDeclaredConstructor().newInstance() as? AltcraftSDK.PushReceiver
            } catch (_: ClassNotFoundException) {
                null
            } catch (e: Exception) {
                error("getAllRecipient", e)
                null
            }
        }
    }
}