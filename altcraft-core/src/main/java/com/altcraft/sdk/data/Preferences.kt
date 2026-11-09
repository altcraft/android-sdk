
package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Stores and retrieves the current RToken.
 */
internal object Preferences {

    private const val PREFERENCES_NAME = "Altcraft_Core"
    private const val RTOKEN_KEY = "Altcraft_Core_RTOKEN"

    /**
     * Returns Core module SharedPreferences.
     *
     * @param context Application context.
     * @return SharedPreferences instance.
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME, Context.MODE_PRIVATE
        )
    }

    /**
     * Saves the current RToken.
     *
     * @param context Application context.
     * @param rToken RToken to save.
     */
    fun setCurrentRToken(context: Context, rToken: String) {
        getPreferences(context).edit {
            putString(RTOKEN_KEY, rToken)
        }
    }

    /**
     * Returns the previously saved RToken.
     *
     * @param context Application context.
     * @return Saved RToken, or `null` if unavailable.
     */
    fun getCurrentRToken(context: Context): String? {
        return getPreferences(context).getString(
            RTOKEN_KEY, null
        )
    }

    /**
     * Removes the saved RToken.
     *
     * @param context Application context.
     */
    fun clearCurrentRToken(context: Context) {
        getPreferences(context).edit {
            remove(RTOKEN_KEY)
        }
    }
}
