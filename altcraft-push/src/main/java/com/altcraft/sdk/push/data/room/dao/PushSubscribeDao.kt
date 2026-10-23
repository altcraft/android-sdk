package com.altcraft.sdk.push.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.push.util.Log

/**
 * DAO for stored push subscription requests.
 */
@Dao
internal interface PushSubscribeDao {

    /**
     * Inserts the specified push subscription.
     *
     * @param entity push subscription entity to save.
     */
    @Insert
    suspend fun insert(entity: PushSubscribeEntity)

    /**
     * Deletes the specified push subscription.
     *
     * @param entity push subscription entity to delete.
     */
    @Delete
    suspend fun deleteItem(entity: PushSubscribeEntity)

    /**
     * Deletes all stored push subscriptions.
     */
    @Query(
        """
        DELETE FROM push_subscribe_table
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
        UPDATE push_subscribe_table
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
        DELETE FROM push_subscribe_table
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
        DELETE FROM push_subscribe_table
        WHERE requestID IN (
            SELECT requestID
            FROM push_subscribe_table
            ORDER BY time ASC
            LIMIT 100
        )
          AND (
            SELECT COUNT(*)
            FROM push_subscribe_table
        ) > 500
        """
    )
    suspend fun deleteOldest()

    /**
     * Returns push subscriptions for the specified user tag.
     *
     * @param userTag user tag used to filter push subscriptions.
     * @return list of stored push subscription entities.
     */
    @Query(
        """
        SELECT *
        FROM push_subscribe_table
        WHERE userTag = :userTag
        ORDER BY time ASC
        """
    )
    suspend fun getAllByTagInternal(
        userTag: String?
    ): List<PushSubscribeEntity>

    /**
     * Returns push subscriptions and completes snapshot on first access.
     *
     * @param userTag user tag used to filter push subscriptions.
     * @return list of stored push subscription entities.
     */
    suspend fun getAllEntitiesByTag(
        userTag: String?
    ): List<PushSubscribeEntity> {
        return getAllByTagInternal(userTag).also {
            if (!SubscriptionProcess.snapshotTaken.isCompleted) {
                SubscriptionProcess.snapshotTaken.complete(Unit)
            }
        }
    }
}