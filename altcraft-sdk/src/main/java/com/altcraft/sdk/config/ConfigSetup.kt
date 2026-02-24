package com.altcraft.sdk.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.sdk_events.EventList.configIsSet
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.Events.event
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** Manages SDK configuration setup and access. */
internal object ConfigSetup {

    internal var configuration: ConfigurationEntity? = null
    private val configMutex = Mutex()

    /**
     * Saves SDK configuration to the database and updates the cache.
     *
     * Converts [AltcraftConfiguration] to an entity and inserts or updates it.
     *
     * @return The stored [ConfigurationEntity], or `null` if invalid or on error.
     */
    suspend fun setConfig(
        context: Context,
        newConfig: AltcraftConfiguration
    ): ConfigurationEntity? {
        return try {
            configuration = null
            val dao = SDKdb.getDb(context).request()
            (newConfig.toEntity() to dao.getConfig()).let {
                rTokenChange(it.first, it.second, dao)
                configInstall(it.first, it.second, dao)
            }
        } catch (e: Exception) {
            error("setConfig", e)
            null
        }
    }

    /**
     * When `rToken` changes, deletes:
     * - all subscriptions
     * - all mobile events
     * - all profile field updates
     *
     * @param newConfig The new configuration containing the current rToken.
     * @param oldConfig The previous configuration containing the old rToken.
     * @param dao Data access object used to clear stored data.
     */
    private suspend fun rTokenChange(
        newConfig: ConfigurationEntity?,
        oldConfig: ConfigurationEntity?,
        dao: DAO
    ) {
        val (newRToken, oldRToken) = newConfig?.rToken to oldConfig?.rToken
        if (newRToken != null && oldRToken != null && newRToken != oldRToken) {
            dao.deleteAllProfileUpdates()
            dao.deleteAllSubscriptions()
            dao.deleteAllMobileEvents()
        }
    }

    /**
     * Updates stored configuration if it has changed and sets the active configuration.
     *
     * Emits a "setConfig" event after successful update.
     *
     * @param newConfig The new configuration to store and activate.
     * @param oldConfig The previously stored configuration.
     * @param dao Data access object used to persist configuration.
     * @return The active configuration, or `null` if [newConfig] is `null`.
     */
    private suspend fun configInstall(
        newConfig: ConfigurationEntity?,
        oldConfig: ConfigurationEntity?,
        dao: DAO
    ): ConfigurationEntity? {
        if (newConfig == null) return null
        if (newConfig != oldConfig) dao.insertConfig(newConfig)
        configuration = newConfig
        return configuration.also { event("setConfig", configIsSet) }
    }

    /**
     * Retrieves configuration with retry logic (up to 3 attempts, 1 second delay).
     *
     * Uses the in-memory cache if available. Otherwise fetches from the database.
     * If the app is in the background, returns the database value immediately.
     * If the app is in the foreground, waits and retries (to allow config to be set).
     *
     * @param context The application context for database access.
     * @return The configuration, or `null` if not found after retries.
     */
    suspend fun getConfig(context: Context): ConfigurationEntity? {
        return configMutex.withLock {
            repeat(3) {
                try {
                    if (configuration != null) return configuration

                    SDKdb.getDb(context).request().getConfig()?.let {
                        if (!isAppInForegrounded()) return it
                    }

                    delay(1000)
                } catch (_: CancellationException) {
                } catch (e: Exception) {
                    error("getConfig", e)
                }
            }
            null
        }
    }

    /**
     * Refreshes the in-memory configuration value from the database.
     *
     * @param context Application context used to access the database.
     */
    suspend fun updateConfigCache(context: Context) {
        configuration = SDKdb.getDb(context).request().getConfig()
    }
}