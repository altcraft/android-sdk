package com.altcraft.sdk.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.util.log.Log

/**
 * DAO for authorization request operations.
 */
@Dao
internal interface AuthRequestDao {

    /**
     * Inserts or replaces the stored authorization request.
     *
     * @param entity authorization request entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AuthRequestEntity)

    /**
     * Returns the stored authorization request.
     *
     * @return stored authorization request or null if it does not exist.
     */
    @Query(
        """
        SELECT *
        FROM auth_request_table
        WHERE id = 0
        """
    )
    suspend fun get(): AuthRequestEntity?

    /**
     * Deletes all stored authorization requests.
     */
    @Query("DELETE FROM auth_request_table")
    suspend fun deleteAll()

    /**
     * Deletes the specified authorization request.
     *
     * @param entity authorization request entity to delete.
     * @return number of deleted rows.
     */
    @Delete
    suspend fun deleteItem(entity: AuthRequestEntity): Int

    /**
     * Increments retry count for the specified row id.
     *
     * @param id row identifier.
     */
    @Query(
        """
        UPDATE auth_request_table
        SET retryCount = retryCount + 1
        WHERE id = :id
        """
    )
    suspend fun increaseRetryCount(id: Int)

    /**
     * Deletes the authorization request if retry limit is exceeded.
     *
     * @param id row identifier.
     * @return number of deleted rows.
     */
    @Query(
        """
        DELETE FROM auth_request_table
        WHERE id = :id
          AND retryCount > maxRetryCount
        """
    )
    suspend fun deleteIfRetryLimitExceeded(id: Int): Int

    /**
     * Increments retry count and deletes the request if the retry limit
     * is exceeded.
     *
     * Emits the retry-limit event when the request is deleted.
     *
     * @param id row identifier.
     * @return `true` if the request was deleted.
     */
    @Transaction
    suspend fun incrementRetryAndDeleteIfExceeded(id: Int): Boolean {
        increaseRetryCount(id)

        if (deleteIfRetryLimitExceeded(id) == 0) return false
        Log.event(
            "incrementRetryAndDeleteIfExceeded",
            CoreEvents.authRequestRetryLimit
        )
        return true
    }
}