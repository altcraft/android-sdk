package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.StringBuilder.deletedPushEventsMsg
import com.altcraft.sdk.additional.StringBuilder.errorEntityType
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.events.EventList.unsupportedEntityType
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.events.Events.event
import com.altcraft.sdk.events.Message.PUSH_EVENT_RETRY_LIMIT
import com.altcraft.sdk.events.Message.SUBSCRIBE_RETRY_LIMIT
import com.altcraft.sdk.extension.ExceptionExtension.exception
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
     * Supports both `SubscribeEntity` and `PushEventEntity`.
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
                else -> error("entityInsert", errorEntityType(entity::class.simpleName))
            }
        } catch (e: Exception) {
            error("entityInsert", e)
        }
    }

    /**
     * Deletes the given entity from the database.
     *
     * Supports both `PushEventEntity` (by `uid`) and `SubscribeEntity` (by `id`).
     *
     * @param entity The entity to delete.
     * @param room The database access point.
     */
    suspend fun entityDelete(room: SDKdb, entity: Any) {
        try {
            when (entity) {
                is SubscribeEntity -> room.request().deleteSubscribeByUid(entity.uid)
                is PushEventEntity -> room.request().deletePushEventByUid(entity.uid)
                else -> error("entityDelete", errorEntityType(entity::class.simpleName))
            }
        } catch (e: Exception) {
            error("entityDelete", e)
        }
    }

    /**
     * Increments the retry count for a supported entity.
     *
     * This function handles both `PushEventEntity` and `SubscribeEntity` types.
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
     * Supports both `SubscribeEntity` and `PushEventEntity`.
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
                else -> exception(unsupportedEntityType)
            }
            if (isLimit) {
                when (entity) {
                    is SubscribeEntity -> event(
                        "isRetryLimit",
                        420 to SUBSCRIBE_RETRY_LIMIT + entity.uid
                    )

                    is PushEventEntity -> event(
                        "isRetryLimit",
                        421 to PUSH_EVENT_RETRY_LIMIT + entity.uid,
                        mapOf(UID to entity.uid)
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
     * Cleans up old push events if count exceeds limit.
     *
     * @param room Local database instance.
     */
    suspend fun clearOldPushEventsFromRoom(room: SDKdb) {
        try {
            room.request().apply {
                if (getPushEventCount() > 500) {
                    deletePushEvents(getOldestPushEvents(100))
                    logger(deletedPushEventsMsg(getPushEventCount()))
                }
            }
        } catch (e: Exception) {
            error("clearOldPushEventsFromRoom", e)
        }
    }

    /**
     * Returns `true` if a subscription with the given tag exists in the local database.
     * Returns `false` on error or if not found.
     *
     * @param context application context
     * @param userTag subscription tag
     */
    suspend fun existSubscriptions(context: Context, userTag: String): Boolean {
        return try {
            SDKdb.getDb(context).request().subscriptionsExistsByTag(userTag)
        } catch (e: Exception) {
            error("existSubscription", e)
            false
        }
    }


    /**
     * Returns `true` if `pushEventTable` contains at least one row.
     * Returns `false` on error or if empty.
     *
     * @param context application context
     */
    suspend fun existPushEvents(context: Context): Boolean {
        return try {
            SDKdb.getDb(context).request().pushEventsExists()
        } catch (e: Exception) {
            error("existPushEvents", e)
            false
        }
    }
}