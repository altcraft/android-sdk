package com.altcraft.sdk.data.room.entities

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo

/**
 * Common contract for Room entities representing persisted retryable requests.
 *
 * @param D DAO type associated with the entity.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface RetryableEntity<D> {

    /**
     * Handles retry processing for the current entity.
     *
     * Increments the retry counter and removes the entity when the configured
     * retry limit is exceeded.
     *
     * @param dao DAO instance associated with this entity type.
     * @return `true` if the entity was deleted, otherwise `false`.
     */
    suspend fun retryLimit(dao: D): Boolean
}