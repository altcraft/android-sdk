package com.altcraft.sdk.push.platform.token.provider

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Interface for RuStore token operations.
 */
@Keep
interface RustoreInterface {

    /**
     * Returns the current RuStore token, or null if unavailable.
     */
    suspend fun getToken(): String?

    /**
     * Deletes the RuStore token and invokes the result callback.
     */
    fun deleteToken(complete: (Boolean) -> Unit)
}
