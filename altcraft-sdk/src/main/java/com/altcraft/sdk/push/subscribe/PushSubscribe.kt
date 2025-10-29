package com.altcraft.sdk.push.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context

import com.altcraft.sdk.additional.MapBuilder.unionMaps
import com.altcraft.sdk.additional.SubFunction.fieldsIsObjects
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.auth.AuthManager.getUserTag
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.concurrency.withInitReady
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.network.Request.pushSubscribeRequest
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.EventList.configIsNull
import com.altcraft.sdk.sdk_events.EventList.fieldsIsObjects
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.userTagIsNull
import com.altcraft.sdk.sdk_events.EventList.userTagIsNullE
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.MapExtension.mapToJson
import com.altcraft.sdk.services.manager.ServiceManager.startSubscribeWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Contains functions for managing the status of a push subscription.
 */
internal object PushSubscribe {

    private val subscriptionMutex = Mutex()
    private val sendAllSubscriptionsMutex = Mutex()

    /**
     * Initiates a subscription process by storing subscription details in the database
     * and triggering the background worker to process pending subscriptions.
     *
     * @param context Application context used for DB and worker scheduling.
     * @param sync Optional flag passed through as-is to transport layer (1 for sync / 0 for async).
     * @param status Target subscription status (default is SUBSCRIBED).
     * @param customFields Optional custom fields to be merged and sent with the request.
     * @param profileFields Optional profile fields to be included with the request.
     * @param cats Optional list of category preferences to accompany the request.
     * @param replace If `true`, marks the request to replace an existing subscription.
     * @param skipTriggers If `true`, asks the server to skip trigger execution.
     */
    fun pushSubscribe(
        context: Context,
        sync: Int? = null,
        status: String = SUBSCRIBED,
        customFields: Map<String, Any?>? = null,
        profileFields: Map<String, Any?>? = null,
        cats: List<DataClasses.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        val initGate = InitBarrier.current()
        CommandQueue.SubscribeCommandQueue.submit {
            try {
                withInitReady("pushSubscribe", gate = initGate) {
                    subscriptionMutex.withLock {
                        val config = getConfig(context) ?: exception(configIsNotSet)
                        if (fieldsIsObjects(customFields)) exception(fieldsIsObjects)
                        val tag = getUserTag(config.rToken) ?: exception(userTagIsNullE)
                        if (!isOnline(context)) error("pushSubscribe", noInternetConnect)

                        val unionFields = unionMaps(context, config, customFields)

                        val entity = SubscribeEntity(
                            userTag = tag,
                            status = status,
                            sync = sync,
                            cats = cats,
                            replace = replace,
                            skipTriggers = skipTriggers,
                            profileFields = profileFields?.mapToJson(),
                            customFields = unionFields.mapToJson(),
                        )

                        entityInsert(context, entity)

                        startSubscribeWorker(context, config)
                    }
                }
            } catch (e: Exception) {
                error("pushSubscribe", e)
            }
        }
    }

    /**
     * Determines whether subscription processing should be retried for the current user tag.
     *
     * Iterates through subscriptions ordered by time (oldest first):
     * - If the request succeeds, the entity is deleted and processing continues.
     * - If it fails with a retry error and the limit is not reached, returns `true`.
     * - If the limit is reached, logs and deletes the entity. If no more entries remain,
     *   returns `false`; otherwise continues with the next record.
     *
     * Ensures sequential execution via a mutex to prevent concurrent access.
     *
     * @param context Application context for config, user tag, and DB access.
     * @return `true` if another retry is required; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context): Boolean {
        return try {
            sendAllSubscriptionsMutex.withLock {
                val config = getConfig(context) ?: exception(configIsNull)
                val tag = getUserTag(config.rToken) ?: exception(userTagIsNull)

                val room = SDKdb.getDb(context)

                room.request().allSubscriptionsByTag(tag).forEach {
                    if (pushSubscribeRequest(context, it) is retry) {
                        if (!isRetryLimit(room, it)) return true
                    } else {
                        entityDelete(room, it)
                    }
                }
                false
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            retry("isRetry :: pushSubscribe", e)
            true
        }
    }
}