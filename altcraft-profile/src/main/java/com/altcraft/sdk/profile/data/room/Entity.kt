package com.altcraft.sdk.profile.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.data.room.entities.RetryableEntity
import com.altcraft.sdk.profile.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.profile.data.Constants.START_RETRY_COUNT
import kotlinx.serialization.json.JsonElement

/**
 * Entity representing a profile update stored by the Profile module.
 *
 * Complex profile field values are converted to [JsonElement] before the entity
 * is created; Room persists them via [Converter].
 *
 * @property requestID unique request identifier.
 * @property time request creation time in milliseconds.
 * @property userTag user tag associated with the request.
 * @property profileFields serialized profile fields.
 * @property skipTriggers flag to skip triggers on the server side.
 * @property retryCount current retry count.
 * @property maxRetryCount maximum permitted retry count.
 */
@Entity(tableName = "profile_update_table")
internal data class Entity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "time")
    val time: Long = time(),
    @ColumnInfo(name = "userTag")
    val userTag: String?,
    @ColumnInfo(name = "profileFields")
    val profileFields: JsonElement?,
    @ColumnInfo(name = "skipTriggers")
    val skipTriggers: Boolean?,
    @ColumnInfo(name = "retryCount")
    var retryCount: Int = START_RETRY_COUNT,
    @ColumnInfo(name = "maxRetryCount")
    val maxRetryCount: Int = MAX_RETRY_COUNT,
) : RetryableEntity<DAO> {

    /**
     * Increments retry count and deletes this entity when its retry limit is exceeded.
     *
     * @param dao Profile module DAO.
     * @return `true` when the entity was deleted.
     */
    override suspend fun retryLimit(dao: DAO): Boolean =
        dao.incrementRetryAndDeleteIfExceeded(requestID)
}
