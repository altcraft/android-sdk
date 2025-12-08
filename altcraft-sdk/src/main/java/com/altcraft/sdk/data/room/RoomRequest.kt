package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.Logger.log
import com.altcraft.sdk.additional.StringBuilder.deletedMobileEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedPushEventsMsg
import com.altcraft.sdk.additional.StringBuilder.deletedSubscriptionsMsg
import com.altcraft.sdk.additional.StringBuilder.errorEntityType
import com.altcraft.sdk.core.Retry.mobileEventsDbSnapshotTaken
import com.altcraft.sdk.core.Retry.pushSubscribeDbSnapshotTaken
import com.altcraft.sdk.data.Constants.NAME
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.sdk_events.EventList.unsupportedEntityType
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import com.altcraft.sdk.sdk_events.Message.PUSH_EVENT_RETRY_LIMIT
import com.altcraft.sdk.sdk_events.Message.SUBSCRIBE_RETRY_LIMIT
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.sdk_events.Message.MOBILE_EVENT_RETRY_LIMIT
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
     * Supports both `SubscribeEntity`, `PushEventEntity`, "MobileEventEntity".
     *
     * Determines the DAO insert method based on the entity type.
     *
     * @param context The application context used to get the database instance.
     * @param entity The entity to insert into the database.
     *
     * @throws IllegalArgumentException If the entity type is unsupported.
     */
    suspend fun entityInsert(context: Context, entity: Any) {
        try {
            when (entity) {
                is SubscribeEntity -> SDKdb.getDb(context).request().insertSubscribe(entity)
                is PushEventEntity -> SDKdb.getDb(context).request().insertPushEvent(entity)
                is MobileEventEntity -> SDKdb.getDb(context).request().insertMobileEvent(entity)
                else -> error("entityInsert", errorEntityType(entity::class.simpleName))
            }
        } catch (e: Exception) {
            error("entityInsert", e)
        }
    }

    /**
     * Deletes the given entity from the database.
     *
     * Supports both `SubscribeEntity`, `PushEventEntity`, "MobileEventEntity".
     *
     * @param entity The entity to delete.
     * @param room The database access point.
     */
    suspend fun entityDelete(room: SDKdb, entity: Any) {
        try {
            when (entity) {
                is SubscribeEntity -> room.request().deleteSubscribeByUid(entity.uid)
                is PushEventEntity -> room.request().deletePushEventByUid(entity.uid)
                is MobileEventEntity -> room.request().deleteMobileEventById(entity.id)
                else -> error("entityDelete", errorEntityType(entity::class.simpleName))
            }
        } catch (e: Exception) {
            error("entityDelete", e)
        }
    }

    /**
     * Increments the retry count for a supported entity.
     *
     * Supports both `SubscribeEntity`, `PushEventEntity`, "MobileEventEntity".
     *
     * For each, it updates the `retryCount` value in the database by incrementing it by 1.
     *
     * @param room The database instance.
     * @param entity The entity whose retry count should be increased.
     *
     * @throws IllegalArgumentException If the entity type is unsupported.
     */
    private suspend fun increaseRetryCount(room: SDKdb, entity: Any) {
        try {
            when (entity) {
                is PushEventEntity -> room.request()
                    .increasePushEventRetryCount(entity.uid, entity.retryCount + 1)

                is SubscribeEntity -> room.request()
                    .increaseSubscribeRetryCount(entity.uid, entity.retryCount + 1)

                is MobileEventEntity -> room.request()
                    .increaseMobileEventRetryCount(entity.id, entity.retryCount + 1)

                else -> error("increaseRetryCount", errorEntityType(entity::class.simpleName))
            }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            error("increaseRetryCount", e)
        }
    }

    /**
     * Checks if the retry limit has been exceeded for a given entity.
     * If the limit is reached, logs and deletes the entity.
     * Otherwise, increments the retry count.
     *
     * Supports both `SubscribeEntity`, `PushEventEntity`, "MobileEventEntity".
     *
     * @param room The database instance.
     * @param entity The entity to process.
     * @return `true` if the retry limit was exceeded and the entity was deleted, otherwise `false`.
     */
    suspend fun isRetryLimit(room: SDKdb, entity: Any): Boolean {
        return try {
            val isLimit = when (entity) {
                is SubscribeEntity -> entity.retryCount > entity.maxRetryCount
                is PushEventEntity -> entity.retryCount > entity.maxRetryCount
                is MobileEventEntity -> entity.retryCount > entity.maxRetryCount
                else -> exception(unsupportedEntityType)
            }
            if (isLimit) {
                when (entity) {
                    is SubscribeEntity -> event(
                        "isRetryLimit",
                        480 to SUBSCRIBE_RETRY_LIMIT + entity.uid
                    )

                    is PushEventEntity -> event(
                        "isRetryLimit",
                        484 to PUSH_EVENT_RETRY_LIMIT + entity.uid,
                        mapOf(UID to entity.uid)
                    )

                    is MobileEventEntity -> event(
                        "isRetryLimit",
                        485 to MOBILE_EVENT_RETRY_LIMIT + entity.eventName,
                        mapOf(NAME to entity.eventName)
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
     * Cleans up old subscribe entries if count exceeds limit.
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
     * Cleans up old subscriptions, push events and mobile events in parallel.
     *
     * @param room Local database instance.
     */
    suspend fun roomOverflowControl(room: SDKdb) = coroutineScope {
        awaitAll(
            async { clearOldSubscriptionsFromRoom(room) },
            async { clearOldMobileEventsFromRoom(room) },
            async { clearOldPushEventsFromRoom(room) },
        )
    }

    /**
     * Retrieves all mobile events for the specified user tag
     * and marks the mobile-events DB snapshot as taken.
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
     * Retrieves all push subscriptions for the specified user tag
     * and marks the subscription DB snapshot as taken.
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
}