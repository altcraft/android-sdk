package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.Constants.OPEN
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.push.events.PushEvent.sendPushEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

/**
 * The `PushAction` object is needed to create actions that will be performed after clicking
 * on a push notification.
 */
internal object PushAction {

    // Intent flags to reuse the activity if it exists, or start it fresh
    private const val FLAGS = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP

    @Volatile
    private var uid: String? = null

    /**
     * Creates an [Intent] to launch [AltcraftPushActionActivity].
     *
     * @param ctx The context used to construct the intent.
     * @return An explicit [Intent] pointing to [AltcraftPushActionActivity].
     */
    private fun intent(ctx: Context): Intent = Intent(ctx, AltcraftPushActionActivity::class.java)

    /**
     * Creates and returns a `PendingIntent` to start an activity in Android after clicking
     * on a push notification.
     *
     * @param context The application context used to create the intent.
     * @param url The URL that should be opened when the push notification is clicked.
     * @param uid The parameter takes the value of the url hubLink open. after clicking
     * on the notification, a request is made for this url to record information about opening the
     * notification on the server..
     * @return A `PendingIntent` configured to start the appropriate activity.
     */
    fun getIntent(
        context: Context,
        url: String,
        uid: String
    ): PendingIntent {
        val newIntent = newIntent(context, url, uid).addFlags(FLAGS)
        val flag = PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(context, Random().nextInt(1000), newIntent, flag)
    }

    /**
     * Builds an intent to open a URL via PushActionActivity,
     * or fallback to app launch if URL is empty.
     *
     * @param context The application context.
     * @param url The URL to open.
     * @param uid The UID used to report notification opening.
     * @return A properly configured intent.
     */
    private fun newIntent(context: Context, url: String, uid: String): Intent {
        val intent = intent(context).putExtras(bundleOf(URL to url, UID to uid))
        return if (url.isNotEmpty()) intent else openAppIntent(context, uid)
    }

    /**
     * Creates an intent to launch the application with package name and UID extras.
     *
     * @param context The application context.
     * @param uid The UID used to report notification opening.
     * @return An intent that starts the app via PushActionActivity.
     */
    private fun openAppIntent(context: Context, uid: String): Intent {
        return intent(context).putExtras(
            bundleOf(
                Constants.PACKAGE_NAME to context.packageName,
                UID to uid
            )
        )
    }

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
     * Handles extras received from the notification and performs the appropriate action:
     * creates an open event and navigates to a link or launches the app.
     *
     * @param context The application context.
     * @param extras A bundle containing the UID and optional URL.
     */
    fun handleExtras(context: Context, extras: Bundle?) {
        try {
            val link = extras?.getString(URL)
            val uid = extras?.getString(UID)

            openEvent(context, uid)

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