package com.altcraft.sdk.inapp.platform.placements

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_O_WORK_TAG
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_QUEUE
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel.ResponseWithInApps
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.inapp.platform.event.EventProcess.sendInAppEvent
import com.altcraft.sdk.inapp.platform.placements.request.PlacementsRequest.inAppPlacementsRequest
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.placements.PlacementCountManager.maxPlacementsChanged
import com.altcraft.sdk.inapp.platform.placements.PlacementCountManager.trimToMax
import com.altcraft.sdk.inapp.system.presentation.Presenter
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.InAppEventType
import com.altcraft.sdk.util.SubFunction.time
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
 * Contains functions for requesting in-app placements.
 */
internal object PlacementsProcess {
    /** Processes SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(
        IN_APP_PLACEMENTS_QUEUE
    )

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = "inAppPlacements"

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing the current API request. */
    private val request = ::inAppPlacementsRequest

    /**
     * Signals when in-app placements are ready for trigger processing.
     *
     * Re-armed by [reset] when the SDK clears in-app data, so triggers
     * scheduled after the clear wait for the next placements request.
     */
    @Volatile
    var placementsReady = CompletableDeferred<Unit>()
        private set

    /**
     * Rearms the placements readiness gate.
     *
     * Trigger workers parked on the previous gate are released, and a new
     * gate is installed so subsequent triggers wait for the next
     * placements request.
     */
    fun reset() {
        val previous = placementsReady
        if (!previous.isCompleted) previous.complete(Unit)
        placementsReady = CompletableDeferred()
    }

