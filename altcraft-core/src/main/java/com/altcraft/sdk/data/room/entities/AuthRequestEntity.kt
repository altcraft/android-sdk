package com.altcraft.sdk.data.room.entities

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.data.Constants.START_RETRY_COUNT
import com.altcraft.sdk.data.room.dao.AuthRequestDao

/**
 * Entity for storing authorization request data.
 */
@Entity(tableName = "auth_request_table")
internal data class AuthRequestEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "requestId")
    val requestId: String,
    @ColumnInfo(name = "user")
    val user: String,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<AuthRequestDao> {

    /** Returns whether the retry limit has been exceeded. */
    override suspend fun retryLimit(
        dao: AuthRequestDao
    ) = dao.incrementRetryAndDeleteIfExceeded(id)
}