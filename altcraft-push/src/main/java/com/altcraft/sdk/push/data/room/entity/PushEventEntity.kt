package com.altcraft.sdk.push.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.data.room.entities.RetryableEntity
import com.altcraft.sdk.push.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.push.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.push.data.room.dao.PushEventDao

/**
 * Entity representing a push event stored by the Push module.
 */
@Entity(tableName = "push_event_table")
internal data class PushEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "time")
    val time: Long = time(),
    @ColumnInfo(name = "uid")
    val uid: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<PushEventDao> {

    /**
     * Returns whether the retry limit has been exceeded.
     *
     * @param dao DAO used to process the retry.
     */
    override suspend fun retryLimit(
        dao: PushEventDao
    ) = dao.incrementRetryAndDeleteIfExceeded(requestID)
}