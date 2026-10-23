package com.altcraft.sdk.inapp.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.altcraft.sdk.inapp.util.Log.error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Stores and retrieves in-app module data in persistent storage.
 *
 * In-app display metadata is stored in a dedicated DataStore file.
 */
internal object Preferenses {

    private const val TAG = "Altcraft_InApp"
    private const val IN_APP_DATA_STORE = "${TAG}_DATASTORE"

    private val Context.inAppDataStore by preferencesDataStore(
        IN_APP_DATA_STORE
    )

    private val IN_APP_LAST_OPEN_TIME_KEY = longPreferencesKey(
        "${TAG}_LAST_OPEN_TIME"
    )

    /**
     * Saves the current in-app last display timestamp.
     *
     * @param context Application context.
     */
    suspend fun setInAppLastOpenTime(context: Context) {
        try {
            val appContext = context.applicationContext

            appContext.inAppDataStore.edit { preferences ->
                preferences[IN_APP_LAST_OPEN_TIME_KEY] =
                    System.currentTimeMillis()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("setInAppLastOpenTime", e)
        }
    }

    /**
     * Returns the last in-app display timestamp.
     *
     * @param context Application context.
     * @return Last display timestamp in milliseconds, or `0` if unknown.
     */
    suspend fun getInAppLastOpenTime(context: Context): Long {
        return try {
            context.applicationContext.inAppDataStore.data
                .first()[IN_APP_LAST_OPEN_TIME_KEY] ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getInAppLastOpenTime", e)
            0
        }
    }

    /**
     * Clears all in-app module persistent storage.
     *
     * @param context Application context.
     */
    suspend fun clearPreferences(context: Context) {
        try {
            context.applicationContext.inAppDataStore.edit {
                it.clear()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("clearPreferences", e)
        }
    }
}
