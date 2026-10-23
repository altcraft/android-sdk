package com.altcraft.sdk.core.environment

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.RestrictTo
import com.altcraft.sdk.data.Constants.PROVIDER_NOT_INITIALIZED
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides Core environment instances and selected Core data
 * to other SDK modules.
 *
 * A new [Environment] instance is created for each operation. Values inside
 * the environment are loaded lazily and cached only for that environment.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CoreEnvironmentProvider {

    @Volatile
    private var appContext: Context? = null

    /**
     * Initializes the provider with the application context.
     *
     * @param context Context used to access Core dependencies.
     */
    internal fun initialize(context: Context) {
        if (appContext != null) return

        synchronized(this) {
            if (appContext == null) {
                appContext = context.applicationContext
            }
        }
    }

    /**
     * Creates a new Core environment.
     *
     * @return New Core environment instance.
     * @throws IllegalStateException if the provider is not initialized.
     */
    internal fun createEnvironment() = Environment.create(
        requireContext()
    )

    /**
     * Returns the current user tag.
     *
     * A temporary Core environment is created for this request.
     *
     * @return Current user tag.
     * @throws IllegalStateException if the provider is not initialized
     * or the user tag is unavailable.
     */
    suspend fun user(): String = createEnvironment()
        .auth
        .external
        .user()


    /**
     * Returns whether rToken-based authentication is in use.
     *
     * @return `true` if the SDK configuration defines an RToken.
     */
    suspend fun usesRToken() = !createEnvironment()
        .config()
        .rToken
        .isNullOrEmpty()

    /**
     * Returns the current application version from the SDK configuration.
     *
     * @return Current application version, or `null` if it is not set.
     */
    suspend fun appVersion() = createEnvironment()
        .config()
        .appInfo
        ?.appVer

    /**
     * Checks whether the current profile is authenticated.
     *
     * A temporary Core environment is created for this request.
     *
     * @return `true` if the current user tag matches the stored
     * authentication state; otherwise `false`.
     */
    suspend fun isAuth() = AuthenticateProcess.isAuthenticated(
        createEnvironment()
    )

    /**
     * Returns the internal authentication state.
     *
     * A temporary Core environment is created for this request.
     *
     * @return Current internal authentication entity.
     */
    suspend fun internalAuth() = withContext(Dispatchers.IO) {
        createEnvironment().auth.internal()
    }

    /**
     * Returns the stored application context.
     *
     * @throws IllegalStateException if the provider is not initialized.
     */
    private fun requireContext() = checkNotNull(appContext) {
        PROVIDER_NOT_INITIALIZED
    }
}