package com.altcraft.sdk.push.platform.token.provider

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Interface for FCM (Firebase Cloud Messaging) operations.
 */
@Keep
interface FCMInterface {

    /**
     * Returns the current FCM token, or null if unavailable.
     */
    suspend fun getToken(): String?

    /**
     * Deletes the FCM token and invokes the result callback.
     */
    fun deleteToken(completion: (Boolean) -> Unit)
}
