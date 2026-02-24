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

    /** Inserts or replaces a configuration entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigurationEntity)

    /** Returns the first configuration entry, or null if none exist. */
    @Query("SELECT * FROM configurationTable LIMIT 1")
    suspend fun getConfig(): ConfigurationEntity?

    /** Deletes all configuration entries and returns the number of rows deleted. */
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

    /** Inserts a subscription entry and returns the new row ID. */
    @Insert
    suspend fun insertSubscribe(entity: SubscribeEntity): Long

    /** Returns subscriptions filtered by user tag, sorted by time. */
    @Query("SELECT * FROM subscribeTable WHERE userTag = :userTag ORDER BY time ASC")
    suspend fun allSubscriptionsByTag(userTag: String?): List<SubscribeEntity>

    /** Deletes a subscription by request ID and returns the number of rows deleted. */
    @Query("DELETE FROM subscribeTable WHERE requestID = :requestID")
    suspend fun deleteSubscribeById(requestID: String): Int

    /** Deletes all subscription entries and returns the number of rows deleted. */
    @Query("DELETE FROM subscribeTable")
    suspend fun deleteAllSubscriptions()

    /** Updates the retry count for a subscription by request ID. */
    @Query("UPDATE subscribeTable SET retryCount = :newRetryCount WHERE requestID = :requestID")
    suspend fun increaseSubscribeRetryCount(requestID: String, newRetryCount: Int)

    /** Returns the number of stored subscriptions. */
    @Query("SELECT COUNT(*) FROM subscribeTable")
    suspend fun getSubscribeCount(): Int

    /** Deletes the given list of subscriptions. */
    @Delete
    suspend fun deleteSubscriptions(subscriptions: List<SubscribeEntity>)

    /** Returns the oldest subscriptions up to a limit. */
    @Query("SELECT * FROM subscribeTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestSubscriptions(limit: Int): List<SubscribeEntity>

    // PushEvent

    /** Inserts a push event. */
    @Insert
    suspend fun insertPushEvent(event: PushEventEntity)

    /** Returns all push events ordered from newest to oldest. */
    @Query("SELECT * FROM pushEventTable ORDER BY time DESC")
    suspend fun getAllPushEvents(): List<PushEventEntity>

    /** Deletes a push event by request ID and returns the number of rows deleted. */
    @Query("DELETE FROM pushEventTable WHERE requestID = :requestID")
    suspend fun deletePushEventById(requestID: String): Int

    /** Deletes all push events. */
    @Query("DELETE FROM pushEventTable")
    suspend fun deleteAllPushEvents()

    /** Returns the number of stored push events. */
    @Query("SELECT COUNT(*) FROM pushEventTable")
    suspend fun getPushEventCount(): Int

    /** Deletes the given list of push events. */
    @Delete
    suspend fun deletePushEvents(events: List<PushEventEntity>)

    /** Returns the oldest push events up to a limit. */
    @Query("SELECT * FROM pushEventTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestPushEvents(limit: Int): List<PushEventEntity>

    /** Updates the retry count for a push event by request ID. */
    @Query("UPDATE pushEventTable SET retryCount = :newRetryCount WHERE requestID = :requestID")
    suspend fun increasePushEventRetryCount(requestID: String, newRetryCount: Int)

    // MobileEvent

    /** Inserts a mobile event. */
    @Insert
    suspend fun insertMobileEvent(event: MobileEventEntity)

    /** Returns mobile events filtered by user tag, ordered from oldest to newest. */
    @Query("SELECT * FROM mobileEventTable WHERE userTag = :userTag ORDER BY time ASC")
    suspend fun allMobileEventsByTag(userTag: String?): List<MobileEventEntity>

    /** Deletes a mobile event by request ID and returns the number of rows deleted. */
    @Query("DELETE FROM mobileEventTable WHERE requestID = :requestID")
    suspend fun deleteMobileEventById(requestID: String): Int

    /** Deletes all mobile events. */
    @Query("DELETE FROM mobileEventTable")
    suspend fun deleteAllMobileEvents()

    /** Returns the number of stored mobile events. */
    @Query("SELECT COUNT(*) FROM mobileEventTable")
    suspend fun getMobileEventCount(): Int

    /** Deletes the given list of mobile events. */
    @Delete
    suspend fun deleteMobileEvents(events: List<MobileEventEntity>)

    /** Returns the oldest mobile events up to a limit. */
    @Query("SELECT * FROM mobileEventTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestMobileEvents(limit: Int): List<MobileEventEntity>

    /** Updates the retry count for a mobile event by request ID. */
    @Query("UPDATE mobileEventTable SET retryCount = :newRetryCount WHERE requestID = :requestID")
    suspend fun increaseMobileEventRetryCount(requestID: String, newRetryCount: Int)

    // ProfileUpdate

    /** Inserts a profile update request. */
    @Insert
    suspend fun insertProfileUpdate(entity: ProfileUpdateEntity)

    /** Returns profile updates filtered by user tag, ordered from oldest to newest. */
    @Query("SELECT * FROM profileUpdateTable WHERE userTag = :userTag ORDER BY time ASC")
    suspend fun allProfileUpdatesByTag(userTag: String?): List<ProfileUpdateEntity>

    /** Deletes a profile update by request ID and returns the number of rows deleted. */
    @Query("DELETE FROM profileUpdateTable WHERE requestID = :requestID")
    suspend fun deleteProfileUpdateById(requestID: String): Int

    /** Deletes all profile updates. */
    @Query("DELETE FROM profileUpdateTable")
    suspend fun deleteAllProfileUpdates()

    /** Returns the number of stored profile updates. */
    @Query("SELECT COUNT(*) FROM profileUpdateTable")
    suspend fun getProfileUpdateCount(): Int

    /** Deletes the given list of profile updates. */
    @Delete
    suspend fun deleteProfileUpdates(entities: List<ProfileUpdateEntity>)

    /** Returns the oldest profile updates up to a limit. */
    @Query("SELECT * FROM profileUpdateTable ORDER BY time ASC LIMIT :limit")
    suspend fun getOldestProfileUpdates(limit: Int): List<ProfileUpdateEntity>

    /** Updates the retry count for a profile update by request ID. */
    @Query("UPDATE profileUpdateTable SET retryCount = :newRetryCount WHERE requestID = :requestID")
    suspend fun increaseProfileUpdateRetryCount(requestID: String, newRetryCount: Int)
}
