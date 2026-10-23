package com.altcraft.sdk.push.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.push.config.ConfigManager.setConfig
import com.altcraft.sdk.push.config.ConfigManager.validateConfig
import com.altcraft.sdk.push.config.PushConfiguration
import com.altcraft.sdk.push.data.Constants.MODULE_NAME

/**
 * Lifecycle implementation of the optional push module.
 *
 * The module is automatically registered in Altcraft SDK core by
 * [ModuleInitializer] when the push dependency is present.
 */
internal object PushModule : AltcraftModule {

    /**
     * Unique push module identifier.
     */
    override val moduleId: String = MODULE_NAME

    /**
     * Validates the Push module configuration.
     *
     * @param context Application context.
     * @param configuration Configuration of the Push module.
     */
    override suspend fun validate(
        context: Context,
        configuration: ModuleConfiguration
    ) = validateConfig(
        context, configuration as PushConfiguration
    )

    /**
     * Persists the push module configuration.
     *
     * @param context Application context.
     * @param configuration Configuration of the push module.
     */
    override suspend fun configure(
        context: Context,
        configuration: ModuleConfiguration
    ) = setConfig(
        context, configuration as PushConfiguration
    )

    /**
     * Starts push-module operations after successful core initialization.
     *
     * @param context Application context.
     */
    override suspend fun onSdkInitialized(context: Context) {
        ModuleLifecycle.onSdkInitialized(
            context
        )
    }

    /**
     * Stops push-module work and clears push-module data.
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
