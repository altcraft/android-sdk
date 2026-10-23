package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity

/**
 * DAO for in-app configuration operations.
 */
@Dao
internal interface InAppConfigDao {

    /**
     * Inserts in-app configuration into the database.
     *
     * Replaces the existing row if it already exists.
     *
     * @param entity In-app configuration entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InAppConfigEntity)

    /**
     * Returns the current in-app configuration.
     *
     * @return Stored configuration or `null` if it does not exist.
     */
    @Query("SELECT * FROM in_app_config WHERE id = 0")
    suspend fun get(): InAppConfigEntity?

    /**
     * Deletes the stored in-app configuration.
     *
     * Called when the in-app configuration fails validation.
     */
    @Query("DELETE FROM in_app_config WHERE id = 0")
    suspend fun delete()
}