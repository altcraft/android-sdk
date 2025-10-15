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
     * Sets up and updates the SDK configuration in the local database.
     *
     * Converts the provided [AltcraftConfiguration] into a database entity, validates it,
     * and checks if the `userTag` is associated with the `rToken`.
     * Updates an existing configuration or inserts a new one.
     *
     * @param context The application context for database access.
     * @param newConfig The [AltcraftConfiguration] instance with new SDK settings.
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
     * Clears pending push subscription requests if the rToken has changed.
     *
     * Used to prevent retrying outdated subscription requests tied to a previous rToken.
     *
     * @param newConfig The new configuration with the current rToken.
     * @param oldConfig The previous configuration with the old rToken.
     * @param dao Data access object used to clear pending subscription requests.
     */
    private suspend fun rTokenChange(
        newConfig: ConfigurationEntity?,
        oldConfig: ConfigurationEntity?,
        dao: DAO
    ) {
        val (newRToken, oldRToken) = newConfig?.rToken to oldConfig?.rToken
        if (newRToken != null && oldRToken != null && newRToken != oldRToken) {
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
     * @return true if the new configuration was successfully set, false otherwise.
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
     * Retrieves configuration data with retry logic.
     *
     * Fetches config data from the database, checking `init == true` if the app is in the
     * foreground, or without checking if in the background.
     * Retries up to 3 times with 1-second delays between attempts.
     *
     * @param context The application context for database access.
     * @return The configuration data, or `null` if not found after retries.
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
     * Updates the cached configuration instance after changing the provider priority list.
     *
     * Used to refresh the in-memory `configuration` value from the database
     * after updating the `providerPriorityList` in `ConfigurationEntity`.
     *
     * @param context Application context used to access the database.
     */
    suspend fun updateConfigCache(context: Context) {
        configuration = SDKdb.getDb(context).request().getConfig()
    }
}