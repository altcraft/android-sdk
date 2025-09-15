package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.altcraftmobile.data.AppPreferenses.getNotificationData
import com.altcraft.altcraftmobile.data.AppDataClasses

object Notification {
    private fun convertToJsonString(actions: List<String>): String {
        return actions
            .joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { label ->
                """{"label":"$label","link":"","action":"open app"}"""
            }
    }

    fun getExamplePushData(context: Context): Map<String, String> {
        val notificationData =
            getNotificationData(context) ?: AppDataClasses.NotificationData.getDefault()

        val title = notificationData.title
        val body = notificationData.body
        val icon = notificationData.smallImageUrl
        val image = notificationData.largeImageUrl
        val buttons = convertToJsonString(notificationData.buttons ?: emptyList())

        val pushMap = mutableMapOf<String, String>().apply {
            put("_title", title)
            put("_body", body)
            put("_buttons", buttons)

            icon?.let { put("_icon", it) }
            image?.let { put("_image", it) }
        }
        pushMap["_ac_push"] = "Altcraft"
        return pushMap
    }
}