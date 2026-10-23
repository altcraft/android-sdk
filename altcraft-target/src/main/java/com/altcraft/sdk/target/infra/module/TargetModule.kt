package com.altcraft.sdk.target.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.target.data.Constants.MODULE_NAME

/**
 * Lifecycle implementation of the optional target module.
 *
 * The module is automatically registered in Altcraft SDK core by
 * [ModuleInitializer] when the target dependency is present.
 */
internal object TargetModule : AltcraftModule {

    /**
     * Unique target module identifier.
     */
    override val moduleId: String = MODULE_NAME

    /**
     * Starts target-module operations after successful core initialization.
     *
     * @param context Application context.
     */
    override suspend fun onSdkInitialized(context: Context) {
        ModuleLifecycle.onSdkInitialized(
            context
        )
    }

    /**
     * Stops target-module work and clears target-module data.
     *
     * @param context Application context.
     */
    override suspend fun onSdkClear(context: Context) {
        ModuleLifecycle.onSdkClear(
            context
        )
    }

    /**
     * Called when the SDK RToken changes.
     *
     * @param context Application context.
     */
    override suspend fun onSdkRTokenChanged(context: Context) {
        ModuleLifecycle.onSdkRTokenChanged(
            context
        )
    }
}
