package com.altcraft.sdk.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.Manifest
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.altcraft.sdk.sdk_events.Events.error
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.altcraft.sdk.data.Constants.AC_PUSH
import com.altcraft.sdk.data.Constants.LOG_NULL
import com.altcraft.sdk.data.Constants.LOG_TAG
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * `SubFunction` provides core SDK helpers for logging, notifications,
 * service checks, permissions, and asset handling.
 */
internal object SubFunction {

    /**
     * function outputs SDK logs with the altcraft_lib tag.
     *
     * @param log The message to be logged. If `null`, a default message is logged.
     */
    fun logger(log: String?) = Log.d(LOG_TAG, log ?: LOG_NULL)

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

        return if (values.isEmpty()) return false else values.any { value ->
            value !is String && value !is Number && value !is Boolean
        }
    }

    /**
     * Loads a bitmap image from the assets folder.
     *
     * This function attempts to open a file located in the `assets` folder of the application
     * and convert it into a `Bitmap` object. If the file cannot be found or there is an error
     * during the loading process, the function will return `null`.
     *
     * @param context The context used to access application assets.
     * @param fileName The name of the image file in the `assets` folder to be loaded as a bitmap.
     * @return A `Bitmap` object if the image is successfully loaded, or `null` if an error occurs.
     */
    fun fromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            val stream = context.assets.open(fileName)
            BitmapFactory.decodeStream(stream)
        } catch (e: IOException) {
            error("fromAssets", e)
            null
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
     * Checks if the app has notification permission.
     *
     * @param context The context used to access the system services.
     * @return `true` if notifications are enabled; `false` otherwise.
     */
    fun checkingNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .areNotificationsEnabled()
        } else NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Checks if the specified service is running.
     *
     * @param context The context used to access the system services.
     * @param serviceClass The class object of the service to check.
     * @return `true` if the service is running; `false` otherwise.
     */
    @Suppress("DEPRECATION")
    internal fun isServiceRunning(context: Context, serviceClass: Class<out Any>): Boolean {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE).any { serviceClass.name == it.service.className }
    }

    /**
     * Requests notification permission from the user.
     *
     * This function prompts the user to grant the app permission to post notifications.
     * This request is only necessary on Android versions TIRAMISU and above,
     * where notification permissions need to be explicitly granted.
     *
     * @param context The context used to check the current permission status.
     * @param activity The activity from which the permission request should be made.
     */
    fun requestNotificationPermission(context: Context, activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != 0)
                ActivityCompat.requestPermissions(activity, arrayOf(permission), 101)
        }
    }

    /**
     * Determines the availability of the Internet connection.
     *
     * This function checks the current state of the device's network connectivity
     * to determine if it is connected to the internet through WiFi, cellular data, or Ethernet.
     *
     * @param context The context used to access the system connectivity service.
     * @return `true` if an active internet connection is available; `false` otherwise.
     */
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            error("isOnline", e)
            false
        }
    }

    /**
     * Determines if a given push notification is an Altcraft push notification.
     *
     * This function checks the data payload of a `RemoteMessage` to see if it contains the key
     * "_ac_push", which identifies it as a push notification from Altcraft.
     *
     * @param message The `RemoteMessage` object representing the push notification received.
     * @return `true` if the message is an Altcraft push notification; `false` otherwise.
     */
    fun altcraftPush(message: Map<String, String>) = message.containsKey(AC_PUSH)


    /** Returns `true` if the string starts like a JSON object or array. */
    fun String?.isJsonString(): Boolean {
        if (this.isNullOrBlank()) return false
        val trimmed = this.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    /**
     * Safely parses a hex color string into an Int color.
     *
     * @param value Color string.
     * @return Parsed color or [Color.BLACK] if an error occurs.
     */
    fun getIconColor(value: String?) = try {
        value?.toColorInt() ?: Color.BLACK
    } catch (_: Exception) {
        Color.BLACK
    }

    /**
     * Returns true if the input contains an <html> (or </html>) tag.
     * - Case-insensitive
     * - Allows whitespace and attributes (e.g., <html lang="en">)
     */
    fun stringContainsHtml(input: String?): Boolean {
        val htmlTag = Regex("""(?is)<\s*/?\s*html(?:\s[^>]*)?>""")
        return htmlTag.containsMatchIn(input ?: return false)
    }

    /**
     * Generates unique request codes for `PendingIntent`.
     *
     * Uses a combination of UID hash and atomic counter to minimize collisions
     * and ensure thread-safe incrementation.
     */
    object UniqueCodeGenerator {
        private val counter = AtomicInteger(0)

        /**
         * Produces a (mostly) unique, non-negative requestCode for PendingIntent.
         * Combines uid hash with an ever-increasing counter to reduce collisions.
         */
        fun uniqueCode(uid: String) =
            (uid.hashCode() xor (counter.getAndIncrement() * 31)).and(0x7FFFFFFF)
    }
}
