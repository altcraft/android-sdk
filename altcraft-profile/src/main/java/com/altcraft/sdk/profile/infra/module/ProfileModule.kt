package com.altcraft.sdk.profile.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.modules.AltcraftModule
import com.altcraft.sdk.profile.data.Constants.MODULE_NAME

/**
 * Lifecycle implementation of the optional profile module.
 *
 * The module is automatically registered in Altcraft SDK core by
 * [ModuleInitializer] when the profile dependency is present.
 */
internal object ProfileModule : AltcraftModule {

    /**
     * Unique profile module identifier.
     */
    override val moduleId: String = MODULE_NAME

    /**
     * Starts profile-module operations after successful core initialization.
     *
     * @param context Application context.
     */
    override suspend fun onSdkInitialized(context: Context) {
        ModuleLifecycle.onSdkInitialized(
            context
        )
    }

    /**
     * Stops profile-module work and clears profile-module data.
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
