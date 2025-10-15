package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.altcraft.sdk.sdk_events.Events.error
import androidx.core.content.edit
import com.altcraft.sdk.json.Converter.json
import kotlinx.serialization.encodeToString

/**
 * The `Constants` object manages keys and functions for setting and retrieving `SharedPreferences`
 * values.
 * It is designed to store and retrieve data in persistent storage on the device, specifically for
 * SDK settings and states.
 */
internal object Preferenses {
    private const val TAG = "Altcraft_SDK"
    internal const val TOKEN_KEY: String = "${TAG}_TOKEN"
    internal const val MANUAL_TOKEN_KEY: String = "${TAG}_MANUAL_TOKEN_KEY"
    internal const val MESSAGE_ID_KEY: String = "${TAG}_MESSAGE_ID"

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
     * Stores a manually obtained push token in SharedPreferences.
     *
     * This method is intended to be used inside `onNewToken()` callbacks
     * of push notification providers (e.g., FCM, HMS).
     *
     *
     * @param context The application context.
     * @param provider The name of the push provider.
     * @param token The raw token string received from the provider.
     */
    @SuppressLint("ApplySharedPref")
    fun setPushToken(context: Context, provider: String, token: String) {
        try {
            val json = json.encodeToString(DataClasses.TokenData(provider, token))
            getPreferences(context).edit(commit = true) {
                putString(MANUAL_TOKEN_KEY, json)
            }
        } catch (e: Exception) {
            error("setPushToken", e)
        }
    }

    /**
     * Retrieves the manually stored push token from SharedPreferences.
     *
     * This method deserializes the stored JSON string into a [DataClasses.TokenData] object.
     * Intended to access a token previously saved in the `onNewToken()` callback.
     *
     * Returns `null` if no token is stored or deserialization fails.
     *
     * @param context The application context.
     * @return The [DataClasses.TokenData] or `null` if unavailable.
     */
    fun getManualToken(context: Context): DataClasses.TokenData? {
        return try {
            getPreferences(context).getString(MANUAL_TOKEN_KEY, null)?.let {
                json.decodeFromString<DataClasses.TokenData>(it)
            }
        } catch (e: Exception) {
            error("getManualToken", e)
            null
        }
    }

    /**
     * Saves the serialized [DataClasses.TokenData] into SharedPreferences as a JSON string.
     *
     *
     * @param context The application context.
     * @param tokenData The [DataClasses.TokenData] object to store. If `null`, stores `null` JSON.
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
     * @return A deserialized [DataClasses.TokenData] instance or `null` if unavailable or failed
     * to parse.
     */
    fun getSavedToken(context: Context): DataClasses.TokenData? {
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
     * Retrieves the current notification message ID and increments it for the next usage.
     *
     * This method fetches the current message ID from the shared preferences.
     * It then increments the ID by 1, updates the shared preferences with the new value,
     * and returns the original ID.
     *
     * @param context The application context used to access shared preferences.
     * @return The current notification message ID.
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