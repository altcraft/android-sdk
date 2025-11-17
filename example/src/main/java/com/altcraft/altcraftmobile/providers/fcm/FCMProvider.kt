package com.altcraft.altcraftmobile.providers.fcm

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.interfaces.FCMInterface
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

/**
 * FCM implementation for managing Firebase Cloud Messaging tokens.
 *
 * Provides methods to retrieve and delete FCM tokens via the Firebase SDK.
 */
class FCMProvider : FCMInterface {

    /**
     * Retrieves the current FCM token.
     *
     * Returns `null` if an error occurs during retrieval.
     *
     * @return The FCM token or `null` on failure.
     */
    override suspend fun getToken(): String? {
        return try {
            Firebase.messaging.token.await()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes the current FCM token.
     *
     * Invokes [completion] with `true` if successful, `false` otherwise.
     *
     * @param completion Callback with the result of the deletion.
     */
    override suspend fun deleteToken(completion: (Boolean) -> Unit) {
        try {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                completion(it.isSuccessful)
            }
        } catch (_: Exception) {
            completion(false)
        }
    }
}
