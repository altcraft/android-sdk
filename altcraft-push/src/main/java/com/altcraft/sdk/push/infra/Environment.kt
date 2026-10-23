package com.altcraft.sdk.push.infra

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.SuspendLazy
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.config.ConfigManager.getConfig
import com.altcraft.sdk.push.data.Preferenses.getManualToken
import com.altcraft.sdk.push.data.Preferenses.getSavedPushToken
import com.altcraft.sdk.push.data.room.DB
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.token.TokenManager.getCurrentPushToken

/**
 * Provides dependencies required by the Push module.
 *
 * Single source of truth for module state: Core configuration, Push
 * configuration, user tag, Room database, current push token, saved push
 * token, and manual push token.
 */
internal class Environment private constructor(
    private val appContext: Context
) {

    /** Push module Room database. */
    val room: DB by lazy {
        DB.getInstance(appContext)
    }

    /**
     * Returns the current user tag.
     *
     * @return Current user tag.
     */
    suspend fun userTag() = CoreEnvironmentProvider.user()

    /**
     * Returns whether rToken-based authentication is in use.
     *
     * @return `true` if the SDK configuration defines an RToken.
     */
    suspend fun usesRTokenAuth() = CoreEnvironmentProvider.usesRToken()

    /**
     * Returns the current Push module configuration.
     *
     * @return Current Push configuration, or `null` if it is not set.
     */
    suspend fun pushConfig() = getConfig(appContext) ?: exception(
        ModuleEvents.configIsNull
    )

    /**
     * Returns the manually assigned push token.
     *
     * The value is loaded once per [Environment] instance.
     *
     * @return Manual push token, or `null` if none exists.
     */
    suspend fun manualToken() = manualTokenLazy.get()

    /** Lazily loaded manual push token. */
    private val manualTokenLazy = SuspendLazy {
        getManualToken(appContext)
    }

    /**
     * Returns the previously saved push token.
     *
     * The value is loaded once per [Environment] instance.
     *
     * @return Saved push token, or `null` if none exists.
     */
    fun savedToken() = savedTokenLazy.value

    /** Lazily loaded saved push token. */
    private val savedTokenLazy = lazy {
        getSavedPushToken(appContext)
    }

    /**
     * Returns the current push token.
     *
     * The token is resolved from the available push providers or a manually
     * assigned token. The value is loaded once per [Environment] instance.
     *
     * @return Current push token, or `null` if unavailable.
     */
    suspend fun currentToken() = currentTokenLazy.get() ?: exception(
        ModuleEvents.pushTokenIsNull
    )

    /** Lazily resolved current push token. */
    private val currentTokenLazy = SuspendLazy {
        getCurrentPushToken(appContext)
    }

    companion object {

        /**
         * Creates a new Push module environment.
         *
         * @param context Android context.
         * @return Push module environment.
         */
        fun create(context: Context) = Environment(
            context.applicationContext
        )
    }
}