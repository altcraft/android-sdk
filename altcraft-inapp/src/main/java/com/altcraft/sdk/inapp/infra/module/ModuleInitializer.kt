package com.altcraft.sdk.inapp.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.inapp.system.lifecycle.ActivityTracker

/**
 * Automatically registers the in-app module in Altcraft SDK core
 * and initializes activity lifecycle tracking.
 *
 * The module is registered during application startup and initialized
 * after the core SDK is initialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ModuleInitializer : Initializer<Unit> {

    /**
     * Registers the in-app module in the core module registry
     * and starts activity lifecycle tracking.
     *
     * @param context Application context supplied by AndroidX Startup.
     */
    override fun create(context: Context) {
        ModuleRegistry.register(InAppModule)

        ActivityTracker.onActivityLifecycle(
            context.applicationContext
        )
    }

    /**
     * Returns AndroidX Startup initializer dependencies.
     *
     * In-app module registration does not depend on other initializers.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}