package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing in-app placements request data.
 */
@Entity(tableName = "in_apps_table")
internal data class InAppsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "acid")
    val acid: String,
    @ColumnInfo(name = "deviceId")
    val deviceId: String,
    @ColumnInfo(name = "lastPublish")
    val lastPublish: Long = 0,
    @ColumnInfo(name = "placementRulesChecksum")
    val placementRulesChecksum: String = "",
    @ColumnInfo(name = "lastRequestTime")
    val lastRequestTime: Long = 0,
)