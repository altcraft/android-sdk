package com.altcraft.altcraftmobile.providers.rustore

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.interfaces.RustoreInterface
import kotlinx.coroutines.CompletableDeferred
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import ru.rustore.sdk.pushclient.RuStorePushClient

/**
 * RuStore implementation for managing push tokens.
 *
 * Provides methods to retrieve and delete tokens via the RuStore Push SDK.
 */
class RuStoreProvider : RustoreInterface {

    /**
     * Retrieves the current RuStore token.
     *
     * @return The token, or `null` on error.
     */
    override suspend fun getToken(): String? {
        val deferred = CompletableDeferred<String?>()
        try {
            val token = RuStorePushClient.getToken().await()

            RuStorePushClient.checkPushAvailability().addOnSuccessListener { result ->
                when (result) {
                    FeatureAvailabilityResult.Available -> deferred.complete(token)
                    is FeatureAvailabilityResult.Unavailable -> deferred.complete(null)
                }
            }.addOnFailureListener {
                deferred.complete(null)
            }
        } catch (_: Exception) {
            return null
        }

        return deferred.await()
    }

    /**
     * Deletes the current RuStore token.
     *
     * @param complete Callback with `true` on success, `false` on failure.
     */
    override suspend fun deleteToken(complete: (Boolean) -> Unit) {
        try {
            RuStorePushClient.deleteToken()
                .addOnSuccessListener {
                    complete(true)
                }
                .addOnFailureListener {
                    complete(false)
                }
        } catch (e: Exception) {
            complete(false)
        }
    }
}