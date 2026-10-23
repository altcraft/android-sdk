package com.altcraft.sdk.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.data.room.entities.ConfigurationEntity

/**
 * DAO for configuration operations.
 */
@Dao
internal interface ConfigurationDao {

    /**
     * Inserts configuration into the database.
     *
     * Replaces the existing row if it already exists.
     *
     * @param config Configuration entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ConfigurationEntity)

    /**
     * Returns the current configuration.
     *
     * @return Stored configuration or `null` if it does not exist.
     */
    @Query("SELECT * FROM configuration_table WHERE id = 0")
    suspend fun get(): ConfigurationEntity?

    /**
     * Deletes configuration from the table.
     */
    @Query("DELETE FROM configuration_table")
    suspend fun delete()
}