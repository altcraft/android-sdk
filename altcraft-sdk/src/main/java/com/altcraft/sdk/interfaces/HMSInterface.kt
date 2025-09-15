package com.altcraft.sdk.interfaces

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep

/**
 * Interface for HMS (Huawei Mobile Services) operations.
 *
 * Provides methods to get and delete the HMS token.
 */
@Keep
interface HMSInterface {

    /**
     * Returns the current HMS token, or null if unavailable.
     *
     * @param context Context required for token access.
     */
    suspend fun getToken(context: Context): String?

    /**
     * Deletes the HMS token and invokes the result callback.
     *
     * @param context Context required for token deletion.
     * @param complete Callback with `true` if successful, `false` otherwise.
     */
    suspend fun deleteToken(context: Context, complete: (Boolean) -> Unit)
}