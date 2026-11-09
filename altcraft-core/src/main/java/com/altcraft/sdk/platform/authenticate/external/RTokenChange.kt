package com.altcraft.sdk.platform.authenticate.external

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.core.modules.ModuleRegistry.notifyRTokenChanged
import com.altcraft.sdk.data.Preferences.getCurrentRToken
import com.altcraft.sdk.data.Preferences.setCurrentRToken
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProcessor.extractRToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handles role token changes.
 *
 * The SDK initialization dispatch additionally re-runs
 * [checkRTokenChangedFromInit] at the dispatch point and tracks delivered
 * notifications through [notificationCount]: a change detected by a JWT read
 * while the module initialization phase is already running completes with the
 * dispatch, and the phase is re-run once so its data processing happens
 * against the new identity (see [hasChangedSince]).
 */
internal object RTokenChange {

    private val mutex = Mutex()

    /**
     * Monotonic count of delivered role-token change notifications.
     *
     * Lets the SDK initialization dispatch detect a change that was
     * delivered while the module initialization phase was already running:
     * a snapshot read before the phase compared with the count after it
     * ([hasChangedSince]) reports whether the phase raced with a change
     * notification and must be re-run.
     */
    private val notificationCount = AtomicInteger(0)

    /**
     * Returns the number of delivered role-token change notifications.
     */
    fun notificationCount(): Int = notificationCount.get()

    /**
     * Returns whether at least one change notification completed since
     * [count] was read.
     *
     * Used by the SDK initialization dispatch to detect a role-token change
     * delivered while the module initialization phase was running.
     *
     * @param count Previously read [notificationCount] snapshot.
     * @return `true` if the count has grown since the snapshot.
     */
    fun hasChangedSince(count: Int): Boolean =
        notificationCount.get() > count

    /**
     * Checks for role token changes in the SDK configuration.
     *
     * @param context Application context.
     */
    suspend fun checkRTokenChangedFromInit(
        context: Context
    ) = checkRTokenChanged(
        context,
        Environment.create(context)
            .auth
            .external
            .rToken()
    )

    /**
     * Checks for role token changes in a JWT.
     *
     * @param context Application context.
     * @param jwt JWT token.
     */
    suspend fun checkRTokenChangedFromJWT(
        context: Context, jwt: String
    ) = checkRTokenChanged(
        context,
        extractRToken(
            jwt
        )
    )

    /**
     * Compares the role token with the previously saved one.
     *
     * Notifies the SDK modules about the change and saves the new token.
     * The first saved token does not trigger the notification.
     * A `null` token is ignored.
     *
     * @param context Application context.
     * @param newToken New role token.
     */
    private suspend fun checkRTokenChanged(
        context: Context,
        newToken: String?
    ) {
        mutex.withLock {
            if (newToken == null) return@withLock
            val oldToken = getCurrentRToken(context)
            if (oldToken == newToken) return@withLock
            if (oldToken != null) {
                notifyRTokenChanged(context)
                notificationCount.incrementAndGet()
            }
            setCurrentRToken(context, newToken)
        }
    }
}
