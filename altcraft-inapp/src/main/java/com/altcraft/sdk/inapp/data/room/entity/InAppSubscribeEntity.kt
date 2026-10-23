package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.data.room.entities.RetryableEntity
import com.altcraft.sdk.inapp.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.inapp.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.inapp.data.room.dao.InAppSubscribeDao
import kotlinx.serialization.json.JsonElement

/**
 * Entity for storing in-app subscribe request data.
 */
@Entity(tableName = "in_app_subscribe_table")
internal data class InAppSubscribeEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "time")
    val time: Long = time(),
    @ColumnInfo(name = "userTag")
    val userTag: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "sync")
    val sync: Boolean?,
    @ColumnInfo(name = "profileFields")
    val profileFields: JsonElement?,
    @ColumnInfo(name = "customFields")
    val customFields: JsonElement?,
    @ColumnInfo(name = "cats")
    val cats: List<CategoryData>?,
    @ColumnInfo(name = "replace")
    val replace: Boolean?,
    @ColumnInfo(name = "skipTriggers")
    val skipTriggers: Boolean?,
    @ColumnInfo(name = "deviceId")
    val deviceId: String,
    @ColumnInfo(name = "acid")
    val acid: String,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<InAppSubscribeDao> {

    /** Returns whether the retry limit has been exceeded. */
    override suspend fun retryLimit(
        dao: InAppSubscribeDao
    ) = dao.incrementRetryAndDeleteIfExceeded(requestID)
}