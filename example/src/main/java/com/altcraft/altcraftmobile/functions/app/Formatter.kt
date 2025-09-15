package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.altcraftmobile.data.AppConstants.EMPTY_VALUE_UI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatter {

    internal fun getUserNameForInfo(input: String?): String {
        return try {
            input?.substring(12)
                ?.substringBefore(".")
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?: EMPTY_VALUE_UI
        } catch (_: Exception) {
            EMPTY_VALUE_UI
        }
    }

    internal fun getLastTenCharacters(input: String?): String {
        return if (input == null) {
            EMPTY_VALUE_UI
        } else if (input.length >= 10) {
            input.takeLast(10)
        } else {
            input
        }
    }

    internal fun formatDateToTimestampString(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(date)
    }

     internal fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}