package com.altcraft.sdk.push.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the UID of a processed push message.
 */
@Entity(
    tableName = "push_message",
    indices = [
        Index(
            value = ["uid"],
            unique = true
        )
    ]
)
internal data class PushMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String
)