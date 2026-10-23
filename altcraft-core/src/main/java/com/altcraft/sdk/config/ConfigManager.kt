package com.altcraft.sdk.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Manages SDK configuration setup and access. */
internal object ConfigManager {

    /**
     * Synchronizes configuration read and write operations.
     */
    private val mutex = Mutex()

    /**
     * Cached SDK configuration.
     */
    @Volatile
    internal var conf: ConfigurationEntity? = null

    /**
     * Clears the cached SDK configuration.
     *
     * Synchronized with [setConfig] and [getConfig] so a concurrent read or
     * write cannot cache a configuration that is being wiped.
     */
    suspend fun clearConfigCache() = mutex.withLock {
        conf = null
    }

    /**
     * Validates the SDK configuration.
     *
     * @param context Application context.
     * @param config SDK configuration to validate.
     */
    suspend fun validateConfig(context: Context, config: AltcraftConfiguration) {
        if (config.valid) return else Environment.create(context).room.dao.config.delete()
        clearConfigCache()
        config.error()
    }

    /**
     * Saves the Altcraft configuration.
     *
     * @param context Application context.
     * @param config Push configuration to save.
     */
    suspend fun setConfig(context: Context, config: AltcraftConfiguration) = mutex.withLock {
        Environment.create(context).room.dao.config.insert((config.toEntity()).also {
            conf = it
        })
    }

    /**
     * Returns the stored configuration.
     *
     * Uses the in-memory cache if available. Otherwise, fetches from the database
     * and caches the result.
     *
     * @param context The application context for database access.
     * @return The configuration, or `null` if it does not exist.
     */
    suspend fun getConfig(context: Context): ConfigurationEntity? = mutex.withLock {
        conf ?: Environment.create(context).room.dao.config.get()?.also {
            conf = it
        }
    }
}
