package com.altcraft.sdk.push.platform.subscription.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context

import com.altcraft.sdk.util.MapBuilder.unionMaps
import com.altcraft.sdk.util.SubFunction.fieldsIsObjects
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.error
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.util.MapExtension.mapToJson
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants.FUNCTION_SUBSCRIBE
import com.altcraft.sdk.push.data.Constants.PUSH_SUBSCRIBE_O_WORK_TAG
import com.altcraft.sdk.push.data.Constants.PUSH_SUB_QUEUE
import com.altcraft.sdk.push.data.Preferenses.setCurrentToken
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.subscription.subscribe.request.SubscriptionRequest.pushSubscribeRequest
import com.altcraft.sdk.push.platform.token.TokenUpdateProcess.checkUpdateFromRtoken
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.push.util.Log.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.collections.forEach
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Volatile

/**
 * Contains functions for managing the status of a push subscription.
 */
internal object SubscriptionProcess {

    /** Signals that the initial entity snapshot has been loaded. */
    @Volatile
    var snapshotTaken = CompletableDeferred<Unit>()

    /** Processes SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(PUSH_SUB_QUEUE)

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = FUNCTION_SUBSCRIBE

    /** Serializes retry-state checks and updates. */
    val rMutex = Mutex()

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing the current API request. */
    private val request = ::pushSubscribeRequest

    /**
     * Initiates a subscription process by storing subscription details in the database
     * and triggering the background worker to process pending subscriptions.
     *
     * @param context Application context used for DB and worker scheduling.
     * @param sync Optional request mode flag (nullable Boolean):
     * - `true` — synchronous mode (server returns the final processing result);
     * - `false` — asynchronous mode (server returns only queue/acceptance status);
     * - `null` — default synchronous.
     * @param status Target subscription status.
     * @param customFields Optional custom fields to be merged and sent with the request.
     * @param profileFields Optional profile fields to be included with the request.
     * @param cats Optional list of category preferences to accompany the request.
     * @param replace If `true`, marks the request to replace an existing subscription.
     * @param skipTriggers If `true`, asks the server to skip trigger execution.
     */
    fun pushSubscribe(
        context: Context,
        sync: Boolean? = null,
        status: String,
        cats: List<CategoryData>? = null,
        customFields: Map<String, Any?>? = null,
        profileFields: Map<String, Any?>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, snapshotTaken
        ) {
            val env = Environment.create(context)

            if (fieldsIsObjects(customFields)) exception(
                ModuleEvents.invalidCustomFields
            )

            val unionFields = unionMaps(
                context, customFields
            )

            val entity = PushSubscribeEntity(
                status = status,
                sync = sync,
                cats = cats,
                replace = replace,
                userTag = env.userTag(),
                skipTriggers = skipTriggers,
                profileFields = profileFields?.mapToJson(),
                customFields = unionFields.mapToJson(),
            )

            env.room.dao.subscription.insert(entity)
            startWorker<SubscriptionWorker>(
                context, PUSH_SUBSCRIBE_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether push-subscription processing should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if another retry should be scheduled; `false` otherwise.
     * @throws CancellationException if the coroutine is canceled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        rMutex.withLock {
            logic(context, workerId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        retry("isRetry :: $FUNCTION", e)
        true
    }

    /**
     * Processes pending push-subscription operations in chronological order.
     *
     * - If RToken auth is used, updates the push token before processing subscriptions.
     * - Sends request for each entity.
     * - If response is not `error`: stores current token.
     * - If response is not `retry`: deletes the entity.
     * - On `retry`:
     *   - retry available → returns `true` if no newer request;
     *   - retry limit reached → keeps entity and continues.
     *
     * @param context Application context for DB and config access.
     * @param id Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     */
    private suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = PUSH_SUBSCRIBE_O_WORK_TAG
        val env = Environment.create(context)
        val dao = env.room.dao.subscription

        val subscriptions = dao.getAllEntitiesByTag(
            env.userTag()
        )
        checkUpdateFromRtoken(context, subscriptions, env)
        subscriptions.forEach {
            val response = request(context, it)
            if (response !is error) setCurrentToken(context, env.currentToken())
            if (response !is retry) dao.deleteItem(it) else if (!it.retryLimit(dao))
                return !hasNewRequest(
                    context, tag, id
                )
        }
        return false
    }
}
