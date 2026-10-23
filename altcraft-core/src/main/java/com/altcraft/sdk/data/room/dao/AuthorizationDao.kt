package com.altcraft.sdk.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.altcraft.sdk.data.room.entities.AuthenticationEntity

/**
 * DAO for authorization state operations.
 */
@Dao
internal interface AuthorizationDao {

    /**
     * Inserts authorization data into the database.
     *
     * Replaces the existing row if it already exists.
     *
     * @param authenticationEntity authorization entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(authenticationEntity: AuthenticationEntity)

    /**
     * Returns current authorization data.
     *
     * @return stored authorization or null if it does not exist.
     */
    @Query("SELECT * FROM authentication_table WHERE id = 0")
    suspend fun get(): AuthenticationEntity?

    /**
     * Deletes authorization data.
     */
    @Query("DELETE FROM authentication_table")
    suspend fun delete()

    /**
     * Updates anonymous acid value.
     *
     * @param anonAcid new anonymous acid value.
     */
    @Query("UPDATE authentication_table SET anonimAcid = :anonAcid WHERE id = 0")
    suspend fun updateAnonAcid(anonAcid: String?)

    /**
     * Updates current acid value.
     *
     * @param currentAcid new current acid value.
     */
    @Query("UPDATE authentication_table SET currentAcid = :currentAcid WHERE id = 0")
    suspend fun updateCurrentAcid(currentAcid: String?)

    /**
     * Updates current user tag value.
     *
     * @param currentUserTag new current user tag.
     */
    @Query("UPDATE authentication_table SET currentUserTag = :currentUserTag WHERE id = 0")
    suspend fun updateCurrentTag(currentUserTag: String?)

    /**
     * Updates test mode flag.
     *
     * @param isTest new test mode value.
     */
    @Query("UPDATE authentication_table SET isTest = :isTest WHERE id = 0")
    suspend fun updateIsTest(isTest: Boolean)
}