package com.altcraft.sdk.push.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.room.entities.RetryableEntity
import com.altcraft.sdk.push.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.push.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.push.data.room.dao.PushSubscribeDao
import kotlinx.serialization.json.JsonElement

/**
 * Entity for storing push subscription request data.
 */
@Entity(tableName = "push_subscribe_table")
internal data class PushSubscribeEntity(
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
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<PushSubscribeDao> {

    /** Returns whether the retry limit has been exceeded. */
    override suspend fun retryLimit(
        dao: PushSubscribeDao
    ) = dao.incrementRetryAndDeleteIfExceeded(requestID)
}