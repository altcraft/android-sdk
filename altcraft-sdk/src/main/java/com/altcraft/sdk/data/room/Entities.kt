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
    @ColumnInfo(name = "resToken")
    var rToken: String?,
    @ColumnInfo(name = "appInfo")
    val appInfo: DataClasses.AppInfo?,
    @ColumnInfo(name = "usingService")
    var usingService: Boolean,
    @ColumnInfo(name = "serviceMessage")
    var serviceMessage: String?,
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
     * @return true if all required fields are non-empty and valid, otherwise false.
     */
    fun isValid() = apiUrl.isNotEmpty() && allProvidersValid(providerPriorityList)
}

/**
 * SubscribeEntity — entity for storing subscribe request data.
 */
@Entity(tableName = "subscribeTable")
internal data class SubscribeEntity(
    @ColumnInfo(name = "userTag")
    var userTag: String?,
    @ColumnInfo(name = "status")
    var status: String,
    @ColumnInfo(name = "sync")
    var sync: Int?,
    @ColumnInfo(name = "profileFields")
    var profileFields: JsonElement?,
    @ColumnInfo(name = "customFields")
    var customFields: JsonElement?,
    @ColumnInfo(name = "cats")
    var cats: List<DataClasses.CategoryData>?,
    @ColumnInfo(name = "replace")
    var replace: Boolean?,
    @ColumnInfo(name = "skipTriggers")
    var skipTriggers: Boolean?,
    @PrimaryKey
    @ColumnInfo(name = "uid")
    var uid: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "time")
    var time: Long = System.currentTimeMillis() / 1000,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    var maxRetryCount: Int = MAX_RETRY_COUNT,
)

/**
 * Entity representing a push event stored in the database.
 *
 * @property uid Unique identifier of the event (now used as the primary key).
 * @property type Type of push event (e.g., "received", "opened", "dismissed").
 * @property time Timestamp of the event in string format.
 * @property retryCount Current retry attempt count.
 * @property maxRetryCount Maximum number of retry attempts allowed.
 */
@Entity(tableName = "pushEventTable")
internal data class PushEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    var uid: String,
    @ColumnInfo(name = "type")
    var type: String,
    @ColumnInfo(name = "time")
    var time: Long = System.currentTimeMillis() / 1000,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    var maxRetryCount: Int = MAX_RETRY_COUNT,
)