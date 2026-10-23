package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing in-app module configuration.
 */
@Entity(tableName = "in_app_config")
internal data class InAppConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "inAppAutoRequest")
    val inAppAutoRequest: Boolean,
    @ColumnInfo(name = "placementsRequestInterval")
    val placementsRequestInterval: Long,
    @ColumnInfo(name = "maxPlacements")
    val maxPlacements: Int
)
