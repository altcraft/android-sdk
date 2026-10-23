package com.altcraft.sdk.data.room.entities

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing authorization state data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("SpellCheckingInspection")
@Entity(tableName = "authentication_table")
data class AuthenticationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "anonimAcid")
    val anonimAcid: String,
    @ColumnInfo(name = "currentAcid")
    val acid: String,
    @ColumnInfo(name = "deviceId")
    val deviceId: String,
    @ColumnInfo(name = "currentUserTag")
    val currentUser: String?,
    @ColumnInfo(name = "startAnonimSession")
    val startAnonSession: Long,
    @ColumnInfo(name = "isTest")
    val isTest: Boolean = false
)