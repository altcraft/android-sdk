package com.altcraft.altcraftmobile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_BODY
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_TITLE
import com.altcraft.sdk.data.DataClasses
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppDataClasses {

    @Serializable
    data class ConfigData(
        val apiUrl: String,
        val icon: Int? = null,
        val rToken: String? = null,
        val usingService: Boolean = false,
        val serviceMessage: String? = null,
        val priorityProviders: List<String>? = null
    )

    @Serializable
    data class SubscribeSettings(
        val sync: Boolean = true,
        val replace: Boolean = false,
        val skipTriggers: Boolean = false,
        val customFields: Map<String, @Contextual Any> = emptyMap(),
        val profileFields: Map<String, @Contextual Any> = emptyMap(),
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