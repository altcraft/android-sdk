package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.data.Constants.ALTCRAFT_DB_NAME

/**
 * SDKdb - Abstract class representing the SDK database based on the Room persistence library.
 */
@Keep
@Database(
    entities = [
        ConfigurationEntity::class,
        SubscribeEntity::class,
        PushEventEntity::class
    ],
    version = 1
)
@TypeConverters(Converter::class)
internal abstract class SDKdb : RoomDatabase() {

    /**
     * Provides the Data Access Object (DAO) for interacting with the database.
     *
     * @return DAO instance for database operations.
     */
    abstract fun request(): DAO

    companion object {
        @Volatile
        private var INSTANCE: SDKdb? = null

        /**
         * Returns a singleton instance of [SDKdb] for database access.
         *
         * This method ensures a single instance of the database is used across the application
         * to prevent multiple database instances from being created.
         *
         * @param context The application context.
         * @return A singleton instance of [SDKdb].
         */
        fun getDb(context: Context): SDKdb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SDKdb::class.java,
                    ALTCRAFT_DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}