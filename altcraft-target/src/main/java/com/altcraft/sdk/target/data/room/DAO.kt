package com.altcraft.sdk.target.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.target.events.ModuleEvents
import com.altcraft.sdk.target.platform.target.TargetProcess.snapshotTaken
import com.altcraft.sdk.target.util.Log

/**
 * DAO for stored Target module mobile event requests.
 */
@Dao
internal interface DAO {

    /**
     * Inserts the specified mobile event.
     *
     * An existing entity with the same request ID is replaced.
     *
     * @param entity mobile event entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Entity)

    /**
     * Deletes the specified mobile event.
     *
     * @param entity mobile event entity to delete.
     */
    @Delete
    suspend fun delete(entity: Entity)

    /**
     * Increments the retry count for the specified request.
     *
     * @param requestID request identifier.
     */
    @Query(
        """
        UPDATE target_table
        SET retryCount = retryCount + 1
        WHERE requestID = :requestID
        """
    )
    suspend fun increaseRetryCount(requestID: String)

    /**
     * Deletes the request when its retry count exceeds the configured limit.
     *
     * @param requestID request identifier.
     * @return number of deleted rows.
     */
    @Query(
        """
        DELETE FROM target_table
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
            ModuleEvents.retryLimit
        )
        return true
    }

    /**
     * Deletes the 100 oldest requests when the table contains more than
     * 500 records.
     *
     * @return number of deleted rows.
     */
    @Query(
        """
        DELETE FROM target_table
        WHERE requestID IN (
            SELECT requestID
            FROM target_table
            ORDER BY time ASC
            LIMIT 100
        )
        AND (
            SELECT COUNT(*)
            FROM target_table
        ) > 500
        """
    )
    suspend fun deleteOldest()

    /**
     * Returns mobile events belonging to the specified user.
     *
     * Events are returned in chronological order.
     *
     * @param userTag user tag used to filter requests.
     * @return stored mobile event entities.
     */
    @Query(
        """
        SELECT *
        FROM target_table
        WHERE userTag = :userTag
        ORDER BY time ASC
        """
    )
    suspend fun getAllByTagInternal(userTag: String): List<Entity>

    /**
     * Returns mobile events and completes the initial snapshot signal.
     *
     * @param userTag user tag used to filter requests.
     * @return stored mobile event entities.
     */
    suspend fun getAllEntitiesByTag(userTag: String): List<Entity> {
        return getAllByTagInternal(userTag).also {
            if (!snapshotTaken.isCompleted) {
                snapshotTaken.complete(Unit)
            }
        }
    }
}