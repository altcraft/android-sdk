package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.altcraft.sdk.data.Constants.EXTRA
import com.altcraft.sdk.data.Constants.MSG_ID
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles actions triggered by tapping a push notification:
 * sends an "open" event, cancels the notification, and either opens a link or launches the app.
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
     * Cancels a notification by its ID, if provided.
     *
     * @param context Application context.
     * @param id Notification ID to cancel; ignored if `null`.
     */
    private fun closeNotification(context: Context, id: Int?) = id?.let {
        NotificationManagerCompat.from(context).cancel(it)
    }

    /**
     * Processes extras from a notification tap:
     * - sends the "open" event;
     * - cancels the notification;
     * - opens a URL (if present) or launches the app.
     *
     * Deduplicates actions by tracking the last handled [uid].
     *
     * @param context Application context.
     * @param extras Bundle with keys:
     *  - [UID] (String),
     *  - [URL] (String, optional),
     *  - [MSG_ID] (Int, optional),
     *  - [EXTRA] (String, optional).
     */
    fun handleExtras(context: Context, extras: Bundle?) {
        try {
            val id = extras?.getInt(MSG_ID)
            val uid = extras?.getString(UID)
            val link = extras?.getString(URL)
            val extra = extras?.getString(EXTRA)

            openEvent(context, uid)
            closeNotification(context, id)

            if (!link.isNullOrEmpty() && this.uid != uid) {
                this.uid = uid
                openLink(context, link, extra)
            } else {
                this.uid = null
                launchApp(context, extra)
            }
        } catch (e: Exception) {
            error("handleExtras", e)
        }
    }

    /**
     * Attempts to open the given URL in a browser; falls back to launching the app on failure.
     *
     * @param context Application context.
     * @param link URL to open.
     * @param extra Value to pass via intent extra [EXTRA].
     */
    private fun openLink(context: Context, link: String, extra: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).setData(link.toUri())
                .putExtra(EXTRA, extra)

            context.startActivity(intent)
        } catch (e: Exception) {
            error("openLink", e)
            launchApp(context, extra)
        }
    }

    /**
     * Launches the app’s main activity.
     *
     * @param context Application context.
     * @param extra Value to pass via intent extra [EXTRA].
     */
    internal fun launchApp(context: Context, extra: String?) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(
                context.packageName
            )?.apply {
                putExtra(EXTRA, extra)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            if (intent != null) context.startActivity(intent)
        } catch (e: Exception) {
            error("launchApp", e)
        }
    }
}