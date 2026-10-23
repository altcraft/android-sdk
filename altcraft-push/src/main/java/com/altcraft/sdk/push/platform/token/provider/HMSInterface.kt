package com.altcraft.sdk.push.platform.token.provider

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep

/**
 * Interface for HMS (Huawei Mobile Services) operations.
 */
@Keep
interface HMSInterface {

    /**
     * Returns the current HMS token, or null if unavailable.
     */
    suspend fun getToken(context: Context): String?

    /**
     * Deletes the HMS token and invokes the result callback.
     */
    fun deleteToken(context: Context, complete: (Boolean) -> Unit)
}
