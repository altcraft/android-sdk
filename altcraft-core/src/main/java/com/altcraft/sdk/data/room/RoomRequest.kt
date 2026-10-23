package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.util.log.Log.error
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles SDK Room database operations.
 *
 * Provides insert, delete, retry, and cleanup logic for Room-managed entities.
 */
internal object RoomRequest {

    /**
     * Deletes requests that must be canceled when the current user changes.
     *
     * @param room SDK database instance used to access DAOs that store user-bound requests.
     */
    suspend fun deleteAcidSensitiveRequests(room: DB) = coroutineScope {
        try {
            awaitAll(
                async { room.dao.authRequest.deleteAll() }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("deleteAcidSensitiveRequests", e)
        }
    }
}