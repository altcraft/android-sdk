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

    /**
     * Updates the `providerPriorityList` of the first configuration entry found (lowest `id`).
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

    /** Updates retry count for a subscription by UID. */
    @Query("UPDATE subscribeTable SET retryCount = :newRetryCount WHERE uid = :uid")
    suspend fun increaseSubscribeRetryCount(uid: String, newRetryCount: Int)

    // PushEvent

    /** Inserts a push event. */
    @Insert
    suspend fun insertPushEvent(event: PushEventEntity)

    /** Returns all push events ordered from newest to oldest. */
    @Query("SELECT * FROM pushEventTable ORDER BY time DESC")
    suspend fun getAllPushEvents(): List<PushEventEntity>

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

    /** Updates retry count for a push event by UID. */
    @Query("UPDATE pushEventTable SET retryCount = :newRetryCount WHERE uid = :uid")
    suspend fun increasePushEventRetryCount(uid: String, newRetryCount: Int)

    //MobileEvent

    /** Inserts a mobile event. */
    @Insert
    suspend fun insertMobileEvent(event: MobileEventEntity)

    /** Returns mobile events filtered by user tag, ordered from oldest to newest. */
    @Query("SELECT * FROM mobileEventTable WHERE userTag = :userTag ORDER BY time ASC")
    suspend fun allMobileEventsByTag(userTag: String?): List<MobileEventEntity>

    /** Deletes a mobile event by ID. */
    @Query("DELETE FROM mobileEventTable WHERE id = :id")
    suspend fun deleteMobileEventById(id: Long): Int

    /** Deletes all mobile events. */
    @Query("DELETE FROM mobileEventTable")
    suspend fun deleteAllMobileEvents()

    /** Returns the number of stored mobile events. */
    @Query("SELECT COUNT(*) FROM mobileEventTable")
    suspend fun getMobileEventCount(): Int

    /** Deletes the given list of mobile events. */
    @Delete
    suspend fun deleteMobileEvents(events: List<MobileEventEntity>)

    /** Returns oldest mobile events up to a limit. */
    @Query("SELECT * FROM mobileEventTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestMobileEvents(limit: Int): List<MobileEventEntity>

    /** Updates retry count for a mobile event by ID. */
    @Query("UPDATE mobileEventTable SET retryCount = :newRetryCount WHERE id = :id")
    suspend fun increaseMobileEventRetryCount(id: Long, newRetryCount: Int)
}