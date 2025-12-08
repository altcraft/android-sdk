package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.os.Build
import androidx.work.Data
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.data.Constants.DELIVERY
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.UID_KEY
import com.altcraft.sdk.extension.MapExtension.toWorkDataOrNull
import com.altcraft.sdk.push.PushPresenter.showPush
import com.altcraft.sdk.workers.foreground.Request.startEventForegroundWorker
import com.altcraft.sdk.workers.foreground.Request.startPushForegroundWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Selects the appropriate way to handle push messages and delivery events based on API level,
 * app state, and push provider. Uses foreground workers when needed to establish a network
 * connection while the app is in the background.
 */
internal object OpenPushStrategy {

    private val isForegroundRestrictionsApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Prepares WorkManager [Data] based on push message and runtime conditions.
     *
     * Returns `null` if the app is currently in the foreground, or if the push originates from FCM
     * and the device runs Android 12 (API 31) or higher. In such cases, using WorkManager may lead
     * to a `ForegroundServiceStartNotAllowedException`, particularly when handling FCM notifications
     * with default (medium) priority.
     *
     * @param message The raw push message as a key-value map.
     * @return A [Data] object for WorkManager, or `null` if background execution is not applicable.
     */
    private fun getWorkData(message: Map<String, String>): Data? {
        return try {
            val fcmPush = (message["_provider"] ?: FCM_PROVIDER) == FCM_PROVIDER

            if (isAppInForegrounded() || fcmPush && isForegroundRestrictionsApi) null
            else message.toWorkDataOrNull()
        } catch (e: Exception) {
            error("isOpenFromWorker", e)
            null
        }
    }

    /**
     * Determines how to handle a push message — immediately or via a foreground worker.
     *
     * Uses a foreground worker when background execution is required. This allows establishing
     * a temporary network connection to fetch push notification content while the app is in the
     * background.
     * If not needed, the push is shown directly on the main coroutine scope.
     *
     * @param context The application context.
     * @param message The push data map.
     */
    fun openPushStrategy(context: Context, message: Map<String, String>) {
        try {
            val data = getWorkData(message)

            if (data != null) startPushForegroundWorker(context, data) else scope.launch {
                showPush(context, message)
            }
        } catch (e: Exception) {
            error("openPushStrategy", e)
        }
    }

    /**
     * Determines how to send a delivery event — via a foreground worker or directly.
     *
     * A foreground worker is used to ensure a background-safe network connection for reporting
     * delivery events when the app is not in the foreground. Otherwise, the event is sent
     * immediately using a coroutine.
     *
     * @param context The application context.
     * @param message The push data map containing the UID.
     */
    fun deliveryEventStrategy(context: Context, message: Map<String, String>) {
        try {
            val data = getWorkData(message)

            if (data != null) startEventForegroundWorker(context, data) else scope.launch {
                sendPushEvent(context, DELIVERY, message[UID_KEY])
            }
        } catch (e: Exception) {
            error("openPushStrategy", e)
        }
    }
}