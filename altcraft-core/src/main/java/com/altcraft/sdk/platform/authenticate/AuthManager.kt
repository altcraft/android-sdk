package com.altcraft.sdk.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.platform.authenticate.external.ExternalAuth
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Provides lazy access to external and internal authentication data.
 *
 * External and internal authentication data are loaded independently
 * only when requested. Neither access blocks the calling thread.
 *
 * @param configReader Loads the current SDK configuration.
 * @param internalReader Loads the current internal authentication state.
 */
internal class AuthManager private constructor(
    private val configReader: suspend () -> ConfigurationEntity,
    private val internalReader: suspend () -> AuthenticationEntity?,
) {

    /** External authentication data provider. */
    val external = ExternalAuth(
        configReader = configReader,
        jwtReader = {
            withContext(Dispatchers.IO) {
                JWTProvider.getJWT()
            }
        }
    )

    /** Guards one-time loading of the internal authentication state. */
    private val internalMutex = Mutex()

    /** Cached internal authentication snapshot for this instance. */
    private var cachedInternal: AuthenticationEntity? = null

    /**
     * Internal authentication state loaded on first access.
     *
     * Suspends the calling coroutine while reading from the database;
     * on subsequent accesses the loaded snapshot is returned immediately.
     * The calling thread is never blocked.
     */
    suspend fun internal(): AuthenticationEntity = internalMutex.withLock {
        cachedInternal ?: internalReader()?.also { cachedInternal = it }
            ?: exception(
                CoreEvents.internalAuthIsNull
            )
    }

    companion object {

        /**
         * Creates a new [AuthManager] instance.
         *
         * @param configReader Loads the current SDK configuration.
         * @param internalReader Loads the current internal authentication state.
         * @return A new [AuthManager] instance.
         */
        fun create(
            configReader: suspend () -> ConfigurationEntity,
            internalReader: suspend () -> AuthenticationEntity?,
        ) = AuthManager(
            configReader = configReader,
            internalReader = internalReader
        )
    }
}