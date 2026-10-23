package com.altcraft.sdk.push.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import com.altcraft.sdk.core.modules.ModuleRegistry

/**
 * Automatically registers the push module in Altcraft SDK core.
 *
 * The module is registered during application startup and initialized
 * after the core SDK is initialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ModuleInitializer : Initializer<Unit> {

    /**
     * Registers the push module in the core module registry.
     *
     * @param context Application context supplied by AndroidX Startup.
     */
    override fun create(context: Context) {
        ModuleRegistry.register(PushModule)
    }

    /**
     * Returns AndroidX Startup initializer dependencies.
     *
     * Push module registration does not depend on other initializers.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}