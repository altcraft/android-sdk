package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.cancellation.CancellationException
import com.altcraft.sdk.util.log.Log.error

/**
 * ForegroundGate — detects when the app enters foreground.
 *
 * `foreground` invokes the callback immediately if already active,
 * or once lifecycle reaches STARTED. Safe from any thread, deduplicates calls.
 */
internal object ForegroundBarrier {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hasSubscription = AtomicBoolean(false)

    /**
     * Registers a callback to be invoked when the app enters foreground,
     * or immediately (posted to a background dispatcher) if it's already
     * in foreground.
     *
     * Non-blocking; safe from any thread.
     * If a wait is already in-flight, subsequent calls are ignored.
     *
     * The callback runs on [Dispatchers.IO]: SDK initialization performed
     * inside it does block I/O (Room, WorkManager, providers) and must
     * never occupy the main thread. Foreground observation
     * (ProcessLifecycleOwner) stays on Main inside [awaitForegroundInternal].
     */
    fun foreground(block: suspend (Boolean) -> Unit) {
        if (!hasSubscription.compareAndSet(false, true)) return
        SDKScope.default.launch {
            try {
                val ok = awaitForegroundInternal()
                try {
                    withContext(Dispatchers.IO) {
                        block(ok)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error("foregroundBlock", e)
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
                /** Resumes the waiter immediately or on the next start event. */
                fun install(lc: Lifecycle?): Boolean {
                    if (lc == null) return false
                    if (lc.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        if (cont.isActive) cont.resume(true)
                        return true
                    }
                    val observer = object : DefaultLifecycleObserver {
                        /** Resumes the waiter when the process reaches the started state. */
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