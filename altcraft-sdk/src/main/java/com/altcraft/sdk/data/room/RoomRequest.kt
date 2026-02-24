package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger.log
import com.altcraft.sdk.additional.StringBuilder.deletedMobileEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedProfileUpdatesMsg
import com.altcraft.sdk.additional.StringBuilder.deletedPushEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedSubscriptionsMsg
import com.altcraft.sdk.core.InitialOperations.mobileEventsDbSnapshotTaken
import com.altcraft.sdk.core.InitialOperations.profileUpdatesDbSnapshotTaken
import com.altcraft.sdk.core.InitialOperations.pushSubscribeDbSnapshotTaken
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.data.Constants.TYPE
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.sdk_events.Message.MOBILE_EVENT_RETRY_LIMIT
import com.altcraft.sdk.sdk_events.Message.PROFILE_UPDATE_RETRY_LIMIT
import com.altcraft.sdk.sdk_events.Message.PUSH_EVENT_RETRY_LIMIT
import com.altcraft.sdk.sdk_events.Message.SUBSCRIBE_RETRY_LIMIT
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles SDK Room database operations.
 *
 * Provides insert, delete, retry, and cleanup logic for Room-managed entities.
 */
internal object RoomRequest {

    /**
     * Inserts the given entity into the database.
     *
     * Supports:
     * `SubscribeEntity`,
     * `PushEventEntity`,
     * `MobileEventEntity`,
     * `ProfileUpdateEntity`.
     *
     * @param context The application context used to get the database instance.
     * @param entity The entity to insert into the database.
     */
    suspend fun entityInsert(context: Context, entity: RequestEntity) {
        try {
            val dao = SDKdb.getDb(context).request()
            when (entity) {
                is SubscribeEntity -> dao.insertSubscribe(entity)
                is PushEventEntity -> dao.insertPushEvent(entity)
                is MobileEventEntity -> dao.insertMobileEvent(entity)
                is ProfileUpdateEntity -> dao.insertProfileUpdate(entity)
            }
        } catch (e: Exception) {
            error("entityInsert", e)
        }
    }

    /**
     * Deletes the given entity from the database.
     *
     * Supports:
     * `SubscribeEntity`,
     * `PushEventEntity`,
     * `MobileEventEntity`,
     * `ProfileUpdateEntity`.
     *
     * @param entity The entity to delete.
     * @param room The database instance.
     */
    suspend fun entityDelete(room: SDKdb, entity: RequestEntity) {
        try {
            val dao = room.request()
            when (entity) {
                is SubscribeEntity -> dao.deleteSubscribeById(entity.requestID)
                is PushEventEntity -> dao.deletePushEventById(entity.requestID)
                is MobileEventEntity -> dao.deleteMobileEventById(entity.requestID)
                is ProfileUpdateEntity -> dao.deleteProfileUpdateById(entity.requestID)
            }
        } catch (e: Exception) {
            error("entityDelete", e)
        }
    }

    /**
     * Increments the retry count in the database for the given entity.
     *
     * Supports:
     * `SubscribeEntity`,
     * `PushEventEntity`,
     * `MobileEventEntity`,
     * `ProfileUpdateEntity`.
     *
     * @param room The database instance.
     * @param entity The entity whose retry count should be increased.
     */
    private suspend fun increaseRetryCount(room: SDKdb, entity: RequestEntity) {
        try {
            val dao = room.request()
            when (entity) {
                is PushEventEntity ->
                    dao.increasePushEventRetryCount(entity.requestID, entity.retryCount + 1)

                is SubscribeEntity ->
                    dao.increaseSubscribeRetryCount(entity.requestID, entity.retryCount + 1)

                is MobileEventEntity ->
                    dao.increaseMobileEventRetryCount(entity.requestID, entity.retryCount + 1)

                is ProfileUpdateEntity ->
                    dao.increaseProfileUpdateRetryCount(entity.requestID, entity.retryCount + 1)
            }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            error("increaseRetryCount", e)
        }
    }

    /**
     * Checks whether the retry limit is exceeded for the given entity.
     *
     * If exceeded, emits an event and deletes the entity; otherwise increments retry count.
     *
     * Supports:
     * `SubscribeEntity`,
     * `PushEventEntity`,
     * `MobileEventEntity`,
     * `ProfileUpdateEntity`.
     *
     * @param room The database instance.
     * @param entity The entity to process.
     * @return `true` if the entity was deleted due to retry limit, otherwise `false`.
     */
    suspend fun isRetryLimit(room: SDKdb, entity: RequestEntity): Boolean {
        return try {
            val isLimit = when (entity) {
                is SubscribeEntity -> entity.retryCount > entity.maxRetryCount
                is PushEventEntity -> entity.retryCount > entity.maxRetryCount
                is MobileEventEntity -> entity.retryCount > entity.maxRetryCount
                is ProfileUpdateEntity -> entity.retryCount > entity.maxRetryCount
            }
            if (isLimit) {
                when (entity) {
                    is SubscribeEntity -> event(
                        "isRetryLimit",
                        480 to SUBSCRIBE_RETRY_LIMIT + entity.requestID
                    )

                    is PushEventEntity -> event(
                        "isRetryLimit",
                        484 to PUSH_EVENT_RETRY_LIMIT + entity.uid,
                        mapOf(UID to entity.uid, TYPE to entity.type)
                    )

                    is MobileEventEntity -> event(
                        "isRetryLimit",
                        487 to MOBILE_EVENT_RETRY_LIMIT + entity.eventName,
                        mapOf(NAME to entity.eventName)
                    )

                    is ProfileUpdateEntity -> event(
                        "isRetryLimit",
                        488 to PROFILE_UPDATE_RETRY_LIMIT + entity.requestID
                    )
                }
                entityDelete(room, entity)
            } else increaseRetryCount(room, entity)
            isLimit
        } catch (_: CancellationException) {
            false
        } catch (e: Exception) {
            error("isRetryLimit", e)
            false
        }
    }

