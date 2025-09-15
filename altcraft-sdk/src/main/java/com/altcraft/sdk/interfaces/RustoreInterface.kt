package com.altcraft.sdk.interfaces

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Interface for RuStore token operations.
 *
 * Provides methods to get and delete the RuStore token.
 */
@Keep
interface RustoreInterface {

    /**
     * Returns the current RuStore token, or null if unavailable.
     */
    suspend fun getToken(): String?

    /**
     * Deletes the RuStore token and invokes the result callback.
     *
     * @param complete Callback with `true` if successful, `false` otherwise.
     */
    suspend fun deleteToken(complete: (Boolean) -> Unit)
}
