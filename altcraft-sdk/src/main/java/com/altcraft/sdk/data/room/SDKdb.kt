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
import com.altcraft.sdk.data.Constants.ERROR
import com.altcraft.sdk.sdk_events.EventList.roomMigrationError
import com.altcraft.sdk.sdk_events.Events.error

/**
 * SDKdb is the Room database used by the SDK.
 */
@Keep
@Database(
    entities = [
        ConfigurationEntity::class,
        SubscribeEntity::class,
        PushEventEntity::class,
        MobileEventEntity::class,
        ProfileUpdateEntity::class
    ],
    version = 1,
    exportSchema = true
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
         * Returns the singleton database instance.
         *
         * Creates and validates the database on first access.
         * If the existing database cannot be opened or migrated,
         * it is deleted and recreated.
         *
         * @param context Context used to access the application database.
         * @return Singleton instance of [SDKdb].
         */
        fun getDb(context: Context): SDKdb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDb(context.applicationContext).also { db ->
                    INSTANCE = db
                }
            }
        }

        /**
         * Builds and validates the database instance.
         *
         * Room first tries to open the database normally. This includes
         * available automatic migrations and destructive fallback when no
         * migration path exists.
         *
         * If opening or migration fails, the database is closed, deleted,
         * and recreated from scratch.
         *
         * @param context Context used to access the application database.
         * @return Validated [SDKdb] instance.
         */
        private fun buildDb(context: Context): SDKdb {
            val appCtx = context.applicationContext

            return try {
                createAndValidateDb(appCtx)
            } catch (e: Exception) {
                error("buildDb",
                    roomMigrationError,
                    mapOf(ERROR to e.message)
                )

                appCtx.deleteDatabase(ALTCRAFT_DB_NAME)

                createAndValidateDb(appCtx)
            }
        }

        /**
         * Creates and validates the database instance.
         *
         * @param context Context used to create and open the database.
         * @return Validated [SDKdb] instance.
         */
        private fun createAndValidateDb(context: Context): SDKdb {
            val db = createDb(context)

            return try {
                db.openHelper.writableDatabase.query("SELECT 1").close()
                db
            } catch (e: Exception) {
                db.close()
                throw e
            }
        }

        /**
         * Creates a Room database instance.
         *
         * @param context Context used to create the database.
         * @return Configured [SDKdb] instance.
         */
        private fun createDb(context: Context): SDKdb {
            return Room.databaseBuilder(
                context.applicationContext,
                SDKdb::class.java,
                ALTCRAFT_DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}