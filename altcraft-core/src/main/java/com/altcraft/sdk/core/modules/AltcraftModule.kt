package com.altcraft.sdk.core.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Defines lifecycle callbacks for an optional Altcraft SDK module.
 *
 * Optional modules register an implementation of this interface in
 * [ModuleRegistry] and receive SDK initialization and cleanup events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AltcraftModule {

    /**
     * Unique module identifier.
     *
     * The identifier is used to prevent duplicate module registration.
     */
    val moduleId: String

    /**
     * Validates this module configuration.
     *
     * Called before [configure] to ensure that the provided configuration
     * contains valid values required by this module.
     *
     * @param context Application context.
     * @param configuration Configuration of this module to validate.
     */
    suspend fun validate(context: Context, configuration: ModuleConfiguration) {}

    /**
     * Persists and applies this module configuration.
     *
     * Called only for modules whose configuration is present and has
     * passed [validate]. The default implementation does nothing.
     *
     * @param context Application context.
     * @param configuration Configuration of this module.
     */
    suspend fun configure(context: Context, configuration: ModuleConfiguration) {}

    /**
     * Called after Altcraft SDK core initialization has completed successfully.
     *
     * This callback can be used to initialize module databases, retry pending
     * operations, register workers, and perform other startup operations.
     *
     * @param context Application context.
     */
    suspend fun onSdkInitialized(context: Context)

    /**
     * Called when Altcraft SDK data is being cleared.
     *
     * The module must stop its background work and remove its own persisted data.
     *
     * @param context Application context.
     */
    suspend fun onSdkClear(context: Context)

    /**
     * Called when the SDK RToken changes.
     *
     * The module should clean up data that is bound to the previous RToken
     * (e.g., subscriptions tied to a specific user profile).
     *
     * @param context Application context.
     */
    suspend fun onSdkRTokenChanged(context: Context) {}
}