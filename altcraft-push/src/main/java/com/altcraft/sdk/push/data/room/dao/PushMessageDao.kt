package com.altcraft.sdk.push.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.push.data.room.entity.PushMessageEntity

/**
 * Provides access to processed push message UIDs.
 */
@Dao
internal interface PushMessageDao {


    /**
     * Inserts a message UID if it does not already exist.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(
        entity: PushMessageEntity
    ): Long


    /**
     * Keeps only the latest 100 message UIDs.
     */
    @Query(
        """
        DELETE FROM push_message
        WHERE id NOT IN (
            SELECT id
            FROM push_message
            ORDER BY id DESC
            LIMIT 100
        )
        """
    )
    suspend fun trim()


    /**
     * Checks whether the message UID is unique.
     *
     * @return `true` if the UID is new, otherwise `false`.
     */
    @Transaction
    suspend fun isMessageUnique(uid: String): Boolean {
        val id = insert(
            PushMessageEntity(uid = uid)
        )


        if (id == -1L) return false


        trim()


        return true
    }
}