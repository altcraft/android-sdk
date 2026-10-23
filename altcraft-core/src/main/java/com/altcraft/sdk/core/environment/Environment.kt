package com.altcraft.sdk.core.environment

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.device.DeviceInfo.getDeviceFields
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.platform.authenticate.AuthManager

/**
 * Provides environment data required for SDK operations.
 *
 * Holds:
 * - database;
 * - configuration;
 * - authentication data;
 * - device fields;
 * - network state.
 */
internal class Environment private constructor(
    private val appContext: Context
) {

    /** Room instance. */
    val room by lazy {
        DB.getDb(appContext)
    }

    /** Authentication data provider. */
    val auth: AuthManager by lazy {
        AuthManager.create(
            configReader = {
                config()
            },
            internalReader = {
                room.dao.authState.get()
            }
        )
    }

    /** Device fields cached for this environment instance. */
    val deviceFields: Map<String, Any> by lazy {
        getDeviceFields()
    }

    /**
     * Returns the current SDK configuration.
     *
     * @return Current SDK configuration.
     */
    suspend fun config() = ConfigManager.getConfig(
        appContext
    ) ?: exception(
        CoreEvents.configIsNull
    )

    companion object {

        /**
         * Creates a new environment using the application context.
         *
         * @param context Context used to create the environment.
         */
        fun create(context: Context) = Environment(
            context.applicationContext
        )
    }
}