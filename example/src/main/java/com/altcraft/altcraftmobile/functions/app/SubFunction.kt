package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object SubFunction {
    internal fun logger(log: String?) = Log.d("altcraft_lib_app", log ?: "this log is null")

    /**
     * Loads a bitmap image from the given network URL using [HttpURLConnection].
     *
     * @param url The full URL of the image resource to download.
     * @return The decoded [Bitmap], or `null` if loading fails.
     */
    @WorkerThread
    internal fun loadImage(context: Context, url: String): Bitmap? {
        if (url.isEmpty()) return null

        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        return try {
            val u = URL(url)
            connection = (u.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 5_000
                readTimeout = 10_000
                doInput = true
                setRequestProperty(
                    "User-Agent",
                    "AltcraftSDK/1.0"
                )
            }
            connection.connect()

            val code = connection.responseCode
            if (code !in 200..299) {
                return null
            }

            input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (_: Exception) {
            null
        } finally {
            try {
                input?.close()
            } catch (_: Exception) {
            }
            connection?.disconnect()
        }
    }
}

/**
 * Utility object for handling push notification permission requests.
 *
 * Provides a single entry point [isGranted] to check or request the
 * POST_NOTIFICATIONS permission (Android 13+).
 *
 * Example usage inside an Activity:
 *
 * ```
 * NotificationPermissionHandler.isGranted(
 *     activity = this,
 *     onGranted = {
 *         AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(context = this)
 *     }
 * )
 * ```
 */
@Suppress("unused")
object NotificationPermissionHandler {
    /**
     * Checks whether the POST_NOTIFICATIONS permission is granted.
     *
     * @param activity The [ComponentActivity] used to register the permission launcher.
     * @param onGranted Callback invoked when permission is granted.
     * @param onDenied Optional callback invoked when permission is denied.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun isGranted(
        activity: ComponentActivity,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onGranted()
            return
        }

        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) onGranted() else onDenied?.invoke()
        }

        launcher.launch(permission)
    }
}