    /**
     * Cleans up old subscriptions if count exceeds limit.
     *
     * @param room Local database instance.
     */
    suspend fun clearOldSubscriptionsFromRoom(room: SDKdb) {
        try {
            room.request().apply {
                if (getSubscribeCount() > 500) {
                    deleteSubscriptions(getOldestSubscriptions(100))
                    log(deletedSubscriptionsMsg(getSubscribeCount()))
                }
            }
        } catch (e: Exception) {
            error("clearOldSubscriptionsFromRoom", e)
        }
    }

    /**
     * Cleans up old push events if count exceeds limit.
     *
     * @param room Local database instance.
     */
    suspend fun clearOldPushEventsFromRoom(room: SDKdb) {
        try {
            room.request().apply {
                if (getPushEventCount() > 500) {
                    deletePushEvents(getOldestPushEvents(100))
                    log(deletedPushEventsMsg(getPushEventCount()))
                }
            }
        } catch (e: Exception) {
            error("clearOldPushEventsFromRoom", e)
        }
    }

    /**
     * Cleans up old mobile events if count exceeds limit.
     *
     * @param room Local database instance.
     */
    suspend fun clearOldMobileEventsFromRoom(room: SDKdb) {
        try {
            room.request().apply {
                if (getMobileEventCount() > 500) {
                    deleteMobileEvents(getOldestMobileEvents(100))
                    log(deletedMobileEventsMsg(getMobileEventCount()))
                }
            }
        } catch (e: Exception) {
            error("clearOldMobileEventsFromRoom", e)
        }
    }

    /**
     * Cleans up old profile updates if count exceeds limit.
     *
     * @param room Local database instance.
     */
    suspend fun clearOldProfileUpdatesFromRoom(room: SDKdb) {
        try {
            room.request().apply {
                if (getProfileUpdateCount() > 500) {
                    deleteProfileUpdates(getOldestProfileUpdates(100))
                    log(deletedProfileUpdatesMsg(getProfileUpdateCount()))
                }
            }
        } catch (e: Exception) {
            error("clearOldProfileUpdatesFromRoom", e)
        }
    }

    /**
     * Cleans up old subscriptions, push events, mobile events, and profile updates in parallel.
     *
     * @param room Local database instance.
     */
    suspend fun roomOverflowControl(room: SDKdb) = coroutineScope {
        awaitAll(
            async { clearOldProfileUpdatesFromRoom(room) },
            async { clearOldSubscriptionsFromRoom(room) },
            async { clearOldMobileEventsFromRoom(room) },
            async { clearOldPushEventsFromRoom(room) },
        )
    }

    /**
     * Returns mobile events by user tag and signals DB snapshot taken.
     *
     * @param room Database instance.
     * @param tag User tag used to filter events.
     * @return List of mobile event entities.
     */
    suspend fun allMobileEventsByTag(
        room: SDKdb,
        tag: String
    ): List<MobileEventEntity> {
        return room.request().allMobileEventsByTag(tag).also {
            mobileEventsDbSnapshotTaken.complete(Unit)
        }
    }

    /**
     * Returns subscriptions by user tag and signals DB snapshot taken.
     *
     * @param room Database instance.
     * @param tag User tag used to filter subscriptions.
     * @return List of subscription entities.
     */
    suspend fun allSubscriptionsByTag(
        room: SDKdb,
        tag: String
    ): List<SubscribeEntity> {
        return room.request().allSubscriptionsByTag(tag).also {
            pushSubscribeDbSnapshotTaken.complete(Unit)
        }
    }

    /**
     * Returns profile updates by user tag and signals DB snapshot taken.
     *
     * @param room Database instance.
     * @param tag User tag used to filter profile updates.
     * @return List of profile update entities.
     */
    suspend fun allProfileUpdatesByTag(
        room: SDKdb,
        tag: String
    ): List<ProfileUpdateEntity> {
        return room.request().allProfileUpdatesByTag(tag).also {
            profileUpdatesDbSnapshotTaken.complete(Unit)
        }
    }
}