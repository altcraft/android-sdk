package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing cached in-app static resources.
 */
@Entity(tableName = "in_app_static_table")
internal data class InAppStaticEntity(
    @PrimaryKey
    val id: Int = 0,
    val inAppLibraryJs: String,
    val blockJs: String,
    val blockCss: String,
    val createdAt: Long
)