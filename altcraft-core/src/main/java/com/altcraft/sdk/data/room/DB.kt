package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.data.Constants.DB_NAME
import com.altcraft.sdk.data.Constants.ERROR_KEY
import com.altcraft.sdk.data.room.dao.AuthorizationDao
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.events.CoreEvents

/**
 * SDKdb is the Room database used by the SDK.
 */
@Keep
@Database(
    entities = [
        ConfigurationEntity::class,
        AuthenticationEntity::class,
        AuthRequestEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converter::class)
internal abstract class DB : RoomDatabase() {

    /**
     * Returns DAO for configuration operations.
     *
     * @return [ConfigurationDao] instance.
     */
    abstract fun configurationDao(): ConfigurationDao

    /**
     * Returns DAO for authorization state operations.
     *
     * @return [AuthorizationDao] instance.
     */
    abstract fun authStateDao(): AuthorizationDao

    /**
     * Returns DAO for authorization request operations.
     *
     * @return [AuthRequestDao] instance.
     */
    abstract fun authRequestDao(): AuthRequestDao

    /**
     * Provides access to all SDK module DAOs.
     */
    val dao: DaoProvider by lazy {
        DaoProvider(
            config = configurationDao(),
            authState = authStateDao(),
            authRequest = authRequestDao()
        )
    }

    /**
     * Clears all Core module database tables.
     */
    fun clear() = clearAllTables()

    /**
     * Container for SDK module DAOs.
     *
     * @property config configuration DAO.
     * @property authState authorization state DAO.
     * @property authRequest authorization request DAO.
     */
    internal class DaoProvider(
        val config: ConfigurationDao,
        val authState: AuthorizationDao,
        val authRequest: AuthRequestDao
    )

    companion object {

        @Volatile
        private var INSTANCE: DB? = null

        /**
         * Returns the singleton database instance.
         *
         * Creates and validates the database on first access.
         * If the existing database cannot be opened or migrated,
         * it is deleted and recreated.
         *
         * @param context Context used to access the application database.
         * @return Singleton instance of [DB].
         */
        fun getDb(context: Context): DB {
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
         * @return Validated [DB] instance.
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
         * Creates and validates the database instance.
         *
         * @param context Context used to create and open the database.
         * @return Validated [DB] instance.
         */
        private fun createAndValidateDb(context: Context): DB {
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
         * @return Configured [DB] instance.
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
