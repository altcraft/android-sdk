package com.altcraft.sdk.inapp.platform.placements

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_O_WORK_TAG
import com.altcraft.sdk.inapp.data.Constants.IN_APP_PLACEMENTS_QUEUE
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel.ResponseWithInApps
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
     * Waiters released by [reset] re-park on the newly installed gate
     * and proceed only when fresh placement data is saved.
     */
    @Volatile
    var placementsReady = CompletableDeferred<Unit>()
        private set

    /**
     * Rearms the placements readiness gate.
     *
     * The previous gate is completed to release parked workers, and a new
     * gate is installed so released workers and subsequent triggers wait
     * for the next placements request before acting on wiped data.
     */
    fun reset() {
        val previous = placementsReady
        if (!previous.isCompleted) previous.complete(Unit)
        placementsReady = CompletableDeferred()
    }

    /**
     * Checks whether an in-app placements request should be performed.
     *
     * Automatic requests are skipped when `inAppAutoRequest` is disabled.
     * Manual requests (`now = true`) bypass this restriction.
     *
     * Clears stored in-app data when the current authentication changes.
     * Otherwise, checks the configured request interval unless the request
     * is manual, test mode is active, or the maximum placements has changed.
     *
     * @param env SDK environment.
     * @param now Whether the request should bypass automatic restrictions.
     * @return `true` if placements should be requested; `false` otherwise.
     */
    private suspend fun shouldRequestPlacements(
        env: Environment,
        now: Boolean
    ): Boolean {
        if (!env.config().inAppAutoRequest
            && !now
        ) {
            placementsReady()
            return false
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
            !now && !auth.isTest
            && !maxPlacementsChanged.get()
            && isRequestTooEarly(
                state,
                env.config()
                    .placementsRequestInterval
            )
        ) return false

        maxPlacementsChanged.set(false)
        return true
    }

    /**
     * Checks in-app placements request conditions and starts background processing.
     *
     * Used by the public API and module lifecycle. Manual requests
     * (`now = true`) bypass automatic request restrictions.
     *
     * @param context Application context.
     */
    fun requestPlacements(
        context: Context,
        now: Boolean = false
    ) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, null
        ) {
            val env = Environment.create(context)

            if (!shouldRequestPlacements(env, now))
                return@submitSdkCommand

            startWorker<PlacementsWorker>(
                context, IN_APP_PLACEMENTS_O_WORK_TAG
            )
        }
    }

    /**
     * Requests in-app placements directly from WorkManager.
     *
     * Waits for SDK initialization and checks request conditions
     * before executing processing without starting another worker.
     *
     * @param context Application context.
     */
    suspend fun requestPlacementsSuspend(
        context: Context
    ) {
        val initGate = InitBarrier.current()
        initGate.await()

        mutex.withLock {
            val env = Environment.create(context)
            if (!shouldRequestPlacements(
                    env, false
                )
            ) return@withLock

            logic(context, null)
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
     * 4. Discards the response when the current user identity does not
     *    match the identity the request was made with.
     * 5. Saves and processes received in-app placements response.
     * 6. Updates request timestamp when response body is empty.
     * 7. Marks placements as ready for trigger processing.
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

        // Discarding the response when the user identity
        // changed while the request was in flight: saving
        // it would resurrect data of the previous user.
        val currentAuth = env.internalAuth()
        if (state.acid != currentAuth.acid ||
            state.deviceId != currentAuth.deviceId
        ) return false

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
