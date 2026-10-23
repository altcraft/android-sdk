package com.altcraft.sdk.platform.authenticate.external

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.PUSH_SUB_MATCHING
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProcessor
import com.altcraft.sdk.util.StringBuilder.bearerJwtToken
import com.altcraft.sdk.util.StringBuilder.bearerRToken
import com.altcraft.sdk.util.SuspendLazy

/**
 * Provides lazy access to external authentication data.
 *
 * Authentication values are loaded independently on first access
 * and cached for the lifetime of this instance.
 *
 * @param configReader Loads the current SDK configuration.
 * @param jwtReader Loads the current JWT token.
 */
internal class ExternalAuth(
    private val configReader: suspend () -> ConfigurationEntity,
    private val jwtReader: suspend () -> String?,
) {

    /** User value loaded on first access. */
    private val userLazy = SuspendLazy {
        val config = configReader()

        config.rToken ?: JWTProcessor.createJWTHash(
            jwtReader()
        )
    }

    /** Authorization header loaded on first access. */
    private val headerLazy = SuspendLazy {
        val config = configReader()

        config.rToken?.let {
            bearerRToken(it)
        } ?: jwtReader()?.let {
            bearerJwtToken(it)
        }
    }

    /** Matching value loaded on first access. */
    private val matchingLazy = SuspendLazy {
        val config = configReader()

        if (config.rToken != null) {
            PUSH_SUB_MATCHING
        } else {
            JWTProcessor.getJWTPayload(
                jwtReader()
            )?.matching
        }
    }

    /**
     * Returns the current user.
     *
     * @throws com.altcraft.sdk.events.SDKException.SDKException if unavailable.
     *
     * @return Current user.
     */
    suspend fun user(): String = userLazy.get() ?: exception(
        CoreEvents.userIsNull
    )

    /**
     * Returns the authorization header.
     *
     * @throws com.altcraft.sdk.events.SDKException.SDKException if unavailable.
     *
     * @return Authorization header.
     */
    suspend fun header(): String = headerLazy.get() ?: exception(
        CoreEvents.authHeaderIsNull
    )

    /**
     * Returns the matching mode.
     *
     * @throws com.altcraft.sdk.events.SDKException.SDKException if unavailable.
     *
     * @return Matching mode.
     */
    suspend fun matching(): String = matchingLazy.get() ?: exception(
        CoreEvents.matchingIsNull
    )
}