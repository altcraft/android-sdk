package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.auth.AuthManager.getAuthHeaderAndMatching
import com.altcraft.sdk.auth.AuthManager.getUserTag
import com.altcraft.sdk.coordination.SuspendLazy
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.Preferenses.getSavedPushToken
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.push.token.TokenManager.getCurrentPushToken
import com.altcraft.sdk.sdk_events.EventList.authDataIsNull
import com.altcraft.sdk.sdk_events.EventList.configIsNull
import com.altcraft.sdk.sdk_events.EventList.pushTokenIsNull
import com.altcraft.sdk.sdk_events.EventList.userTagIsNull

/**
 * Provides lazily initialized environment for SDK operations:
 * Room database, config, user tag, auth data, and push token.
 *
 * Suspend accessors throw if required data is missing.
 */
internal class Environment private constructor(
    private val appContext: Context
) {
    /** Room instance. */
    val room by lazy { SDKdb.getDb(appContext) }

    /** Token saved in preferences (cached once per Environment). */
    val savedToken by lazy { getSavedPushToken(appContext) }

    private val configLazy = SuspendLazy(this::loadConfig)
    private val tokenLazy = SuspendLazy(this::loadToken)
    private val authLazy = SuspendLazy(this::loadAuth)
    private val tagLazy = SuspendLazy(this::loadTag)

    /** Auth header and matching mode; throws if auth data is missing. */
    suspend fun auth() = authLazy.get() ?: exception(authDataIsNull)

    /** User tag; depends on config; throws if tag is missing. */
    suspend fun userTag() = tagLazy.get() ?: exception(userTagIsNull)

    /** Config; throws if config is not set. */
    suspend fun config() = configLazy.get() ?: exception(configIsNull)

    /** Current push token; throws if token is missing. */
    suspend fun token() = tokenLazy.get() ?: exception(pushTokenIsNull)

    /** Loads config; cached by SuspendLazy */
    private suspend fun loadConfig() = getConfig(appContext)

    /** Loads user tag using rToken; cached by SuspendLazy. */
    private suspend fun loadTag() = getUserTag(config().rToken)

    /** Loads current push token; cached by SuspendLazy. */
    private suspend fun loadToken() = getCurrentPushToken(appContext)

    /** Builds auth header and matching mode; cached by SuspendLazy. */
    private suspend fun loadAuth() = getAuthHeaderAndMatching(config())

    companion object {
        fun create(context: Context) = Environment(context.applicationContext)
    }
}