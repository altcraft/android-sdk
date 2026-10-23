package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess.snapshotTaken
import com.altcraft.sdk.inapp.util.Log

/**
 * DAO for stored in-app subscription requests.
 */
@Dao
internal interface InAppSubscribeDao {

    /**
     * Inserts the specified in-app subscription.
     *
     * @param entity in-app subscription entity to save.
     */
    @Insert
    suspend fun insert(entity: InAppSubscribeEntity)

    /**
     * Deletes the specified in-app subscription.
     *
     * @param entity in-app subscription entity to delete.
     */
    @Delete
    suspend fun deleteItem(entity: InAppSubscribeEntity)

    /**
     * Deletes all stored in-app subscriptions.
     */
    @Query(
        """
        DELETE FROM in_app_subscribe_table
        """
    )
    suspend fun deleteAll()

    /**
     * Increments retry count for the specified request.
     *
     * @param requestID request identifier.
     */
    @Query(
        """
        UPDATE in_app_subscribe_table
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
        DELETE FROM in_app_subscribe_table
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
            ModuleEvents.subscribeRetryLimit
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
        DELETE FROM in_app_subscribe_table
        WHERE requestID IN (
            SELECT requestID
            FROM in_app_subscribe_table
            ORDER BY time ASC
            LIMIT 100
        )
          AND (
            SELECT COUNT(*)
            FROM in_app_subscribe_table
        ) > 500
        """
    )
    suspend fun deleteOldest()

    /**
     * Returns in-app subscriptions for the specified user tag.
     *
     * @param userTag user tag used to filter subscriptions.
     * @return list of stored in-app subscription entities.
     */
    @Query(
        """
        SELECT *
        FROM in_app_subscribe_table
        WHERE userTag = :userTag
        ORDER BY time ASC
        """
    )
    suspend fun getAllByTagInternal(userTag: String?): List<InAppSubscribeEntity>

    /**
     * Returns in-app subscriptions and completes snapshot on first access.
     *
     * @param userTag user tag used to filter subscriptions.
     * @return list of stored in-app subscription entities.
     */
    suspend fun getAllEntitiesByTag(userTag: String?): List<InAppSubscribeEntity> {
        return getAllByTagInternal(userTag).also {
            if (!snapshotTaken.isCompleted) snapshotTaken.complete(Unit)
        }
    }
}