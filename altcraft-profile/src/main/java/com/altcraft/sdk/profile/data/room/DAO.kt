package com.altcraft.sdk.profile.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.profile.events.ModuleEvents
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess.snapshotTaken
import com.altcraft.sdk.profile.util.Log

/**
 * DAO for stored profile update requests.
 */
@Dao
internal interface DAO {

    /**
     * Inserts the specified profile update.
     *
     * @param entity profile update entity to save.
     */
    @Insert
    suspend fun insert(entity: Entity)

    /**
     * Deletes the specified profile update.
     *
     * @param entity profile update entity to delete.
     */
    @Delete
    suspend fun delete(entity: Entity)

    /**
     * Increments retry count for the specified request.
     *
     * @param requestID request identifier.
     */
    @Query(
        """
        UPDATE profile_update_table
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
        DELETE FROM profile_update_table
        WHERE requestID = :requestID
          AND retryCount > maxRetryCount
        """
    )
    suspend fun deleteIfRetryLimitExceeded(requestID: String): Int

    /**
     * Increments retry count and deletes the request if the retry limit is exceeded.
     *
     * @param requestID request identifier.
     * @return true if the request was deleted.
     */
    @Transaction
    suspend fun incrementRetryAndDeleteIfExceeded(
        requestID: String
    ): Boolean {
        increaseRetryCount(requestID)

        if (deleteIfRetryLimitExceeded(requestID) == 0) return false
        Log.event(
            "incrementRetryAndDeleteIfExceeded", ModuleEvents.retryLimit
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
        DELETE FROM profile_update_table
        WHERE requestID IN (
            SELECT requestID
            FROM profile_update_table
            ORDER BY time ASC
            LIMIT 100
        )
        AND (
            SELECT COUNT(*)
            FROM profile_update_table
        ) > 500
        """
    )
    suspend fun deleteOldest()

    /**
     * Returns profile updates belonging to the specified user.
     *
     * Updates are returned in chronological order.
     *
     * @param userTag user tag used to filter requests.
     * @return stored profile update entities.
     */
    @Query(
        """
        SELECT *
        FROM profile_update_table
        WHERE userTag = :userTag
        ORDER BY time ASC
        """
    )
    suspend fun getAllByTagInternal(userTag: String?): List<Entity>

    /**
     * Returns profile updates and completes the initial snapshot signal.
     *
     * @param userTag user tag used to filter requests.
     * @return stored profile update entities.
     */
    suspend fun getAllEntitiesByTag(userTag: String?): List<Entity> {
        return getAllByTagInternal(userTag).also {
            if (!snapshotTaken.isCompleted) {
                snapshotTaken.complete(Unit)
            }
        }
    }
}
