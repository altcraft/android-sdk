package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

/**
 * DAO interface containing query functions for database tables.
 *
 * This interface defines various methods to perform CRUD operations on different tables.
 */
@Keep
@Dao
internal interface DAO {
    // Config
    /** Inserts a configuration item. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigurationEntity)

    /** Returns the first configuration entry, or null if none exist. */
    @Query("SELECT * FROM configurationTable LIMIT 1")
    suspend fun getConfig(): ConfigurationEntity?

    /** Deletes all configuration entries. */
    @Query("DELETE FROM configurationTable")
    suspend fun deleteConfig(): Int

    // Subscribe
    /** Inserts a subscription entry. */
    @Insert
    suspend fun insertSubscribe(entity: SubscribeEntity): Long

    /** Returns subscriptions filtered by user tag, sorted by time. */
    @Query("SELECT * FROM subscribeTable WHERE userTag = :userTag ORDER BY time ASC")
    suspend fun allSubscriptionsByTag(userTag: String?): List<SubscribeEntity>

    /** Deletes a subscription by UID. */
    @Query("DELETE FROM subscribeTable WHERE uid = :uid")
    suspend fun deleteSubscribeByUid(uid: String): Int

    /** Deletes all subscription entries. */
    @Query("DELETE FROM subscribeTable")
    suspend fun deleteAllSubscriptions(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM subscribeTable WHERE userTag = :tag)")
    suspend fun subscriptionsExistsByTag(tag: String): Boolean

    // PushEvent
    /** Inserts a push event. */
    @Insert
    suspend fun insertPushEvent(event: PushEventEntity)

    /** Returns all push events ordered from newest to oldest. */
    @Query("SELECT * FROM pushEventTable ORDER BY time DESC")
    suspend fun getAllPushEvents(): List<PushEventEntity>

    /** Returns true if pushEventTable has at least one row, false otherwise. */
    @Query("SELECT EXISTS(SELECT 1 FROM pushEventTable)")
    suspend fun pushEventsExists(): Boolean

    /** Deletes a push event by UID. */
    @Query("DELETE FROM pushEventTable WHERE uid = :uid")
    suspend fun deletePushEventByUid(uid: String): Int

    /** Deletes all push events. */
    @Query("DELETE FROM pushEventTable")
    suspend fun deleteAllPushEvents()

    /** Returns the number of stored push events. */
    @Query("SELECT COUNT(*) FROM pushEventTable")
    suspend fun getPushEventCount(): Int

    /** Deletes the given list of push events. */
    @Delete
    suspend fun deletePushEvents(events: List<PushEventEntity>)

    /** Returns oldest push events up to a limit. */
    @Query("SELECT * FROM pushEventTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestPushEvents(limit: Int): List<PushEventEntity>

    //Increase retry count

    /** Updates retry count for a subscription by UID. */
    @Query("UPDATE subscribeTable SET retryCount = :newRetryCount WHERE uid = :uid")
    suspend fun increaseSubscribeRetryCount(uid: String, newRetryCount: Int)

    /** Updates retry count for a push event by UID. */
    @Query("UPDATE pushEventTable SET retryCount = :newRetryCount WHERE uid = :uid")
    suspend fun increasePushEventRetryCount(uid: String, newRetryCount: Int)

    //Update
    /**
     * Updates the providerChangeList of the first configuration entry found.
     *
     * @param list The new prioritized list of push providers.
     */
    @Query(
        """
    UPDATE configurationTable
    SET providerPriorityList = :list
    WHERE id = (
        SELECT id FROM configurationTable
        ORDER BY id ASC
        LIMIT 1
    )
    """
    )
    suspend fun updateProviderPriorityList(list: List<String>)
}