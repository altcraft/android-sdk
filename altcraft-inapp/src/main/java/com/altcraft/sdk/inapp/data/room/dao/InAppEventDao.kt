package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.util.Log

/**
 * DAO for stored in-app event requests.
 */
@Dao
internal interface InAppEventDao {

    /**
     * Inserts the specified in-app event.
     *
     * @param entity in-app event entity to save.
     */
    @Insert
    suspend fun insert(entity: InAppEventEntity)

    /**
     * Returns all stored in-app events.
     *
     * @return list of stored in-app event entities.
     */
    @Query(
        """
        SELECT *
        FROM in_app_event_table
        ORDER BY time DESC
        """
    )
    suspend fun getAll(): List<InAppEventEntity>

    /**
     * Deletes the specified in-app event.
     *
     * @param entity in-app event entity to delete.
     */
    @Delete
    suspend fun deleteItem(entity: InAppEventEntity)

    /**
     * Returns whether there are no stored in-app events.
     *
     * @return `true` if the table is empty.
     */
    @Query(
        """
        SELECT NOT EXISTS(
            SELECT 1
            FROM in_app_event_table
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
        UPDATE in_app_event_table
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
        DELETE FROM in_app_event_table
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
        DELETE FROM in_app_event_table
        WHERE requestID IN (
            SELECT requestID
            FROM in_app_event_table
            ORDER BY time ASC
            LIMIT 100
        )
          AND (
            SELECT COUNT(*)
            FROM in_app_event_table
        ) > 500
        """
    )
    suspend fun deleteOldest()
}