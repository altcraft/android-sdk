package com.altcraft.altcraftmobile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.data.json.Converter.fromStringJson
import com.altcraft.altcraftmobile.data.json.Converter.toStringJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppPreferenses {

    private const val TAG_APP = "APP"

    private const val STATUS_KEY = "${TAG_APP}_STATUS"
    private const val IN_APP_STATUS_KEY = "${TAG_APP}_IN_APP_STATUS"
    private const val CONFIG_KEY = "${TAG_APP}_CONFIG"
    private const val REG_JWT_KEY = "${TAG_APP}_REG_JWT"
    private const val ANON_JWT_KEY = "${TAG_APP}_ANON_JWT"

    private const val INTERNAL_AUTH_STATUS_KEY = "${TAG_APP}_INTERNAL_AUTH_STATUS"
    private const val EXTERNAL_AUTH_STATUS_KEY = "${TAG_APP}_EXTERNAL_AUTH_STATUS"

    private const val SUB_SETTINGS_KEY = "${TAG_APP}_SUBSCRIBE_SETTING"
    private const val NOTIFICATION_DATA_KEY = "${TAG_APP}_NOTIFICATION_DATA"
    private const val TOKEN_UPDATE_TIME_KEY = "${TAG_APP}_TOKEN_UPDATE_DATA"

    private fun getPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(TAG_APP, Context.MODE_PRIVATE)

    fun setTokenUpdateTime(context: Context, date: Date) {
        getPreferences(context).edit(commit = true) {
            putString(
                TOKEN_UPDATE_TIME_KEY,
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
            )
        }
    }

    fun getTokenUpdateTime(context: Context): String? =
        getPreferences(context).getString(TOKEN_UPDATE_TIME_KEY, null)

    fun setInternalAuthStatus(context: Context, status: Boolean) {
        getPreferences(context).edit {
            putBoolean(INTERNAL_AUTH_STATUS_KEY, status)
        }
    }

    fun getInternalAuthStatus(context: Context): Boolean =
        getPreferences(context).getBoolean(INTERNAL_AUTH_STATUS_KEY, false)

    fun setExternalAuthStatus(context: Context, status: Boolean) {
        getPreferences(context).edit {
            putBoolean(EXTERNAL_AUTH_STATUS_KEY, status)
        }
    }

    fun getExternalAuthStatus(context: Context): Boolean =
        getPreferences(context).getBoolean(EXTERNAL_AUTH_STATUS_KEY, false)

    fun setSubscriptionStatus(context: Context, status: String) {
        getPreferences(context).edit {
            putString(STATUS_KEY, status)
        }
    }

    fun getSubscriptionStatus(context: Context): String? =
        getPreferences(context).getString(STATUS_KEY, null)

    fun clearSubscriptionStatus(context: Context) {
        getPreferences(context).edit {
            remove(STATUS_KEY)
        }
    }

    fun setInAppSubscriptionStatus(context: Context, status: String) {
        getPreferences(context).edit {
            putString(IN_APP_STATUS_KEY, status)
        }
    }

    fun getInAppSubscriptionStatus(context: Context): String =
        getPreferences(context).getString(IN_APP_STATUS_KEY, UNSUBSCRIBED) ?: UNSUBSCRIBED

    fun clearSdkIndicatorStatuses(context: Context) {
        getPreferences(context).edit {
            putString(STATUS_KEY, UNSUBSCRIBED)
            putString(IN_APP_STATUS_KEY, UNSUBSCRIBED)
            putBoolean(INTERNAL_AUTH_STATUS_KEY, false)
            putBoolean(EXTERNAL_AUTH_STATUS_KEY, false)
        }
    }

    fun setAnonJWT(context: Context, token: String?) {
        getPreferences(context).edit {
            putString(ANON_JWT_KEY, token)
        }
    }

    fun setRegJWT(context: Context, token: String?) {
        getPreferences(context).edit {
            putString(REG_JWT_KEY, token)
        }
    }

    fun getAnonJWT(context: Context): String? =
        getPreferences(context).getString(ANON_JWT_KEY, null)

    fun getRegJWT(context: Context): String? =
        getPreferences(context).getString(REG_JWT_KEY, null)

    @SuppressLint("ApplySharedPref")
    fun removeJWT(context: Context) {
        getPreferences(context).edit(commit = true) {
            remove(ANON_JWT_KEY).remove(REG_JWT_KEY)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setConfig(context: Context, config: AppDataClasses.ConfigData) {
        getPreferences(context).edit(commit = true) {
            putString(CONFIG_KEY, config.toStringJson("setConfig"))
        }
    }

    fun getConfig(context: Context): AppDataClasses.ConfigData? {
        val jsonString = getPreferences(context).getString(CONFIG_KEY, null)

        return if (jsonString == null) null else try {
            jsonString.fromStringJson<AppDataClasses.ConfigData>("getConfig")
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("ApplySharedPref")
    fun removeConfig(context: Context) {
        getPreferences(context).edit(commit = true) {
            remove(CONFIG_KEY)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setSubscribeSettings(
        context: Context,
        settings: AppDataClasses.SubscribeSettings
    ) {
        getPreferences(context).edit(commit = true) {
            putString(SUB_SETTINGS_KEY, settings.toStringJson("setSubscribeSettings"))
        }
    }

    fun getSubscribeSettings(context: Context): AppDataClasses.SubscribeSettings? {
        return try {
            val setting = getPreferences(context).getString(SUB_SETTINGS_KEY, null)
            setting.fromStringJson<AppDataClasses.SubscribeSettings>("getSubscribeSettings")
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("ApplySharedPref")
    fun removeSubscribeSettings(context: Context) {
        getPreferences(context).edit(commit = true) {
            remove(SUB_SETTINGS_KEY)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setNotificationData(context: Context, data: AppDataClasses.NotificationData) {
        getPreferences(context).edit(commit = true) {
            putString(NOTIFICATION_DATA_KEY, data.toJsonString())
        }
    }

    fun getNotificationData(context: Context): AppDataClasses.NotificationData? {
        val jsonString = getPreferences(context).getString(NOTIFICATION_DATA_KEY, null)
        return AppDataClasses.NotificationData.fromJsonString(jsonString)
    }

    @SuppressLint("ApplySharedPref")
    fun removeNotificationData(context: Context) {
        getPreferences(context).edit(commit = true) {
            remove(NOTIFICATION_DATA_KEY)
        }
    }
}