package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RestrictTo
import com.altcraft.sdk.util.log.Log.error
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * `SubFunction` provides core SDK helpers for logging, notifications,
 * service checks, permissions, and asset handling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SubFunction {

    /**
     * Generates a random UUID string.
     */
    fun uuid() = UUID.randomUUID().toString()

    /** Returns the current system time in milliseconds. */
    fun time() = System.currentTimeMillis()

    /**
     * Generates a nano12 string.
     */
    internal fun nano12() = Nano12.getNano12()

    /**
     * 1500 ms delay in the background
     */
    internal suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(
            1.5.seconds
        )
    }

    /**
     * Checks whether the map contains at least one non-primitive value.
     *
     * @param input The input map with possibly mixed or null values.
     * @return `true` if at least one value is not a primitive, `false` if all values are
     * primitives or input is null.
     */
    fun fieldsIsObjects(input: Map<String, Any?>?): Boolean {
        if (input.isNullOrEmpty()) return false

        val values = input.values.filterNotNull()

        return if (values.isEmpty()) false else values.any {
            it !is String && it !is Number && it !is Boolean
        }
    }

    /**
     * Determines whether the app is currently in the foreground.
     *
     * The check is based on two sources:
     * 1. [ProcessLifecycleOwner] — the application's lifecycle state.
     * 2. [ActivityManager.RunningAppProcessInfo] — the system process importance level.
     *
     * @return `true` if the app is in the foreground, `false` otherwise.
     */
    fun isAppInForegrounded(): Boolean {
        return try {
            val lifecycleState = ProcessLifecycleOwner.get().lifecycle.currentState
            val isLifecycleForeground = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
            val processInfo = ActivityManager.RunningAppProcessInfo().apply {
                ActivityManager.getMyMemoryState(this)
            }
            val isProcessForeground = processInfo.importance == IMPORTANCE_FOREGROUND ||
                    processInfo.importance == IMPORTANCE_VISIBLE

            isLifecycleForeground || isProcessForeground
        } catch (e: Exception) {
            error("isAppInForegrounded", e)
            false
        }
    }

    /**
     * Determines the availability of the Internet connection.
     *
     * This function checks the current state of the device's network connectivity
     * to determine if it is connected to the internet through Wi-Fi, cellular data, or Ethernet.
     *
     * @param context The context used to access the system connectivity service.
     * @return `true` if an active internet connection is available; `false` otherwise.
     */
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            val network = (cm as ConnectivityManager).activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            error("isOnline", e)
            false
        }
    }

    /** Returns `true` if the string starts like a JSON object or array. */
    internal fun String?.isJsonString(): Boolean {
        if (this.isNullOrBlank()) return false
        val trimmed = this.trimStart()

        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }
}
