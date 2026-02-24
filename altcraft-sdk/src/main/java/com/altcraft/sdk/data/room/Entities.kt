package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.push.token.TokenManager.allProvidersValid
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * ConfigurationEntity - entity for storing configuration data.
 */
@Entity(tableName = "configurationTable")
internal data class ConfigurationEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "icon")
    var icon: Int? = null,
    @ColumnInfo(name = "apiUrl")
    var apiUrl: String,
    @ColumnInfo(name = "rToken")
    var rToken: String?,
    @ColumnInfo(name = "appInfo")
    val appInfo: DataClasses.AppInfo?,
    @ColumnInfo(name = "pushReceiverModules")
    var pushReceiverModules: List<String>? = null,
    @ColumnInfo(name = "providerPriorityList")
    var providerPriorityList: List<String>? = null,
    @ColumnInfo(name = "pushChannelName")
    var pushChannelName: String? = null,
    @ColumnInfo(name = "pushChannelDescription")
    var pushChannelDescription: String? = null
) {
    /**
     * Validates the ConfigurationEntity object.
     *
     * @return `true` if `apiUrl` is not empty and providers are valid.
     */
    fun isValid() = apiUrl.isNotEmpty() && allProvidersValid(providerPriorityList)
}

/** Sealed marker interface for Room entities representing persisted request payloads. */
sealed interface RequestEntity

/**
 * Generates a random UUID string.
 */
private fun uuid() = UUID.randomUUID().toString()

/**
 * SubscribeEntity — entity for storing subscribe request data.
 */
@Entity(tableName = "subscribeTable")
internal data class SubscribeEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "userTag")
    val userTag: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "sync")
    val sync: Int?,
    @ColumnInfo(name = "profileFields")
    val profileFields: JsonElement?,
    @ColumnInfo(name = "customFields")
    val customFields: JsonElement?,
    @ColumnInfo(name = "cats")
    val cats: List<DataClasses.CategoryData>?,
    @ColumnInfo(name = "replace")
    val replace: Boolean?,
    @ColumnInfo(name = "skipTriggers")
    val skipTriggers: Boolean?,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RequestEntity

/**
 * Entity representing a push event stored in the database.
 */
@Entity(tableName = "pushEventTable")
internal data class PushEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "uid")
    val uid: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RequestEntity

/**
 * Entity representing a mobile event stored in the database.
 */
@Entity(tableName = "mobileEventTable")
internal data class MobileEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "userTag")
    val userTag: String,
    @ColumnInfo(name = "tz")
    val timeZone: Int,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sid")
    val sid: String,
    @ColumnInfo(name = "altcraftClientID")
    val altcraftClientID: String?,
    @ColumnInfo(name = "eventName")
    val eventName: String,
    @ColumnInfo(name = "payload")
    val payload: String?,
    @ColumnInfo(name = "matching")
    val matching: String?,
    @ColumnInfo(name = "matchingType")
    val matchingType: String?,
    @ColumnInfo(name = "profileFields")
    val profileFields: String?,
    @ColumnInfo(name = "subscription")
    val subscription: String?,
    @ColumnInfo(name = "sendMessageId")
    val sendMessageId: String?,
    @ColumnInfo(name = "utm")
    val utmTags: String?,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RequestEntity

/**
 * Entity representing a profile update stored in the database.
 */
@Entity(tableName = "profileUpdateTable")
internal data class ProfileUpdateEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "userTag")
    val userTag: String?,
    @ColumnInfo(name = "time")
    val time: Long,
    @ColumnInfo(name = "profileFields")
    val profileFields: JsonElement?,
    @ColumnInfo(name = "skipTriggers")
    val skipTriggers: Boolean?,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RequestEntity
