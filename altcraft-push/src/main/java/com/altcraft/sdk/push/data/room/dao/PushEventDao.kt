package com.altcraft.sdk.push.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.push.data.room.entity.PushEventEntity
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.util.Log

/**
 * DAO for stored push event requests.
 */
@Dao
internal interface PushEventDao {

    /**
     * Inserts the specified push event.
     *
     * @param entity push event entity to save.
     */
    @Insert
    suspend fun insert(entity: PushEventEntity)

    /**
     * Returns all stored push events.
     *
     * @return list of stored push event entities.
     */
    @Query(
        """
        SELECT *
        FROM push_event_table
        ORDER BY time DESC
        """
    )
    suspend fun getAll(): List<PushEventEntity>

    /**
     * Deletes the specified push event.
     *
     * @param entity push event entity to delete.
     */
    @Delete
    suspend fun deleteItem(entity: PushEventEntity)

    /**
     * Returns whether there are no stored push events.
     *
     * @return `true` if the table is empty.
     */
    @Query(
        """
        SELECT NOT EXISTS(
            SELECT 1
            FROM push_event_table
        )
        """
    )
    suspend fun isEmpty(): Boolean

    /**
     * Increments retry count for the specified request.
     *
     * @param requestID request identifier.
     */
    @Query(
        """
        UPDATE push_event_table
        SET retryCount = retryCount + 1
        WHERE requestID = :requestID
        """
    )
    suspend fun increaseRetryCount(requestID: String)

    /**
     * Deletes the request if retry limit is exceeded.
     *
     * @param requestID request identifier.
     * @return number of deleted rows.
     */
    @Query(
        """
        DELETE FROM push_event_table
        WHERE requestID = :requestID
          AND retryCount > maxRetryCount
        """
    )
    suspend fun deleteIfRetryLimitExceeded(requestID: String): Int

    /**
     * Increments retry count and deletes the request if the retry limit
     * is exceeded.
     *
     * Emits the retry-limit event when the request is deleted.
     *
     * @param requestID request identifier.
     * @return `true` if the request was deleted.
     */
    @Transaction
    suspend fun incrementRetryAndDeleteIfExceeded(requestID: String): Boolean {
        increaseRetryCount(requestID)

        if (deleteIfRetryLimitExceeded(requestID) == 0) return false
        Log.event(
            "incrementRetryAndDeleteIfExceeded",
            ModuleEvents.eventRetryLimit
        )
        return true
    }

    /**
     * Deletes the oldest requests when the table exceeds its size limit.
     *
     * @return number of deleted rows.
     */
    @Query(
        """
        DELETE FROM push_event_table
        WHERE requestID IN (
            SELECT requestID
            FROM push_event_table
            ORDER BY time ASC
            LIMIT 100
        )
          AND (
            SELECT COUNT(*)
            FROM push_event_table
        ) > 500
        """
    )
    suspend fun deleteOldest()
}