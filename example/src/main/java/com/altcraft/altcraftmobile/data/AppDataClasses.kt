package com.altcraft.altcraftmobile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.altcraftmobile.data.AppConstants.CATS
import com.altcraft.altcraftmobile.data.AppConstants.CUSTOM_FIELDS
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_BODY
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_TITLE
import com.altcraft.altcraftmobile.data.AppConstants.PROFILE_FIELDS
import com.altcraft.altcraftmobile.data.AppConstants.REPLACE
import com.altcraft.altcraftmobile.data.AppConstants.SKIP_TRIGGERS
import com.altcraft.altcraftmobile.data.AppConstants.SYNC
import com.altcraft.altcraftmobile.functions.app.Converter.Parser.parseJsonToMap
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.data.DataClasses
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class AppDataClasses {

    @Serializable
    data class ConfigData(
        val apiUrl: String,
        val icon: Int? = null,
        val rToken: String? = null,
        val usingService: Boolean = false,
        val serviceMessage: String? = null,
        val priorityProvider: List<String>? = null
    ) {
        companion object {
            fun from(config: AltcraftConfiguration?): ConfigData? {
                return if (config == null) null else ConfigData(
                    apiUrl = config.getApiUrl(),
                    icon = config.getIcon(),
                    rToken = config.getRToken(),
                    usingService = config.getUsingService(),
                    serviceMessage = config.getServiceMessage(),
                    priorityProvider = config.getProviderPriorityList()
                )
            }
        }
    }


    data class SubscribeSettings(
        val sync: Boolean = true,
        val replace: Boolean = false,
        val skipTriggers: Boolean = false,
        val customFields: Map<String, Any> = emptyMap(),
        val profileFields: Map<String, Any> = emptyMap(),
        val cats: List<DataClasses.CategoryData> = listOf()
    ) {
        companion object {
            fun getDefault() = SubscribeSettings(
                sync = true,
                replace = false,
                skipTriggers = false,
                customFields = emptyMap(),
                profileFields = emptyMap(),
                cats = listOf()
            )

            fun from(jsonString: String?): SubscribeSettings? {
                if (jsonString.isNullOrBlank()) return null
                return try {
                    val json = JSONObject(jsonString)
                    val gson = Gson()

                    SubscribeSettings(
                        sync = json.optBoolean(SYNC),
                        replace = json.optBoolean(REPLACE),
                        skipTriggers = json.optBoolean(SKIP_TRIGGERS),
                        customFields = parseJsonToMap(json.optString(CUSTOM_FIELDS)),
                        profileFields = parseJsonToMap(json.optString(PROFILE_FIELDS)),
                        cats = gson.fromJson(
                            json.optString(CATS),
                            object : TypeToken<List<DataClasses.CategoryData>>() {}.type
                        ) ?: emptyList()
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    @Serializable
    data class NotificationData(
        val title: String = EXAMPLE_TITLE,
        val body: String = EXAMPLE_BODY,
        val buttons: List<String>? = null,
        val smallImageUrl: String? = null,
        val largeImageUrl: String? = null
    ) {
        companion object {
            fun getDefault(): NotificationData = NotificationData()

            fun fromJsonString(jsonString: String?): NotificationData? {
                if (jsonString.isNullOrBlank()) return null
                return try {
                    Json.decodeFromString(jsonString)
                } catch (_: Exception) {
                    null
                }
            }
        }

        fun toJsonString(): String = Json.encodeToString(this)
    }
}