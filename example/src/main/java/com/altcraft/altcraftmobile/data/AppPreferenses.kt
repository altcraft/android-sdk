package com.altcraft.altcraftmobile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.altcraft.sdk.config.AltcraftConfiguration
import org.json.JSONObject
import androidx.core.content.edit
import com.altcraft.altcraftmobile.data.AppConstants.API_URL
import com.altcraft.altcraftmobile.data.AppConstants.APP_ID
import com.altcraft.altcraftmobile.data.AppConstants.APP_IID
import com.altcraft.altcraftmobile.data.AppConstants.APP_VER
import com.altcraft.altcraftmobile.data.AppConstants.CATS
import com.altcraft.altcraftmobile.data.AppConstants.CUSTOM_FIELDS
import com.altcraft.altcraftmobile.data.AppConstants.ICON
import com.altcraft.altcraftmobile.data.AppConstants.PROFILE_FIELDS
import com.altcraft.altcraftmobile.data.AppConstants.PROVIDER_PRIORITY_LIST
import com.altcraft.altcraftmobile.data.AppConstants.PUSH_CHANNEL_DESCRIPTION
import com.altcraft.altcraftmobile.data.AppConstants.PUSH_CHANNEL_NAME
import com.altcraft.altcraftmobile.data.AppConstants.PUSH_RECEIVER_MODULES
import com.altcraft.altcraftmobile.data.AppConstants.REPLACE
import com.altcraft.altcraftmobile.data.AppConstants.RTOKEN
import com.altcraft.altcraftmobile.data.AppConstants.SERVICE_MESSAGE
import com.altcraft.altcraftmobile.data.AppConstants.SKIP_TRIGGERS
import com.altcraft.altcraftmobile.data.AppConstants.SYNC
import com.altcraft.altcraftmobile.data.AppConstants.USING_SERVICE
import com.altcraft.sdk.data.DataClasses
import com.google.gson.Gson
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppPreferenses {

    private const val TAG_APP = "APP"

    //pref keys
    private const val STATUS_KEY = "${TAG_APP}_STATUS"
    private const val CONFIG_KEY = "${TAG_APP}_CONFIG"
    private const val REG_JWT_KEY = "${TAG_APP}_REG_JWT"
    private const val ANON_JWT_KEY = "${TAG_APP}_ANON_JWT"
    private const val AUTH_STATUS_KEY = "${TAG_APP}_AUTH_STATUS"
    private const val SUB_SETTINGS_KEY = "${TAG_APP}_SUBSCRIBE_SETTING"
    private const val NOTIFICATION_DATA_KEY = "${TAG_APP}_NOTIFICATION_DATA"
    private const val TOKEN_UPDATE_TIME_KEY: String = "${TAG_APP}_TOKEN_UPDATE_DATA"

    private fun getPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(TAG_APP, Context.MODE_PRIVATE)

    /**
     * Token Update time set / get
     */

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

    /**
     * Auth status set / get
     */

    fun setAuthStatus(context: Context, status: Boolean) {
        getPreferences(context).edit {
            putBoolean(AUTH_STATUS_KEY, status)
        }
    }

    fun getAuthStatus(context: Context): Boolean =
        getPreferences(context).getBoolean(AUTH_STATUS_KEY, false)

    /**
     * subscription status set / get / clear
     */

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

    /**
     * JWT set / get / remove
     */

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

    /**
     * Config  set / get / remove
     */

    @SuppressLint("ApplySharedPref")
    fun setConfig(context: Context, config: AltcraftConfiguration) {
        val json = JSONObject().apply {
            put(API_URL, config.getApiUrl())
            put(ICON, config.getIcon())
            put(RTOKEN, config.getRToken())
            put(USING_SERVICE, config.getUsingService())
            put(SERVICE_MESSAGE, config.getServiceMessage())

            put(
                PROVIDER_PRIORITY_LIST, JSONArray(
                    config.getProviderPriorityList() ?: emptyList<String>()
                )
            )
            put(
                PUSH_RECEIVER_MODULES, JSONArray(
                    config.getPushReceiverModules() ?: emptyList<String>()
                )
            )
        }

        getPreferences(context).edit(commit = true) {
            putString(CONFIG_KEY, json.toString())
        }
    }

    fun getConfig(context: Context): AltcraftConfiguration? {
        val jsonString = getPreferences(context).getString(CONFIG_KEY, null)
        if (jsonString == null) return null else return try {

            val json = JSONObject(jsonString)

            val apiUrl = json.optString(API_URL)
            val rToken = json.optString(RTOKEN).ifEmpty { null }
            val icon = if (json.has(ICON)) json.getInt(ICON) else null
            val usingService = json.optBoolean(USING_SERVICE, false)
            val serviceMessage = json.optString(SERVICE_MESSAGE).ifEmpty { null }

            val providerPriorityList: List<String>? = runCatching {
                json.optJSONArray(PROVIDER_PRIORITY_LIST)?.let { array ->
                    List(array.length()) { i -> array.getString(i) }
                }
            }.getOrNull()

            if (apiUrl.isBlank() || icon == null) return null

            AltcraftConfiguration.Builder(
                apiUrl = apiUrl,
                icon = icon,
                rToken = rToken,
                usingService = usingService,
                serviceMessage = serviceMessage,
                providerPriorityList = providerPriorityList,
                pushChannelName = PUSH_CHANNEL_NAME,
                pushChannelDescription = PUSH_CHANNEL_DESCRIPTION,
                appInfo = DataClasses.AppInfo(
                    appID = APP_ID, appIID = APP_IID, appVer = APP_VER
                )
            ).build()
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

    /**
     * Subscribe setting set / get / remove
     */

    @SuppressLint("ApplySharedPref")
    fun setSubscribeSettings(
        context: Context,
        settings: AppDataClasses.SubscribeSettings
    ) {
        val gson = Gson()

        val json = JSONObject().apply {
            put(SYNC, settings.sync)
            put(REPLACE, settings.replace)
            put(SKIP_TRIGGERS, settings.skipTriggers)
            put(CUSTOM_FIELDS, JSONObject(settings.customFields))
            put(PROFILE_FIELDS, JSONObject(settings.profileFields))
            put(CATS, JSONArray(gson.toJson(settings.cats)))
        }

        getPreferences(context).edit(commit = true) {
            putString(SUB_SETTINGS_KEY, json.toString())
        }
    }

    fun getSubscribeSettings(context: Context): AppDataClasses.SubscribeSettings? {
        return try {
            val setting = getPreferences(context).getString(SUB_SETTINGS_KEY, null)

            AppDataClasses.SubscribeSettings.from(setting ?: return null)
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

    /**
     * Notification data set / get / remove
     */
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
