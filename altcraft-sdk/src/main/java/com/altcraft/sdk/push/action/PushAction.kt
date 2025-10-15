package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.altcraft.sdk.data.Constants.MESSAGE_ID
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.String

/**
 * The `PushAction` object is needed to create actions that will be performed after clicking
 * on a push notification.
 */
internal object PushAction {

    @Volatile
    private var uid: String? = null

    /**
     * Registers a unique push opening event ("open" event)
     * @param context Application context used to send the event.
     * @param uid Unique Altcraft notification identifier.
     */
    private fun openEvent(context: Context, uid: String?) {
        if (uid != this.uid) {
            CoroutineScope(Dispatchers.IO).launch {
                sendPushEvent(context, OPEN, uid)
            }
        }
    }

    /**
     * Closes (cancels) a notification by its ID if provided.
     *
     * @param context Application context.
     * @param id Notification ID to cancel; ignored if null.
     */
    private fun closeNotification(context: Context, id: Int?) = id?.let {
        NotificationManagerCompat.from(context).cancel(it)
    }

    /**
     * Handles extras received from the notification and performs the appropriate action:
     * creates an open event and navigates to a link or launches the app.
     *
     * @param context The application context.
     * @param extras A bundle containing the UID and optional URL.
     */
    fun handleExtras(context: Context, extras: Bundle?) {
        try {
            val uid = extras?.getString(UID)
            val link = extras?.getString(URL)
            val id = extras?.getInt(MESSAGE_ID)

            openEvent(context, uid)
            closeNotification(context, id)

            if (!link.isNullOrEmpty() && this.uid != uid) {
                this.uid = uid; openLink(context, link)
            } else {
                this.uid = null; launchApp(context)
            }
        } catch (e: Exception) {
            error("handleExtras", e)
        }
    }

    /**
     * Attempts to open the provided link in a browser.
     * Falls back to launching the app if the link fails to open.
     *
     * @param context The application context.
     * @param link The URL to open.
     */
    private fun openLink(context: Context, link: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).setData(link.toUri()))
        } catch (e: Exception) {
            error("openLink", e)
            launchApp(context)
        }
    }

    /**
     * Launches the app using its package name.
     * Used as a fallback when no URL is provided or opening fails.
     *
     * @param context The application context.
     */
    internal fun launchApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            try {
                context.startActivity(it)
            } catch (e: Exception) {
                error("launchApp", e)
            }
        }
    }
}