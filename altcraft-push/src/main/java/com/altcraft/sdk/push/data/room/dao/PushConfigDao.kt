package com.altcraft.sdk.push.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity

/**
 * DAO for push configuration operations.
 */
@Dao
internal interface PushConfigDao {

    /**
     * Inserts push configuration into the database.
     *
     * Replaces the existing row if it already exists.
     *
     * @param entity push configuration entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PushConfigEntity)

    /**
     * Returns the current push configuration.
     *
     * @return stored configuration or null if it does not exist.
     */
    @Query("SELECT * FROM push_config_table WHERE id = 0")
    suspend fun get(): PushConfigEntity?

    /**
     * Deletes all push configurations from the table.
     */
    @Query("DELETE FROM push_config_table")
    suspend fun delete()
}