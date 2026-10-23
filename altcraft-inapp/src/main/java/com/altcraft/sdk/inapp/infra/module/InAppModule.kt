package com.altcraft.sdk.inapp.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.inapp.config.ConfigManager.setConfig
import com.altcraft.sdk.inapp.config.ConfigManager.validateConfig
import com.altcraft.sdk.inapp.config.InAppConfiguration
import com.altcraft.sdk.inapp.data.Constants.MODULE_NAME

/**
 * Lifecycle implementation of the optional in-app module.
 *
 * The module is automatically registered in Altcraft SDK core by
 * [ModuleInitializer] when the in-app dependency is present.
 */
internal object InAppModule : AltcraftModule {

    /**
     * Unique in-app module identifier.
     */
    override val moduleId: String = MODULE_NAME

    /**
     * Validates the in-app module configuration.
     *
     * Removes the stored configuration, clears the in-memory cache, and
     * throws an SDKException when the configuration is invalid.
     *
     * @param context Application context.
     * @param configuration Configuration of the in-app module.
     */
    override suspend fun validate(
        context: Context,
        configuration: ModuleConfiguration
    ) = validateConfig(
        context, configuration as InAppConfiguration
    )

    /**
     * Persists the in-app module configuration.
     *
     * @param context Application context.
     * @param configuration Configuration of the in-app module.
     */
    override suspend fun configure(
        context: Context,
        configuration: ModuleConfiguration
    ) = setConfig(
        context, configuration as InAppConfiguration
    )

    /**
     * Starts in-app module operations after successful core initialization.
     *
     * @param context Application context.
     */
    override suspend fun onSdkInitialized(context: Context) {
        ModuleLifecycle.onSdkInitialized(
            context
        )
    }

    /**
     * Stops in-app module work and clears in-app module data.
     *
     * @param context Application context.
     */
    override suspend fun onSdkClear(context: Context) {
        ModuleLifecycle.onSdkClear(
            context
        )
    }

    /**
     * Handles an SDK RToken change.
     *
     * @param context Application context.
     */
    override suspend fun onSdkRTokenChanged(context: Context) {
        ModuleLifecycle.onSdkRTokenChanged(
            context
        )
    }
}
