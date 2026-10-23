package com.altcraft.sdk.target.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.data.room.entities.RetryableEntity
import com.altcraft.sdk.target.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.target.data.Constants.START_RETRY_COUNT

/**
 * Entity representing a target request stored by the Target module.
 *
 * Complex request values are serialized to JSON strings before the entity is
 * created. Therefore, no Room type converters are required.
 *
 * @property requestID unique request identifier.
 * @property time request creation time in milliseconds.
 * @property userTag user tag associated with the request.
 * @property timeZone client time-zone offset.
 * @property sid pixel identifier.
 * @property altcraftClientID optional Altcraft client identifier.
 * @property eventName target event name.
 * @property payload serialized event payload.
 * @property matching serialized matching data.
 * @property matchingType optional matching mode.
 * @property profileFields serialized profile fields.
 * @property subscription serialized subscription data.
 * @property sendMessageId optional message identifier.
 * @property utmTags serialized UTM attribution data.
 * @property retryCount current retry count.
 * @property maxRetryCount maximum permitted retry count.
 */
@Entity(tableName = "target_table")
internal data class Entity(
    @PrimaryKey
    @ColumnInfo(name = "requestID")
    val requestID: String = uuid(),
    @ColumnInfo(name = "time")
    val time: Long = time(),
    @ColumnInfo(name = "userTag")
    val userTag: String,
    @ColumnInfo(name = "tz")
    val timeZone: Int,
    @ColumnInfo(name = "sid")
    val sid: String,
    @ColumnInfo(name = "acid")
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
) : RetryableEntity<DAO> {

    /**
     * Returns whether the retry limit has been exceeded.
     *
     * @param dao DAO used to process the retry.
     */
    override suspend fun retryLimit(
        dao: DAO
    ) = dao.incrementRetryAndDeleteIfExceeded(requestID)
}