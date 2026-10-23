package com.altcraft.sdk.push.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.altcraft.sdk.push.data.Constants.AC_PUSH
import com.altcraft.sdk.util.SubFunction.isAppInForegrounded
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Provides utility functions used by the target module.
 */
internal object Utils {

    /**
     * Adds a short delay when the application is running in the background.
     *
     * The delay gives the application process time to stabilize before
     * pending push operations are processed.
     */
    suspend fun awaitInBackground() {
        if (!isAppInForegrounded()) delay(1.5.seconds)
    }

    /**
     * Checks whether a push notification belongs to Altcraft.
     *
     * The `RemoteMessage` payload is expected to be converted to `Map<String, String>`.
     * The message is considered an Altcraft push if it contains the `_ac_push` key.
     *
     * @param message Push payload converted from `RemoteMessage.data`.
     * @return `true` if this is an Altcraft push; otherwise `false`.
     */
    fun altcraftPush(message: Map<String, String>) = message.containsKey(
        AC_PUSH
    )

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
     * Loads a bitmap image from the application assets.
     *
     * @param context Context used to access application assets.
     * @param fileName Name of the image file.
     * @return Loaded bitmap, or `null` if loading fails.
     */
    fun fromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: IOException) {
            Log.error("fromAssets", e)
            null
        }
    }

    /**
     * Checks whether the app has notification permission.
     *
     * @param context Context used to check the permission.
     * @return `true` if notification permission is granted; otherwise `false`.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests notification permission on Android 13+.
     *
     * @param context Context used to check the permission.
     * @param activity Activity used to request the permission.
     */
    fun requestNotificationPermission(context: Context, activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != 0) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), 101)
            }
        }
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
         * Produces a unique, non-negative requestCode for PendingIntent.
         * Combines uid hash with an ever-increasing counter to reduce collisions.
         */
        fun uniqueCode(uid: String) = (uid.hashCode() xor (counter.getAndIncrement() * 31))
            .and(0x7FFFFFFF)
    }
}