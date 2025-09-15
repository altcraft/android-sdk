package test.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.Converter

/**
 * Minimal Room database for androidTest, using production entities and converters.
 * - Uses the same DAO interface as production so Room generates a REAL implementation.
 */
@Database(
    entities = [
        ConfigurationEntity::class,
        SubscribeEntity::class,
        PushEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class TestRoom : RoomDatabase() {
    internal abstract fun request(): DAO
}