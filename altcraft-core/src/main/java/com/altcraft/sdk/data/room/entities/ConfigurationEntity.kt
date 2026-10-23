package com.altcraft.sdk.data.room.entities

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altcraft.sdk.data.models.CoreModels.PropertyModel.AppInfo

/**
 * ConfigurationEntity - entity for storing configuration data.
 */
@Suppress("SpellCheckingInspection")
@Entity(tableName = "configuration_table")
internal data class ConfigurationEntity(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "apiUrl")
    val apiUrl: String,
    @ColumnInfo(name = "rToken")
    val rToken: String?,
    @ColumnInfo(name = "appInfo")
    val appInfo: AppInfo?
)
