package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import com.altcraft.sdk.additional.SubFunction.UniqueCodeGenerator.uniqueCode
import com.altcraft.sdk.data.Constants.MESSAGE_ID
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.URL

/**
 * Utility object for creating PendingIntents to handle push notification clicks.
 */
object Intent {

    //target Activity
    private val target = AltcraftPushActionActivity::class.java

    // Intent flags to reuse the activity if it exists, or start it fresh
    private const val FLAGS =
        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP

    //flag for pending intent
    private const val PENDING_FLAG = PendingIntent.FLAG_IMMUTABLE

    /**
     * Returns a `PendingIntent` that launches the target activity when a push
     * notification is clicked.
     *
     * @param context Application context.
     * @param id Notification ID used by `NotificationManager` to display/cancel the notification.
     * @param url URL to open after the click.
     * @param uid Unique Altcraft message identifier.
     */
    fun getIntent(context: Context, id: Int, url: String, uid: String): PendingIntent {
        val bundles = bundleOf(MESSAGE_ID to id, UID to uid, URL to url)

        val intent = Intent(context, target).addFlags(FLAGS).putExtras(bundles)

        return PendingIntent.getActivity(context, uniqueCode(uid), intent, PENDING_FLAG)
    }
}