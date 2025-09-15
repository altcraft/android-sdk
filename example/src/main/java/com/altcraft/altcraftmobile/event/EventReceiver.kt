package com.altcraft.altcraftmobile.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.data.DataClasses
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import java.util.Date

class EventReceiver private constructor(appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val welcomeEvent: DataClasses.Event = DataClasses.Event(
        function = "app",
        eventCode = 1,
        eventMessage = "Welcome to the Altcraft Android SDK test app!",
        eventValue = null,
        date = Date()
    )

    val events: SharedFlow<DataClasses.Event> =
        callbackFlow {
            AltcraftSDK.eventSDKFunctions.subscribe { event ->
                trySend(event).isSuccess
            }
            awaitClose {
                AltcraftSDK.eventSDKFunctions.unsubscribe()
            }
        }
            .buffer(capacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .onStart { emit(welcomeEvent) }
            .shareIn(
                scope = scope,
                started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                replay = 64
            )

    companion object {
        @Volatile
        private var INSTANCE: EventReceiver? = null

        /** Initializes EventReceiver singleton */
        fun init(context: Context): EventReceiver {
            val existing = INSTANCE
            if (existing != null) return existing
            return synchronized(this) {
                INSTANCE ?: EventReceiver(context.applicationContext).also { INSTANCE = it }
            }
        }

        /** Returns EventReceiver instance */
        fun get(): EventReceiver {
            return requireNotNull(INSTANCE) {
                "EventReceiver is not initialized. Call EventReceiver.init(context) in Application.onCreate()."
            }
        }
    }
}