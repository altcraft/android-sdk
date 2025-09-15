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
import com.altcraft.sdk.network.Request.subscribeRequest
import com.altcraft.sdk.data.Constants.SUBSCRIBED
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.RoomRequest.isRetryLimit
import com.altcraft.sdk.data.room.RoomRequest.entityDelete
import com.altcraft.sdk.data.room.RoomRequest.entityInsert
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.events.EventList.configIsNotSet
import com.altcraft.sdk.events.EventList.configIsNull
import com.altcraft.sdk.events.EventList.fieldsIsObjects
import com.altcraft.sdk.events.EventList.noInternetConnect
import com.altcraft.sdk.events.EventList.userTagIsNull
import com.altcraft.sdk.events.EventList.userTagIsNullE
import com.altcraft.sdk.events.Events.retry
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.extension.MapExtension.mapToJson
import com.altcraft.sdk.services.manager.ServiceManager.startSubscribeWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Contains public functions for executing profile subscription-related requests and obtaining
 * profile information.
 */
internal object PushSubscribe {

    private val subscriptionMutex = Mutex()
    private val sendAllSubscriptionsMutex = Mutex()

    /**
     * Initiates a subscription process by storing subscription details in the database
     * and triggering the background worker to process pending subscriptions.
     *
     * @param context The application context used for database operations.
     * @param status The subscription status (default is "subscribe").
     * @param sync An optional synchronization flag.
     * @param customFields Additional custom fields associated with the subscription.
     * @param cats A map of category preferences for the subscription.
     * @param replace A flag indicating whether to replace an existing subscription.
     * @param skipTriggers A flag indicating whether to skip associated triggers.
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
     * Determines whether any subscription should be retried
     *
     * This function retrieves all subscription records associated with the current user tag
     * and attempts to process them. If a subscription request fails with a `RetryError`,
     * it checks whether the retry limit has been reached. If the limit is exceeded, the subscription
     * is deleted; otherwise, the retry count is incremented, and the subscription will be retried.
     *
     * Additionally, this function:
     * - Ensures subscriptions are processed sequentially.
     * - Calls `checkSubServerClosed()` after processing.
     * - Releases the subscription lock to allow further operations.
     *
     * @return `true` if at least one subscription should be retried, otherwise `false`.
     */
    suspend fun isRetry(context: Context): Boolean {
        return try {
            sendAllSubscriptionsMutex.withLock {

                val config = getConfig(context) ?: exception(configIsNull)
                val tag = getUserTag(config.rToken) ?: exception(userTagIsNull)

                val room = SDKdb.getDb(context)

                room.request().allSubscriptionsByTag(tag).forEach {
                    if (subscribeRequest(context, it) is retry) {
                        return !isRetryLimit(room, it)
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