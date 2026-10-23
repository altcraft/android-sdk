package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity

/**
 * DAO for stored in-app placements requests.
 */
@Dao
internal interface InAppsDao {

    /**
     * Inserts or replaces the specified in-app placements request.
     *
     * @param entity In-app placements entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InAppsEntity)

    /**
     * Returns stored in-app placements request.
     *
     * @return Stored in-app placements entity or `null` if it does not exist.
     */
    @Query("""SELECT * FROM in_apps_table WHERE id = 0""")
    suspend fun get(): InAppsEntity?

    /**
     * Resets the placement rules checksum.
     */
    @Query("""
        UPDATE in_apps_table
        SET placementRulesChecksum = ''
        WHERE id = 0
    """)
    suspend fun resetChecksum()

    /**
     * Deletes stored in-app placements request.
     */
    @Query("""DELETE FROM in_apps_table""")
    suspend fun clearInAppsData()
}