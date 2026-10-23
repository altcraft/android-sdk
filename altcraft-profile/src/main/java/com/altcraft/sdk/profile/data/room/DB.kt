package com.altcraft.sdk.profile.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.profile.util.Log.error
import com.altcraft.sdk.profile.data.Constants.DB_NAME
import com.altcraft.sdk.profile.data.Constants.ERROR_KEY

/**
 * Room database used by the Profile module.
 *
 * Stores pending profile update requests independently of the main SDK database.
 */
@Keep
@Database(
    entities = [
        Entity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converter::class)
internal abstract class DB : RoomDatabase() {

    /**
     * Returns the DAO used for profile update operations.
     *
     * @return Profile module [DAO].
     */
    abstract fun dao(): DAO

    /**
     * Clears all Profile module database tables.
     */
    fun clear() = clearAllTables()

    companion object {

        /**
         * Singleton Profile database instance.
         */
        @Volatile
        private var instance: DB? = null

        /**
         * Returns the singleton Profile database instance.
         *
         * The application context is used to prevent leaking an Activity or
         * other short-lived Context.
         *
         * @param context context used to initialize Room.
         * @return singleton [DB] instance.
         */
        fun getInstance(context: Context): DB {
            return instance ?: synchronized(this) {
                instance ?: buildDb(
                    context.applicationContext
                ).also { database ->
                    instance = database
                }
            }
        }

        /**
         * Builds and validates the database instance.
         *
         * If opening the existing database fails, it is deleted and rebuilt
         * from scratch because pending profile updates can safely be requested
         * again by subsequent SDK calls.
         *
         * @param context application context.
         * @return validated [DB] instance.
         */
        private fun buildDb(context: Context): DB {
            val appCtx = context.applicationContext

            return try {
                createAndValidateDb(appCtx)
            } catch (cause: Exception) {
                recreateDb(appCtx, cause)
            }
        }

        /**
         * Deletes the existing database and recreates it from scratch.
         *
         * Emits a database migration error event before deleting the corrupted
         * database so that the failure is observable.
         *
         * @param context Context used to access the application database.
         * @param cause Original exception that caused the previous database to fail.
         * @return Recreated and validated [DB] instance.
         */
        private fun recreateDb(
            context: Context,
            cause: Exception
        ): DB {
            error(
                "buildDb",
                CoreEvents.roomMigrationError,
                mapOf(ERROR_KEY to cause.message)
            )

            context.deleteDatabase(DB_NAME)
            return createAndValidateDb(context)
        }

        /**
         * Creates and immediately opens the database to validate its schema.
         *
         * @param context application context.
         * @return validated Profile database.
         */
        private fun createAndValidateDb(context: Context): DB {
            val database = createDb(context)

            return try {
                database.openHelper.writableDatabase
                    .query("SELECT 1")
                    .close()

                database
            } catch (exception: Exception) {
                database.close()
                throw exception
            }
        }

        /**
         * Creates the Room database instance.
         *
         * @param context Application context.
         * @return Newly created database instance.
         */
        private fun createDb(context: Context): DB {
            return Room.databaseBuilder(
                context.applicationContext,
                DB::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}