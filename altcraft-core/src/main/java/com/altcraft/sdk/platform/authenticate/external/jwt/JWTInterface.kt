package com.altcraft.sdk.platform.authenticate.external.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Interface for accessing a JWT token.
 *
 * Provides a method to retrieve the current JSON Web Token.
 */
@Keep
interface JWTInterface {

    /**
     * Returns the current JWT token, or null if unavailable.
     */
    fun getJWT(): String?
}
