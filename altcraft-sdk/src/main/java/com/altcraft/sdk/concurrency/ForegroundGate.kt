package com.altcraft.sdk.concurrency

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import com.altcraft.sdk.sdk_events.Events.error

/**
 * ForegroundGate — detects when the app enters foreground.
 *
 * `foregroundCallback` invokes the callback immediately if already active,
 * or once lifecycle reaches STARTED. Safe from any thread, deduplicates calls.
 */
object ForegroundGate {

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hasSubscription = AtomicBoolean(false)

    /**
     * Registers a callback to be invoked when the app enters foreground,
     * or immediately (posted to Main) if it's already in foreground.
     *
     * Non-blocking; safe from any thread.
     * If a wait is already in-flight, subsequent calls are ignored.
     */
    fun foregroundCallback(callback: (Boolean) -> Unit) {
        // Drop duplicates while a previous wait is running
        if (!hasSubscription.compareAndSet(false, true)) return

        internalScope.launch {
            try {
                val ok = awaitForegroundInternal()
                withContext(Dispatchers.Main.immediate) {
                    try {
                        callback(ok)
                    } catch (e: Exception) {
                        error("foregroundCallback", e)
                    }
                }
            } finally {
                hasSubscription.set(false)
            }
        }
    }

    /**
    Always waits for foreground while the process is alive.
    Single safe retry on the next main tick.
     */
    private suspend fun awaitForegroundInternal(): Boolean =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { cont ->
                fun install(lc: Lifecycle?): Boolean {
                    if (lc == null) return false
                    if (lc.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        if (cont.isActive) cont.resume(true)
                        return true
                    }
                    val observer = object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            owner.lifecycle.removeObserver(this)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                    cont.invokeOnCancellation {
                        try {
                            lc.removeObserver(observer)
                        } catch (_: Throwable) {
                        }
                    }
                    return try {
                        lc.addObserver(observer)
                        true
                    } catch (_: Throwable) {
                        if (cont.isActive) cont.resume(false)
                        true
                    }
                }

                try {
                    val lcNow = try {
                        ProcessLifecycleOwner.get().lifecycle
                    } catch (_: Throwable) {
                        null
                    }
                    if (install(lcNow)) return@suspendCancellableCoroutine

                    mainHandler.post {
                        if (!cont.isActive) return@post
                        val lcRetry = try {
                            ProcessLifecycleOwner.get().lifecycle
                        } catch (_: Throwable) {
                            null
                        }
                        if (!install(lcRetry) && cont.isActive) cont.resume(false)
                    }
                } catch (_: Throwable) {
                    if (cont.isActive) cont.resume(false)
                }
            }
        }
}