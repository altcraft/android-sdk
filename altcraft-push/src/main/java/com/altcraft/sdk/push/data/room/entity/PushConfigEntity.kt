package com.altcraft.sdk.push.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing push module configuration.
 */
@Entity(tableName = "push_config_table")
internal data class PushConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "icon")
    val icon: Int? = null,
    @ColumnInfo(name = "providerPriorityList")
    val providerPriority: List<String> = emptyList(),
    @ColumnInfo(name = "pushReceiverModules")
    val receiverPackages: List<String> = emptyList(),
    @ColumnInfo(name = "pushChannelName")
    val pushChannelName: String? = null,
    @ColumnInfo(name = "pushChannelDescription")
    val pushChannelDescription: String? = null
)