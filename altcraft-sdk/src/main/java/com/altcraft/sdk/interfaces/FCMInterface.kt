package com.altcraft.sdk.interfaces

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Interface for FCM (Firebase Cloud Messaging) operations.
 *
 * Provides methods to get and delete the FCM token.
 */
@Keep
interface FCMInterface {

    /**
     * Returns the current FCM token, or null if unavailable.
     */
    suspend fun getToken(): String?

    /**
     * Deletes the FCM token and invokes the result callback.
     *
     * @param completion Callback with `true` if successful, `false` otherwise.
     */
    suspend fun deleteToken(completion: (Boolean) -> Unit)
}