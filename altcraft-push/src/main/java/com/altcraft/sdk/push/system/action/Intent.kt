package com.altcraft.sdk.push.system.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.altcraft.sdk.push.data.Constants.EXTRA
import com.altcraft.sdk.push.data.Constants.MSG_ID
import com.altcraft.sdk.push.data.Constants.UID
import com.altcraft.sdk.push.data.Constants.URL
import com.altcraft.sdk.push.util.Utils.UniqueCodeGenerator.uniqueCode
import kotlin.jvm.java
import kotlin.text.isNotEmpty

/**
 * Utility object for creating PendingIntents to handle push notification clicks.
 */
internal object Intent {
    private val target = AltcraftPushActionActivity::class.java

    private const val FLAGS =
        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP

    private const val PENDING_FLAG = PendingIntent.FLAG_IMMUTABLE

    /**
     * Returns a `PendingIntent` that launches the target activity when a push
     * notification is clicked.
     *
     * @param context Application context.
     * @param id Notification ID used to display or cancel the notification.
     * @param url URL to open after the click.
     * @param uid Unique Altcraft message identifier.
     * @param extra Optional extra payload passed via intent extra [EXTRA].
     *   Empty values are not added to extras.
     */
    fun getIntent(
        context: Context,
        id: Int,
        url: String,
        uid: String,
        extra: String
    ): PendingIntent {
        val intent = Intent(context, target)
            .addFlags(FLAGS)
            .putExtra(MSG_ID, id)
            .putExtra(UID, uid)
            .putExtra(URL, url)

        if (extra.isNotEmpty()) intent.putExtra(EXTRA, extra)

        return PendingIntent.getActivity(context, uniqueCode(uid), intent, PENDING_FLAG)
    }
}