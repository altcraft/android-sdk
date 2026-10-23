package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.inapp.data.room.entity.InAppStaticEntity

/**
 * DAO for cached in-app static resources.
 */
@Dao
internal interface InAppStaticDao {

    /**
     * Returns cached in-app static resources.
     *
     * @return cached static resources or null if they do not exist.
     */
    @Query("SELECT * FROM in_app_static_table WHERE id = 0")
    suspend fun get(): InAppStaticEntity?

    /**
     * Inserts or replaces cached in-app static resources.
     *
     * @param entity in-app static entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InAppStaticEntity)

    /**
     * Deletes cached in-app static resources.
     */
    @Query("DELETE FROM in_app_static_table WHERE id = 0")
    suspend fun delete()
}