package com.altcraft.sdk.profile.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import com.altcraft.sdk.core.modules.ModuleRegistry

/**
 * Automatically registers the profile module in Altcraft SDK core.
 *
 * The module is registered during application startup and initialized
 * after the core SDK is initialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ModuleInitializer : Initializer<Unit> {

    /**
     * Registers the profile module in the core module registry.
     *
     * @param context Application context supplied by AndroidX Startup.
     */
    override fun create(context: Context) {
        ModuleRegistry.register(ProfileModule)
    }

    /**
     * Returns AndroidX Startup initializer dependencies.
     *
     * Profile module registration does not depend on other initializers.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
