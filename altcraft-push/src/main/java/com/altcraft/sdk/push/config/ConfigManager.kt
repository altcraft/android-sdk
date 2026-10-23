package com.altcraft.sdk.push.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages persistence of the Push module configuration.
 */
internal object ConfigManager {

    /**
     * Synchronizes configuration read and write operations.
     */
    private val mutex = Mutex()

    /**
     * Cached Push module configuration.
     */
    @Volatile
    private var conf: PushConfigEntity? = null

    /**
     * Clears the cached Push configuration.
     *
     * Synchronized with [setConfig] and [getConfig] so a concurrent read or
     * write cannot cache a configuration that is being wiped.
     */
    suspend fun clearConfigCache() = mutex.withLock {
        conf = null
    }

    /**
     * Validates the Push configuration.
     *
     * @param context Application context.
     * @param config Push configuration to validate.
     */
    suspend fun validateConfig(context: Context, config: PushConfiguration) {
        if (config.valid) return else Environment.create(context).room.dao.config.delete()
        clearConfigCache()
        config.error()
    }

    /**
     * Saves the Push configuration.
     *
     * @param context Application context.
     * @param config Push configuration to save.
     */
    suspend fun setConfig(context: Context, config: PushConfiguration) = mutex.withLock {
        Environment.create(context).room.dao.config.insert(config.entity.also {
            conf = it
        })
    }

    /**
     * Returns the stored Push configuration.
     *
     * @param context Application context.
     * @return Stored configuration, or `null` if it does not exist.
     */
    suspend fun getConfig(context: Context) = mutex.withLock {
        conf ?: Environment.create(context).room.dao.config.get()?.also {
            conf = it
        }
    }
}