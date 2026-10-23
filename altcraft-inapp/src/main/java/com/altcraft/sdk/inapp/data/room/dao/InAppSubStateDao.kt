package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.inapp.data.room.entity.InAppSubStateEntity

/**
 * DAO for storing and reading in-app subscription state.
 */
@Dao
internal interface InAppSubStateDao {

    /**
     * Saves in-app subscription state.
     *
     * Uses [OnConflictStrategy.REPLACE], so state for the same user tag is
     * fully overwritten.
     *
     * @param state In-app subscription state to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: InAppSubStateEntity)

    /**
     * Returns in-app subscription state for the specified user tag.
     *
     * @param userTag SDK user tag.
     * @return Saved subscription state, or `null` if not found.
     */
    @Query("SELECT * FROM in_app_sub_state_entity WHERE userTag = :userTag LIMIT 1")
    suspend fun get(userTag: String): InAppSubStateEntity?

    /**
     * Removes all saved in-app subscription states.
     */
    @Query("DELETE FROM in_app_sub_state_entity")
    suspend fun clear()
}