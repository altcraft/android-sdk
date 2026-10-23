package test.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.data.room.Converter
import com.altcraft.sdk.data.room.dao.AuthorizationDao
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.data.room.entities.AuthRequestEntity

/**
 * Minimal Room database for androidTest, using production entities and converters.
 * Mirrors [com.altcraft.sdk.data.room.DB] entity list and DAO accessors.
 */
@Database(
    entities = [
        ConfigurationEntity::class,
        AuthenticationEntity::class,
        AuthRequestEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class TestRoom : RoomDatabase() {
    internal abstract fun configurationDao(): ConfigurationDao
    internal abstract fun authStateDao(): AuthorizationDao
    internal abstract fun authRequestDao(): AuthRequestDao
}
