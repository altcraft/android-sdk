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
import com.altcraft.sdk.inapp.data.room.dao.InAppEventDao

/**
 * Entity representing an in-app event stored in the database.
 */
@Entity(tableName = "in_app_event_table")
internal data class InAppEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "time")
    val time: Long = time(),
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "smid")
    val smid: String,
    @ColumnInfo(name = "acid")
    val acid: String,
    @ColumnInfo(name = "data")
    val data: String?,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<InAppEventDao> {

    /** Returns whether the retry limit has been exceeded. */
    override suspend fun retryLimit(
        dao: InAppEventDao
    ) = dao.incrementRetryAndDeleteIfExceeded(requestID)
}