package com.altcraft.sdk.inapp.infra

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.inapp.config.ConfigManager.getConfig
import com.altcraft.sdk.inapp.data.room.DB

/**
 * Provides dependencies required by the In-App module.
 *
 * Shared SDK data is requested through [CoreEnvironmentProvider].
 */
internal class Environment private constructor(
    private val appContext: Context
) {

    /** In-app module Room database. */
    val room: DB by lazy {
        DB.getInstance(appContext)
    }

    /**
     * Returns the stored In-App module configuration.
     *
     * @return Stored In-App configuration, or `null` if not available.
     */
    suspend fun config() = getConfig(appContext)

    /**
     * Returns the current application version from the SDK configuration.
     *
     * @return Current application version, or `null` if it is not set.
     */
    suspend fun appVersion() = CoreEnvironmentProvider.appVersion()

    /**
     * Returns the internal authentication state.
     *
     * @return Current internal authentication entity.
     */
    suspend fun internalAuth() = CoreEnvironmentProvider.internalAuth()

    /**
     * Returns the current user tag.
     *
     * @return Current user tag.
     */
    suspend fun userTag() = CoreEnvironmentProvider.user()


    companion object {

        /**
         * Creates an In-App module environment.
         *
         * @param context Android context.
         * @return In-App module environment.
         */
        fun create(context: Context) = Environment(
            context.applicationContext
        )
    }
}
