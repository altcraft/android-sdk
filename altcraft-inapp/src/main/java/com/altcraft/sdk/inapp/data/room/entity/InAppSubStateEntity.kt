package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the latest in-app subscription state for a specific SDK user.
 *
 * The [userTag] is used as the primary key, so saving a new state for the same
 * user fully replaces the previous state when DAO uses `REPLACE` policy.
 *
 * @property userTag Current SDK user tag.
 * @property status Raw in-app subscription status.
 */
@Entity(tableName = "in_app_sub_state_entity")
internal data class InAppSubStateEntity(
    @PrimaryKey
    val userTag: String,
    val status: String
)