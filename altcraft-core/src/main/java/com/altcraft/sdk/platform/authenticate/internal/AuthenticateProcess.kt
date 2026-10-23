package com.altcraft.sdk.platform.authenticate.internal

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.SubFunction.nano12
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.util.SubFunction.uuid
import com.altcraft.sdk.coordination.CommandQueue.SerialCommandQueue
import com.altcraft.sdk.coordination.submitSdkCommand
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.Constants.AUTH_QUEUE
import com.altcraft.sdk.data.Constants.AUTH_REQUEST_O_WORK_TAG
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.data.room.RoomRequest.deleteAcidSensitiveRequests
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log.event
import com.altcraft.sdk.util.log.Log.retry
import com.altcraft.sdk.platform.authenticate.internal.request.AuthenticateRequest.authenticateRequest
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.startWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages SDK authentication state and user identity switching.
 *
 * Handles:
 * - anonymous auth initialization;
 * - user authentication requests;
 * - pending auth retry processing;
 * - logout and anonymous session restore;
 * - current authentication state checks.
 */
internal object AuthenticateProcess {

    /** Processes SDK commands sequentially within this queue. */
    private val queue = SerialCommandQueue(AUTH_QUEUE)

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = "authenticate"

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Reference to the function executing the current API request. */
    private val request = ::authenticateRequest

    /**
     * Authenticates the Altcraft user and switches the session
     * from anonymous to identified.
     *
     * @param context The application context.
     */
    fun authenticate(context: Context) {
        submitSdkCommand(
            context, FUNCTION, mutex, queue, null
        ) {
            val env = Environment.create(
                context
            )
            val userTag = env.auth.external
                .user()

            if (env.auth.internal().currentUser
                == userTag
            ) return@submitSdkCommand

            val dao = env.room.dao.authRequest
            if (userTag != dao.get()?.user) {
                dao.insert(
                    AuthRequestEntity(
                        0, uuid(), userTag
                    )
                )
            }
            startWorker<AuthenticateWorker>(
                context, AUTH_REQUEST_O_WORK_TAG
            )
        }
    }

    /**
     * Checks whether authentication should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
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
     * Processes the current pending auth request.
     *
     * Deletes the request on success, retries on recoverable failure
     * retries the request when possible, or clears it when the retry limit is reached.
     *
     * @param context Application context.
     * @param id Optional worker identifier.
     * @return `true` if another retry is needed; `false` otherwise.
     */
    private suspend fun logic(
        context: Context, id: UUID?
    ): Boolean {
        val tag = AUTH_REQUEST_O_WORK_TAG
        val env = Environment.create(context)
        val reqDao = env.room.dao.authRequest
        val stateDao = env.room.dao.authState
        val req = reqDao.get() ?: return false
        if (env.auth.external.user() != req.user) {
            reqDao.deleteAll()
            return false
        }

        val res = request(context, req)

        if (res.event is retry) return !req.retryLimit(
            reqDao
        ) && !hasNewRequest(context, tag, id) else {
            reqDao.deleteItem(req)
        }

        val acid = res.response?.acid ?: return false

        stateDao.updateCurrentAcid(acid)
        stateDao.updateCurrentTag(req.user)
        stateDao.updateIsTest(res.response.isTest)
        if (env.auth.internal().anonimAcid == acid)
            stateDao.updateAnonAcid(
                nano12()
            )
        return false
    }

    /**
     * Initializes internal authentication state if it does not exist yet.
     *
     * Creates an anonymous auth record with generated ACID and device ID.
     *
     * @param context Application context.
     */
    internal suspend fun setAuth(context: Context) {
        mutex.withLock {
            val env = Environment.create(context)
            val dao = env.room.dao.authState
            if (dao.get() != null) return@withLock

            val acid = nano12()

            dao.insert(
                AuthenticationEntity(
                    0, acid, acid, uuid(), null, time()
                )
            )
        }
    }

    /**
     * Restores anonymous authentication state for the current device.
     *
     * Keeps the stored anonymous ACID and device ID, clears the current user tag,
     * and starts a new anonymous session.
     *
     * @param context Application context.
     */
    suspend fun logOut(context: Context) {
        mutex.withLock {
            val env = Environment.create(context)
            val dao = env.room.dao.authState
            if (dao.get() == null) return@withLock

            event("logOut", CoreEvents.userLogOut)
            deleteAcidSensitiveRequests(env.room)

            val internalAuth = env.auth.internal()

            dao.insert(
                AuthenticationEntity(
                    0,
                    internalAuth.anonimAcid,
                    internalAuth.anonimAcid,
                    internalAuth.deviceId,
                    null,
                    time()
                )
            )
        }
    }

    /**
     * Checks whether the current SDK user is authenticated.
     *
     * @param env The SDK environment providing internal auth data and current user tag.
     */
    internal suspend fun isAuthenticated(env: Environment) = withContext(Dispatchers.IO) {
        mutex.withLock {
            env.auth.internal().currentUser.let { it != null && it == env.auth.external.user() }
        }
    }
}
