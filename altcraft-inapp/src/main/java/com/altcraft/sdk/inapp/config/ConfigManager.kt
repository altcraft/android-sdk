package com.altcraft.sdk.inapp.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_MAX_PLACEMENTS
import com.altcraft.sdk.inapp.data.Constants.DEFAULT_PLACEMENTS_REQ_INTERVAL_MS
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.placements.PlacementCountManager.checkMaxPlacementChange
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages persistence of the in-app module configuration.
 */
internal object ConfigManager {

    /**
     * Synchronizes configuration read and write operations.
     */
    private val mutex = Mutex()

    /**
     * Cached in-app module configuration.
     */
    @Volatile
    private var conf: InAppConfigEntity? = null

    /**
     * Default in-app module configuration.
     */
    private val default = InAppConfigEntity(
        inAppAutoRequest = true,
        placementsRequestInterval = DEFAULT_PLACEMENTS_REQ_INTERVAL_MS,
        maxPlacements = DEFAULT_MAX_PLACEMENTS
    )

    /**
     * Clears the cached in-app configuration.
     */
    fun clearCache() {
        conf = null
    }

    /**
     * Validates the in-app configuration.
     *
     * When the configuration is invalid, it is removed from the local
     * database, the in-memory cache is cleared, and an [SDKException]
     * is thrown using the in-app `invalidConfig` event.
     *
     * @param context Application context.
     * @param config In-app configuration to validate.
     */
    suspend fun validateConfig(context: Context, config: InAppConfiguration) {
        if (config.valid) return else Environment.create(context).room.dao.config.delete()
        clearCache()
        config.error()
    }

    /**
     * Saves the in-app configuration.
     *
     * @param context Application context.
     * @param config In-app configuration to save.
     */
    suspend fun setConfig(context: Context, config: InAppConfiguration) = mutex.withLock {
        Environment.create(context).room.dao.config.insert(config.entity.also {
            conf = it; checkMaxPlacementChange(context, config)
        })
    }

    /**
     * Returns the stored in-app configuration or the default configuration
     * if no configuration is stored.
     *
     * @param context Application context.
     * @return Stored or default in-app configuration.
     */
    suspend fun getConfig(context: Context): InAppConfigEntity = mutex.withLock {
        conf ?: (Environment.create(context).room.dao.config.get() ?: default).also {
            conf = it
        }
    }
}