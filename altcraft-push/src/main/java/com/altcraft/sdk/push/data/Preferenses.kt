package com.altcraft.sdk.push.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.altcraft.sdk.push.util.Log.error
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Stores and retrieves Push module data in persistent storage.
 *
 * Manual tokens are stored in DataStore. The currently selected push token
 * is stored in SharedPreferences.
 */
internal object Preferenses {
    private const val TAG = "Altcraft_Push"
    private const val PREFERENCES_NAME = "${TAG}_PREFERENCES"
    private const val MANUAL_TOKEN_STORE = "${TAG}_MANUAL_TOKEN_STORE"
    internal const val MESSAGE_ID_KEY: String = "${TAG}_MESSAGE_ID"
    private const val TOKEN_KEY = "${TAG}_TOKEN"

    private val Context.manualTokenDataStore by preferencesDataStore(
        MANUAL_TOKEN_STORE
    )

    private val MANUAL_TOKEN_KEY = stringPreferencesKey(
        "${TAG}_MANUAL_TOKEN_KEY"
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** Serializes notification ID increments across threads. */
    private val messageIdLock = Any()

    /**
     * Returns Push module SharedPreferences.
     *
     * @param context Application context.
     * @return SharedPreferences instance.
     */
    private fun getPreferences(
        context: Context
    ): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME, Context.MODE_PRIVATE
        )
    }

    /**
     * Saves a manual push token or removes it when provider or token is empty.
     *
     * @param context Application context.
     * @param provider Push provider identifier.
     * @param token Push token.
     */
    suspend fun setPushToken(
        context: Context,
        provider: String?,
        token: String?
    ) {
        val appContext = context.applicationContext

        try {
            val tokenData = if (provider.isNullOrEmpty() || token.isNullOrEmpty())
                null else TokenData(provider, token)


            appContext.manualTokenDataStore.edit { preferences ->
                if (tokenData == null) preferences.remove(MANUAL_TOKEN_KEY)
                else preferences[MANUAL_TOKEN_KEY] = json.encodeToString(
                    tokenData
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("setPushToken", e)
        }
    }

    /**
     * Returns the manually assigned push token.
     *
     * Returns immediately if a token already exists. Otherwise, waits up to
     * five seconds for a non-empty value.
     *
     * @param context Application context.
     * @return Manual token, or `null` if unavailable.
     */
    suspend fun getManualToken(
        context: Context
    ): TokenData? {
        return try {
            val dataStore = context.applicationContext.manualTokenDataStore
            val current = dataStore.data.map { it[MANUAL_TOKEN_KEY] }.first()

            val tokenJson =
                if (!current.isNullOrEmpty()) current else withTimeoutOrNull(
                    5.seconds
                ) {
                    dataStore.data.map { it[MANUAL_TOKEN_KEY] }.firstOrNull {
                        !it.isNullOrEmpty()
                    }
                }

            tokenJson?.let {
                json.decodeFromString<TokenData>(it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("getManualToken", e)
            null
        }
    }

    /**
     * Saves the current push token.
     *
     * @param context Application context.
     * @param tokenData Token to save.
     */
    @SuppressLint("ApplySharedPref")
    fun setCurrentToken(context: Context, tokenData: TokenData?) {
        try {
            val value = if (tokenData != null) json.encodeToString(
                tokenData
            ) else return

            getPreferences(context).edit(commit = true) {
                putString(TOKEN_KEY, value)
            }
        } catch (e: Exception) {
            error("setCurrentToken", e)
        }
    }

    /**
     * Returns the previously saved push token.
     *
     * @param context Application context.
     * @return Saved token, or `null` if unavailable.
     */
    fun getSavedPushToken(context: Context): TokenData? {
        return try {
            getPreferences(context).getString(TOKEN_KEY, null)
                ?.let {
                    json.decodeFromString<TokenData>(it)
                }
        } catch (e: Exception) {
            error("getSavedPushToken", e)
            null
        }
    }

    /**
     * Increments and returns the notification message ID.
     *
     * Reads the current value, increments it by 1, stores the new value,
     * and returns the incremented value. The read-modify-write sequence is
     * synchronized so concurrently processed pushes never receive the same
     * ID and never overwrite each other in the notification shade.
     *
     * @param context Application context.
     * @return The next notification message ID (starting from 1).
     */
    fun getMessageId(context: Context): Int {
        return try {
            synchronized(messageIdLock) {
                val preferences = getPreferences(context)
                val next = preferences.getInt(MESSAGE_ID_KEY, 0) + 1
                preferences.edit { putInt(MESSAGE_ID_KEY, next) }
                next
            }
        } catch (e: Exception) {
            error("getMessageId", e)
            1
        }
    }

    /**
     * Clears all Push module persistent storage.
     *
     * @param context Application context.
     */
    @SuppressLint("ApplySharedPref")
    suspend fun clearPreferences(context: Context) {
        try {
            val appContext = context.applicationContext

            getPreferences(appContext).edit(commit = true) {
                clear()
            }
            appContext.manualTokenDataStore.edit { it.clear() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("clearPreferences", e)
        }
    }
}