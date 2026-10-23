package com.altcraft.sdk.inapp.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.inapp.data.Constants.DB_NAME
import com.altcraft.sdk.inapp.data.Constants.ERROR_KEY
import com.altcraft.sdk.inapp.data.room.dao.InAppConfigDao
import com.altcraft.sdk.inapp.data.room.dao.InAppDao
import com.altcraft.sdk.inapp.data.room.dao.InAppSubStateDao
import com.altcraft.sdk.inapp.data.room.dao.InAppsDao
import com.altcraft.sdk.inapp.data.room.dao.InAppEventDao
import com.altcraft.sdk.inapp.data.room.dao.InAppStaticDao
import com.altcraft.sdk.inapp.data.room.dao.InAppSubscribeDao
import com.altcraft.sdk.inapp.data.room.entity.InAppConfigEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppPlacementEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppSubStateEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppTriggerEventEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.inapp.data.room.entity.PersonalPolicyEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppEventEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppStaticEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.util.Log.error

/**
 * Room database used by the in-app module.
 */
@Database(
    entities = [
        InAppConfigEntity::class,
        InAppEntity::class,
        InAppPlacementEntity::class,
        InAppTriggerEventEntity::class,
        PersonalPolicyEntity::class,
        InAppsEntity::class,
        InAppSubStateEntity::class,
        InAppEventEntity::class,
        InAppStaticEntity::class,
        InAppSubscribeEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converter::class)
internal abstract class DB : RoomDatabase() {

    /**
     * Returns DAO for in-app configuration operations.
     */
    protected abstract fun inAppConfigDao(): InAppConfigDao

    /**
     * Returns DAO for in-app campaign operations.
     */
    protected abstract fun inAppDao(): InAppDao

    /**
     * Returns DAO for stored in-app placements request.
     */
    protected abstract fun inAppsDao(): InAppsDao

    /**
     * Returns DAO for in-app subscription operations.
     */
    protected abstract fun inAppSubscribeDao(): InAppSubscribeDao

    /**
     * Returns DAO for in-app event operations.
     */
    protected abstract fun inAppEventDao(): InAppEventDao

    /**
     * Returns DAO for cached in-app static resources.
     */
    protected abstract fun inAppStaticDao(): InAppStaticDao

    /**
     * Returns DAO used for storing and retrieving in-app subscription state.
     */
    protected abstract fun inAppSubStateDao(): InAppSubStateDao

    /**
     * Provides access to all in-app module DAOs.
     */
    val dao: DaoProvider by lazy {
        DaoProvider(

            inApp = inAppDao(),
            inApps = inAppsDao(),
            event = inAppEventDao(),
            static = inAppStaticDao(),
            config = inAppConfigDao(),
            subState = inAppSubStateDao(),
            subscribe = inAppSubscribeDao(),
        )
    }

    /**
     * Clears all In-app module database tables.
     */
    fun clear() = clearAllTables()

    /**
     * Container for in-app module DAOs.
     *
     * @property config in-app configuration DAO.
     * @property inApp in-app campaign DAO.
     * @property inApps stored in-app placements request DAO.
     * @property subscribe in-app subscription DAO.
     * @property event in-app event DAO.
     * @property static cached in-app static resources DAO.
     * @property subState in-app subscription state DAO.
     */
    internal class DaoProvider(
        val config: InAppConfigDao,
        val inApp: InAppDao,
        val inApps: InAppsDao,
        val subscribe: InAppSubscribeDao,
        val event: InAppEventDao,
        val static: InAppStaticDao,
        val subState: InAppSubStateDao
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
        fun getInstance(context: Context): DB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDb(context.applicationContext).also { db ->
                    INSTANCE = db
                }
            }
        }

        /**
         * Builds and validates the module database instance.
         *
         * If opening or migration fails, the database is deleted
         * and recreated from scratch.
         *
         * @param context Context used to access the application database.
         * @return Validated [DB] instance.
         */
        private fun buildDb(context: Context): DB {
            val appCtx = context.applicationContext

            return try {
                createAndValidateDb(appCtx)
            } catch (e: Exception) {
                recreateDb(appCtx, e)
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
