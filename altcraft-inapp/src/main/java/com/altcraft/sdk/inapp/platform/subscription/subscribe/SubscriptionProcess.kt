package com.altcraft.sdk.inapp.platform.subscription.subscribe

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.ProfileModel
import com.altcraft.sdk.data.models.error
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.inapp.util.Log
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.util.MapExtension.mapToJson
import com.altcraft.sdk.inapp.data.Constants
import com.altcraft.sdk.inapp.data.room.entity.InAppSubStateEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.subscription.subscribe.request.SubscriptionRequest
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.util.MapBuilder
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.inapp.util.Log.retry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Volatile

/**
 * Contains functions for managing in-app subscription requests.
 *
 * This object:
 * - validates input before enqueueing a request;
 * - stores in-app subscription requests in the local database;
 * - starts a coroutine worker to send pending requests;
 * - processes retry flow for failed requests.
 */
internal object SubscriptionProcess {

    /** Signals all entities requested; ensures startup retries run first. */
    @Volatile
    var snapshotTaken = CompletableDeferred<Unit>()

    /** Processes SDK commands sequentially within this queue. */
    private val queue = CommandQueue.SerialCommandQueue(
        Constants.IN_APP_SUB_QUEUE
    )

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = "inAppSubscribe"

    /** Serializes retry-state checks and updates. */
    val rMutex = Mutex()

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing the current API request. */
    private val request = SubscriptionRequest::inAppSubscribeRequest

    /**
     * Initiates an in-app subscription request by storing request data in the database
     * and triggering the background worker to process pending in-app subscriptions.
     *
     * The function:
     * - waits until SDK initialization is ready;
     * - validates network availability, config, user tag and custom fields;
     * - merges SDK-defined fields with [customFields];
     * - stores the request as an [InAppSubscribeEntity];
     * - starts the in-app subscription coroutine worker.
     *
     * @param context Application context used for config access, DB operations and worker scheduling.
     * @param sync Optional request mode flag (nullable Boolean):
     * - `true` — synchronous mode;
     * - `false` — asynchronous mode;
     * - `null` — default server behavior.
     * @param status Target in-app subscription status. Defaults to [Constants.SUBSCRIBED].
     * @param customFields Optional custom subscription fields to be sent in the request.
     * @param profileFields Optional profile fields to be included in the request.
     * @param cats Optional list of category preferences for the in-app subscription.
     * @param replace If `true`, replaces an existing subscription state on the server.
     * @param skipTriggers If `true`, requests the server to skip trigger execution.
     */
    fun inAppSubscribe(
        context: Context,
        sync: Boolean? = null,
        status: String = Constants.SUBSCRIBED,
        customFields: Map<String, Any?>? = null,
        profileFields: Map<String, Any?>? = null,
        cats: List<ProfileModel.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, snapshotTaken
        ) {
            val env = Environment.create(context)
            if (!CoreEnvironmentProvider.isAuth())
                SDKException.exception(
                    CoreEvents.notAuthenticatedError
                )

            if (SubFunction.fieldsIsObjects(customFields))
                SDKException.exception(
                    ModuleEvents.invalidCustomFields
                )

            val unionFields = MapBuilder.unionMaps(
                context, customFields
            )

            val entity = InAppSubscribeEntity(
                userTag = env.userTag(),
                status = status,
                sync = sync,
                cats = cats,
                replace = replace,
                skipTriggers = skipTriggers,
                acid = env.internalAuth().acid,
                deviceId = env.internalAuth().deviceId,
                customFields = unionFields.mapToJson(),
                profileFields = profileFields?.mapToJson(),
            )

            env.room.dao.subscribe.insert(entity)
            startWorker<SubscriptionWorker>(
                context, Constants.IN_APP_SUBSCRIBE_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether in-app subscription processing should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if another retry attempt should be performed; `false` otherwise.
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
     * Processes pending in-app subscription requests in chronological order.
     *
     * Processing rules:
     * - on non-retry result, the entity is removed from the database;
     * - on retry result with remaining retry attempts, returns `true`
     *   if there is no newer worker request;
     * - otherwise continues processing remaining entities.
     *
     * @param context Application context for DB and SDK environment access.
     * @param id Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     */
    suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = Constants.IN_APP_SUBSCRIBE_O_WORK_TAG
        val env = Environment.create(context)
        val dao = env.room.dao.subscribe
        val subscriptions = dao.getAllEntitiesByTag(env.userTag())

        subscriptions.forEach {
            val response = request(context, it)
            if (response !is error) setInAppSubStatus(context, it)
            if (response !is retry) dao.deleteItem(it) else if (
                !it.retryLimit(dao)) return !hasNewRequest(
                context, tag, id
            )
        }
        return false
    }

    /**
     * Saves in-app subscription state for the current SDK user.
     *
     * The status is taken from [subscription]. The current user tag is taken from
     * [Environment.internalAuth]. If a state for the same user tag already exists,
     * it is fully replaced.
     *
     * @param context Android context used to access environment and database.
     * @param subscription In-app subscription entity received from backend.
     */
    suspend fun setInAppSubStatus(
        context: Context,
        subscription: InAppSubscribeEntity
    ) {
        try {
            val env = Environment.create(
                context
            )
            val tag = env.internalAuth()
                .currentUser

            val state = InAppSubStateEntity(
                userTag = tag ?: return,
                status = subscription.status
            )

            env.room.dao.subState.save(state)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("setInAppSubStatus", e)
        }
    }

    /**
     * Returns in-app subscription status for the current SDK user.
     *
     * Returns `false` when:
     * - current user tag is missing;
     * - subscription state for the current user tag is missing;
     * - saved status is not [Constants.SUBSCRIBED];
     * - reading state fails.
     *
     * @param context Android context used to access environment and database.
     * @return `true` only when saved state for the current user has status [Constants.SUBSCRIBED].
     */
    suspend fun getInAppSubscriptionStatus(
        context: Context
    ): Boolean {
        return try {
            val env = Environment.create(context)
            val authUserTag = env.internalAuth()
                .currentUser ?: return false
            val currentTag = env.userTag()

            currentTag == authUserTag &&
                    env.room.dao.subState.get(
                        authUserTag
                    )?.status == Constants.SUBSCRIBED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("getInAppSubscriptionStatus", e)
            false
        }
    }
}