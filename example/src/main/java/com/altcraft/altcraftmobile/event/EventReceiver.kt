package com.altcraft.altcraftmobile.event

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class EventReceiver private constructor(appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val events: SharedFlow<Event> =
        callbackFlow {
            AltcraftSDK.sdkEvents.subscription.subscribe { event ->
                trySend(event).isSuccess
            }
            awaitClose {
                AltcraftSDK.sdkEvents.subscription.unsubscribe()
            }
        }.buffer(capacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .shareIn(
                scope = scope,
                started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                replay = 64
            )
    //.onStart { emit(welcomeEvent) }

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