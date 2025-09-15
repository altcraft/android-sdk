package com.altcraft.sdk.auth

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.interfaces.JWTInterface

/**
 * Singleton manager responsible for handling JWT-based authentication within the SDK.
 *
 * This class provides a central point to register a [JWTInterface] provider and retrieve
 * the current JWT token when needed by internal SDK operations.
 */
internal class JWTManager private constructor() {

    /**
     * Holds the registered JWT token provider instance.
     */
    private var jwtProvider: JWTInterface? = null

    companion object {

        /**
         * Lazily initialized singleton instance of [JWTManager].
         */
        val instance: JWTManager by lazy { JWTManager() }

        /**
         * Registers a [JWTInterface] implementation as the current JWT provider.
         *
         * This method sets the provider globally for SDK usage.
         *
         * @param provider An implementation of [JWTInterface] that supplies JWT tokens.
         */
         fun register(provider: JWTInterface?) {
            instance.jwtProvider = provider
        }
    }

    /**
     * Returns the current JWT token provided by the registered [JWTInterface].
     *
     * @return A JWT token string, or `null` if no provider is set.
     */
    fun getJWT(): String? = jwtProvider?.getJWT()
}