    /**
     * Stores in-app placements request data and starts background processing.
     *
     * Automatic requests are skipped when the in-app module configuration
     * disables `inAppAutoRequest`. A manual user request (`userReq = true`)
     * always proceeds regardless of the flag value.
     *
     * When the automatic request is skipped, placements are immediately
     * marked ready so queued in-app triggers are not left waiting for a
     * request that will never happen.
     *
     * @param context Application context.
     */
    fun requestPlacements(
        context: Context,
        userReq: Boolean = false,
        workerReq: Boolean = false,
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, null
        ) {
            val env = Environment.create(context)

            if (!env.config().inAppAutoRequest
                && !userReq
            ) {
                placementsReady()
                return@submitSdkCommand
            }
            val inAppsDao = env.room.dao.inApps
            val inAppDao = env.room.dao.inApp
            val auth = env.internalAuth()
            val state = inAppsDao.get()

            if (state?.acid != auth.acid ||
                state.deviceId != auth.deviceId
            ) {
                inAppDao.clearInAppData()
                inAppsDao.clearInAppsData()
            } else if (
                !userReq && !auth.isTest
                && !maxPlacementsChanged.get()
                && isRequestTooEarly(
                    state,
                    env.config()
                        .placementsRequestInterval
                )
            ) return@submitSdkCommand

            maxPlacementsChanged.set(false)
            if (workerReq) logic(context, null)
            else startWorker<PlacementsWorker>(
                context, IN_APP_PLACEMENTS_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether in-app placements processing should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if another retry should be scheduled; `false` otherwise.
     * @throws CancellationException if the coroutine is canceled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        mutex.withLock {
            logic(context, workerId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        retry("isRetry :: $FUNCTION", e)
        true
    }

    /**
     * Processes pending in-app placements requests in chronological order.
     *
     * The method:
     *
     * 1. Loads the current in-app synchronization state or creates a default one.
     * 2. Requests actual in-app placements from backend.
     * 3. Stops processing early if retry is required and no newer request exists.
     * 4. Saves and processes received in-app placements response.
     * 5. Updates request timestamp when response body is empty.
     * 6. Marks placements as ready for trigger processing.
     *
     * @param context Application context.
     * @param id Optional worker identifier.
     * @return `true` if another retry pass is required; `false` otherwise.
     */
    private suspend fun logic(
        context: Context,
        id: UUID? = null
    ): Boolean {
        val tag = IN_APP_PLACEMENTS_O_WORK_TAG
        val env = Environment.create(context)
        val inAppsDao = env.room.dao.inApps
        val auth = env.internalAuth()
        val now = time()

        // Getting current in-app synchronization
        // state or creating default state.
        val state = inAppsDao.get()
            ?: InAppsEntity(
                deviceId = auth.deviceId,
                acid = auth.acid
            )

        // Requesting actual in-app
        // placements from backend.
        val response = request(
            context, state, id?.toString()
        )

        // Stopping current pass if retry
        // is required and no newer request exists.
        if (response.event is retry) {
            return !hasNewRequest(
                context, tag, id
            )
        }

        // Processing successful backend response
        // and synchronizing local in-app data.
        response.response?.let {
            processPlacementsResponse(
                context, env, state, it, now
            )
        } ?: run {
            // Updating last request time
            // when response body is empty.
            inAppsDao.insert(
                state.copy(
                    lastRequestTime = now
                )
            )
        }

        // Marking placements as ready
        // for in-app processing.
        placementsReady()
        return false
    }

    /**
     * Saves and processes successful in-app placements response.
     *
     * The method:
     *
     * 1. Saves updated synchronization state and request timestamp.
     * 2. Saves received in-app campaigns to local database.
     * 3. Creates missing personal policy rows for received in-apps.
     * 4. Sends delivery events for in-apps whose delivery report
     *    has not been sent yet.
     * 5. Removes obsolete personal policies that are no longer present
     *    in backend response.
     * 6. Sends and optionally presents test in-app campaigns.
     *
     * @param context Application context.
     * @param env SDK environment.
     * @param state Current in-app placements state.
     * @param response Successful in-app placements response.
     * @param now Current request timestamp.
     */
    private suspend fun processPlacementsResponse(
        context: Context,
        env: Environment,
        state: InAppsEntity,
        response: ResponseWithInApps,
        now: Long
    ) {
        val eventType = InAppEventType.DELIVERY
        val inAppsDao = env.room.dao.inApps
        val inAppDao = env.room.dao.inApp
        val auth = env.internalAuth()

        // Saving updated in-app
        // synchronization state.
        inAppsDao.insert(
            response.toStateEntity(
                acid = state.acid,
                deviceId = state.deviceId,
                lastRequestTime = now
            )
        )

        // Mapping regular in-apps from response
        // and trimming to the configured maximum.
        val inApps = trimToMax(
            response.inApps,
            env.config().maxPlacements
        )

        // Saving regular in-apps to local database.
        inAppDao.saveInApps(inApps)

        // Saving missing personal policies to local database.
        inAppDao.savePersonalPolicies(
            state.acid, inApps
        )

        // Sending delivery events for
        // in-apps that have not been reported yet.
        inAppDao.getUnreportedInApps()
            .forEach {
                sendInAppEvent(
                    context,
                    eventType,
                    it.smid,
                    null
                )
            }

        // Removing personal policies for
        // in-apps that are no longer available.
        inAppDao.deletePersonalPoliciesNotIn(
            acid = auth.acid,
            smidList = inApps.map {
                it.entity.smid
            }
        )

        // Sending delivery events for test
        // in-apps and showing them in test mode.
        response.testInApps
            .forEach {
                sendInAppEvent(
                    context,
                    eventType,
                    it.smid,
                    null
                )

                if (auth.isTest) {
                    Presenter.present(
                        context, it
                    )
                }
            }
    }

    /**
     * Returns `true` when requesting placements is still too early.
     *
     * @param state Current in-app placements state.
     * @param intervalMs Configured interval between automatic requests, in milliseconds.
     */
    private fun isRequestTooEarly(state: InAppsEntity?, intervalMs: Long) =
        (time() - (state?.lastRequestTime ?: 0) < intervalMs)
            .also { if (it) placementsReady() }

    /**
     * Marks in-app placements as ready for trigger processing.
     */
    fun placementsReady() = placementsReady.complete(Unit)
}