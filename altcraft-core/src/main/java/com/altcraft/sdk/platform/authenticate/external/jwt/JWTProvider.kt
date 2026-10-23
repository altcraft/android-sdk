package com.altcraft.sdk.platform.authenticate.external.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Holds the registered JWT token provider.
 *
 * Provides a central point to register a [JWTInterface] implementation and
 * retrieve the current JWT token when needed by internal SDK operations.
 */
internal object JWTProvider {

    /**
     * Registered JWT token provider instance.
     */
    @Volatile
    internal var provider: JWTInterface? = null

    /**
     * Returns the current JWT token provided by the registered [JWTInterface].
     *
     * @return A JWT token string, or `null` if no provider is set.
     */
    fun getJWT(): String? = provider?.getJWT()
}