package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.altcraft.sdk.sdk_events.Events.error
import androidx.core.content.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.altcraft.sdk.json.Converter.json
import kotlinx.serialization.encodeToString
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Stores and retrieves SDK-related data in persistent device storage using `SharedPreferences`.
 *
 * Used for saving tokens, counters, and other small state values required by the SDK
 * between application launches.
 */
internal object Preferenses {
    private const val TAG = "Altcraft_SDK"
    internal const val TOKEN_KEY: String = "${TAG}_TOKEN"
    internal const val MESSAGE_ID_KEY: String = "${TAG}_MESSAGE_ID"

    private val Context.manualTokenDataStore by preferencesDataStore(
        "${TAG}_MANUAL_TOKEN_STORE"
    )
    private val MANUAL_TOKEN_KEY = stringPreferencesKey(
        "${TAG}_MANUAL_TOKEN_KEY"
    )

    /**
     * Returns the `SharedPreferences` object associated with the application for storing and
     * retrieving data.
     *
     * @param context The context of the application, used to access the `SharedPreferences`.
     * @return The `SharedPreferences` instance for the SDK.
     */
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
    }

    /**
     * Saves a manual push token to DataStore (or clears it if params are empty).
     *
     * @param context Android context used to access the DataStore.
     * @param provider Push provider id (e.g., FCM/HMS/RuStore). If null/empty,
     * stored token is removed.
     * @param token Push token value. If null/empty, stored token is removed.
     */
    fun setPushToken(context: Context, provider: String?, token: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data: String? =
                    if (provider.isNullOrEmpty() || token.isNullOrEmpty()) null
                    else json.encodeToString(DataClasses.TokenData(provider, token))

                context.manualTokenDataStore.edit { prefs ->
                    if (data == null) prefs.remove(MANUAL_TOKEN_KEY)
                    else prefs[MANUAL_TOKEN_KEY] = data
                }
            } catch (e: Exception) {
                error("setPushToken", e)
            }
        }
    }

    /**
     * Reads a manual push token from DataStore.
     * Returns immediately if a token is already stored, otherwise waits up to 5 seconds
     * for a non-empty value to appear.
     *
     * @param context Android context used to access the DataStore.
     * @return Parsed TokenData, or null if missing/empty, timed out, or on error.
     */
    suspend fun getManualToken(context: Context): DataClasses.TokenData? {
        return try {
            val current = context.manualTokenDataStore.data.map {
                it[MANUAL_TOKEN_KEY]
            }.first()

            val tokenJson: String? = if (!current.isNullOrEmpty()) {
                current
            } else {
                withTimeoutOrNull(5_000) {
                    context.manualTokenDataStore.data
                        .map { it[MANUAL_TOKEN_KEY] }
                        .distinctUntilChanged()
                        .firstOrNull { !it.isNullOrEmpty() }
                }
            }

            tokenJson?.let {
                json.decodeFromString<DataClasses.TokenData>(it)
            }
        } catch (e: Exception) {
            error("getManualToken", e)
            null
        }
    }

    /**
     * Clears manual push token from DataStore.
     *
     * @param context The application context.
     */
    suspend fun clearManualToken(context: Context) {
        try {
            context.manualTokenDataStore.edit { it.remove(MANUAL_TOKEN_KEY) }
        } catch (e: Exception) {
            error("clearManualToken", e)
        }
    }

    /**
     * Saves the serialized [DataClasses.TokenData] into SharedPreferences as a JSON string.
     *
     *
     * @param context The application context.
     * @param tokenData The [DataClasses.TokenData] object to store.
     * If `null`, stores `null` JSON.
     */
    @SuppressLint("ApplySharedPref")
    fun setCurrentToken(context: Context, tokenData: DataClasses.TokenData?) {
        try {
            val json = json.encodeToString(tokenData ?: return)

            getPreferences(context).edit(commit = true) {
                putString(TOKEN_KEY, json)
            }
        } catch (e: Exception) {
            error("setCurrentToken", e)
        }
    }

    /**
     * Retrieves and deserializes [DataClasses.TokenData] from SharedPreferences.
     *
     *
     * @param context The application context.
     * @return A deserialized [DataClasses.TokenData] instance or `null` if unavailable
     * or failed to parse.
     */
    fun getSavedPushToken(context: Context): DataClasses.TokenData? {
        return try {
            getPreferences(context).getString(TOKEN_KEY, null)?.let {
                json.decodeFromString<DataClasses.TokenData>(it)
            }
        } catch (e: Exception) {
            error("getSavedToken", e)
            null
        }
    }

    /**
     * Increments and returns the notification message ID.
     *
     * Reads the current value, increments it by 1, stores the new value,
     * and returns the incremented value.
     *
     * @param context Application context.
     * @return The next notification message ID (starting from 1).
     */
    fun getMessageId(context: Context): Int {
        return try {
            val next = getPreferences(context).getInt(MESSAGE_ID_KEY, 0) + 1
            getPreferences(context).edit { putInt(MESSAGE_ID_KEY, next) }
            next
        } catch (e: Exception) {
            error("getMessageId", e)
            1
        }
    }
}
