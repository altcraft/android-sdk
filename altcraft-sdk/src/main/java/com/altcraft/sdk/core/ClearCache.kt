package com.altcraft.sdk.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.services.manager.ServiceManager.stopService
import com.altcraft.sdk.data.Preferenses.getPreferences
import com.altcraft.sdk.events.EventList.sdkCleared
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.events.Events.event
import com.altcraft.sdk.workers.coroutine.CancelWork.cancelCoroutineWorkersTask
import com.altcraft.sdk.workers.periodical.CommonFunctions.cancelPeriodicalWorkersTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.altcraft.sdk.config.ConfigSetup.configuration
import com.altcraft.sdk.push.Core.pushControl
import com.altcraft.sdk.push.token.TokenManager.tokens
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Clears SDK data from DB, preferences, and active workers.
 *
 * - Removes data from all SDK tables.
 * - Cancels background workers and services.
 * - Clears related SharedPreferences and flags.
 */
internal object ClearCache {

    /**
     * Performs full cleanup of SDK-related data.
     *
     * @param context App context.
     * @param onComplete Callback on completion.
     */
    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    fun clear(context: Context, onComplete: () -> Unit) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                configuration = null
                pushControl = AtomicBoolean(false)
                tokens.clear()

                // Get the instance of the SDK database
                val room = SDKdb.getDb(context)

                // Clear all entries from database
                room.request().deleteConfig()
                room.request().deleteAllSubscriptions()
                room.request().deleteAllPushEvents()

                // Cancel any ongoing workers or tasks
                suspendCoroutine { continuation ->
                    cancelCoroutineWorkersTask(context) {
                        continuation.resume(Unit)
                    }
                }

                // Intent to close services
                stopService(context, SUBSCRIBE_SERVICE)
                stopService(context, UPDATE_SERVICE)

                // Clear specific preferences related to SDK
                getPreferences(context).edit {
                    remove(Preferenses.TOKEN_KEY)
                    remove(Preferenses.MANUAL_TOKEN_KEY)
                    remove(Preferenses.MESSAGE_ID_KEY)
                }

                cancelPeriodicalWorkersTask(context)

                //Switch to the main thread to notify completion
                withContext(Dispatchers.Main) {
                    event("clear", sdkCleared)
                    onComplete()
                }
            }
        } catch (e: Exception) {
            error("clear", e)
        }
    }
}