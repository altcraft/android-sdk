package com.altcraft.sdk.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.coordination.SDKScope
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTInterface
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProvider
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import kotlinx.coroutines.launch

/**
 * Public facade for authentication operations.
 */
@Keep
object AuthAPI {

    /**
     * Sets the JWT token provider.
     *
     * @param provider Implementation of [JWTInterface] supplying JWT tokens.
     */
    fun setJWTProvider(provider: JWTInterface?) {
        JWTProvider.provider = provider
    }

    /**
     * Authenticates the current SDK user.
     *
     * @param context Application context.
     */
    fun authenticate(context: Context) {
        AuthenticateProcess.authenticate(context)
    }

    /**
     * Logs out the current SDK user and restores anonymous auth state.
     *
     * @param context Application context.
     */
    fun logOut(context: Context) = SDKScope.io.launch {
        AuthenticateProcess.logOut(context)
    }

    /**
     * Checks whether the current SDK user is authenticated.
     *
     * @param context Application context.
     * @return `true` if current SDK user is authenticated, otherwise `false`.
     */
    suspend fun isAuthenticated(context: Context) = AuthenticateProcess.isAuthenticated(
        Environment.create(context)
    )
}
