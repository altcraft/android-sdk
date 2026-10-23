package com.altcraft.sdk.platform.authenticate.external

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.core.modules.ModuleRegistry.notifyRTokenChanged

/**
 * Handles role token changes in the SDK configuration.
 */
internal object RTokenChange {

    /**
     * Checks whether the stored role token differs from the new one.
     *
     * If both tokens are present and their values differ, all registered
     * SDK modules are notified before the new configuration is installed.
     *
     * @param context Application context.
     * @param config New SDK configuration.
     */
    suspend fun notifyIfRTokenChanged(
        context: Context,
        config: AltcraftConfiguration
    ) {
        val oldToken = Environment.create(context).room.dao.config.get()?.rToken

        val newToken = config.rToken

        if (oldToken != null && newToken != null && oldToken != newToken) notifyRTokenChanged(
            context
        )
    }
}