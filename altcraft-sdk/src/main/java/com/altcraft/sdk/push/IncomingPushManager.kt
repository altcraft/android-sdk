package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.additional.SubFunction.altcraftPush
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.Constants.PUSH_RECEIVER
import com.altcraft.sdk.data.Constants.MESSAGE
import com.altcraft.sdk.sdk_events.EventList.altcraftPush
import com.altcraft.sdk.sdk_events.EventList.notAltcraftPush
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.sdk_events.Message.RECEIVER_REDEFINED
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startPushProcessingCoroutineWorker

/**
 * Contains the functions necessary to receive incoming push notifications.
 */
internal object IncomingPushManager {

    /**
     * Handles an incoming push payload: logs reception and, for Altcraft pushes,
     * schedules background processing via WorkManager.
     *
     * Flow:
     * 1) Detects whether the message is an Altcraft push (`altcraftPush(message)`).
     * 2) Generates a notification receipt event.
     * 3) If the message is not from Altcraft, returns without further processing.
     * 4) For Altcraft pushes, enqueues PushProcessingCoroutineWorker via
     *    `startPushProcessingCoroutineWorker(context, message)`.
     *
     * @param context Used to access configuration and schedule work.
     * @param message Push data map.
     */
    fun handlePush(context: Context, message: Map<String, String>) {
        try {
            val isAltcraftPush = altcraftPush(message)

            event(
                "takePush",
                if (isAltcraftPush) altcraftPush else notAltcraftPush,
                mapOf(MESSAGE to message)
            )

            if (!isAltcraftPush) return

            startPushProcessingCoroutineWorker(context, message)
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