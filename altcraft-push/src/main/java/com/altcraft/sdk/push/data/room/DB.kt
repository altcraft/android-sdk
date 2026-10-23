package com.altcraft.sdk.push.data.room

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
import com.altcraft.sdk.push.data.Constants.DB_NAME
import com.altcraft.sdk.push.data.Constants.ERROR_KEY
import com.altcraft.sdk.push.data.room.dao.PushConfigDao
import com.altcraft.sdk.push.data.room.dao.PushEventDao
import com.altcraft.sdk.push.data.room.dao.PushMessageDao
import com.altcraft.sdk.push.data.room.dao.PushSubscribeDao
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity
import com.altcraft.sdk.push.data.room.entity.PushEventEntity
import com.altcraft.sdk.push.data.room.entity.PushMessageEntity
import com.altcraft.sdk.push.data.room.entity.PushSubscribeEntity
import com.altcraft.sdk.push.util.Log.error

/**
 * Room database used by the Push module.
 */
@Keep
@Database(
    entities = [
        PushEventEntity::class,
        PushSubscribeEntity::class,
        PushConfigEntity::class,
        PushMessageEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converter::class)
internal abstract class DB : RoomDatabase() {

    /**
     * Returns the Push event DAO.
     *
     * @return Push event [PushEventDao] instance.
     */
    protected abstract fun pushEventDao(): PushEventDao

    /**
     * Returns the Push subscription DAO.
     *
     * @return Push subscription [PushSubscribeDao] instance.
     */
    protected abstract fun pushSubscribeDao(): PushSubscribeDao

    /**
     * Returns the Push configuration DAO.
     *
     * @return Push configuration [PushConfigDao] instance.
     */
    protected abstract fun pushConfigDao(): PushConfigDao

    /**
     * Returns the Push message DAO.
     *
     * @return Push message [PushMessageDao] instance.
     */
    protected abstract fun pushMessageDao(): PushMessageDao

    /**
     * Provides access to all Push module DAOs.
     */
    val dao: DaoProvider by lazy {
        DaoProvider(
            event = pushEventDao(),
            subscription = pushSubscribeDao(),
            config = pushConfigDao(),
            message = pushMessageDao()
        )
    }

    /**
     * Clears all Push module database tables.
     */
    fun clear() = clearAllTables()

    /**
     * Container for Push module DAOs.
     *
     * @property event Push event DAO.
     * @property subscription Push subscription DAO.
     * @property config Push configuration DAO.
     * @property message Processed push message DAO.
     */
    internal class DaoProvider(
        val event: PushEventDao,
        val subscription: PushSubscribeDao,
        val config: PushConfigDao,
        val message: PushMessageDao
    )

    companion object {

        /**
         * Singleton database instance.
         */
        @Volatile
        private var instance: DB? = null

        /**
         * Returns the singleton database instance.
         *
         * @param context Application context.
         * @return Push database instance.
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
         * from scratch.
         *
         * @param context Application context.
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
         * Creates the database and validates its schema.
         *
         * @param context Application context.
         * @return Valid [DB] instance.
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