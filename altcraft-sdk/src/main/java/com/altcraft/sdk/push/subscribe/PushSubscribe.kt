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
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.core.Retry.awaitSubscribeRetryStarted
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.Constants.SUBSCRIBE_C_WORK_TAG
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.RoomRequest.allSubscriptionsByTag
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.sdk_events.EventList.configIsNotSet
import com.altcraft.sdk.sdk_events.EventList.fieldsIsObjects
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.userTagIsNullE
import com.altcraft.sdk.sdk_events.Events.retry
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.MapExtension.mapToJson
import com.altcraft.sdk.network.Request.request
import com.altcraft.sdk.services.manager.ServiceManager.startSubscribeWorker
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.collections.forEach
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
     * @param sync Optional flag passed through as-is to transport layer.
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
                    awaitSubscribeRetryStarted()
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
     * Checks whether push-subscription processing should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
     * @throws CancellationException if the coroutine is cancelled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null): Boolean {
        return try {
            sendAllSubscriptionsMutex.withLock { logic(context, workerId) }
        } catch (e: Exception) {
            retry("isRetry :: pushSubscribe", e); true
        }
    }

    /**
     * Processes pending push-subscription operations in chronological order.
     *
     * - On success, deletes the entity and continues.
     * - On retry-eligible failure with remaining retries:
     *   returns `true` if no newer request exists; otherwise `false`.
     * - On exceeded retry limit, deletes the entity and proceeds.
     *
     * @param context Application context for DB and config access.
     * @param id Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     */
    private suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = SUBSCRIBE_C_WORK_TAG
        val env = Environment.create(context)
        val subscriptions = allSubscriptionsByTag(env.room, env.userTag())

        subscriptions.forEach {
            if (request(context, it) !is retry) entityDelete(env.room, it)
            else if (!isRetryLimit(env.room, it)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }
}