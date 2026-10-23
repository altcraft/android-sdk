package com.altcraft.sdk.device

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.os.Build
import com.altcraft.sdk.data.Constants.ANDROID_OS
import com.altcraft.sdk.data.Constants.DEVICE_TYPE
import com.altcraft.sdk.util.log.Log.error
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Collects device information and system attributes. */
internal object DeviceInfo {

    /**
     * Retrieves detailed device information.
     *
     * @return A map containing device information:
     * - `_os` — operating system name
     * - `_os_tz` — time zone
     * - `_os_language` — device language
     * - `_device_type` — device type
     * - `_device_model` — device model
     * - `_device_name` — device name
     * - `_os_ver` — OS version
     */
     fun getDeviceFields(): Map<String, Any> {
        return try {
            mutableMapOf<String, Any>().apply {
                val deviceModel = Build.MODEL
                val deviceName = Build.DEVICE
                val osVersion = Build.VERSION.RELEASE
                val timeZone = getTimeZoneOffset()
                val language = Locale.getDefault().language

                this["_os"] = ANDROID_OS
                this["_os_tz"] = timeZone
                this["_os_language"] = language
                this["_device_type"] = DEVICE_TYPE
                this["_device_model"] = deviceModel
                this["_device_name"] = deviceName
                this["_os_ver"] = osVersion
            }
        } catch (e: Exception) {
            error("getDeviceFields", e)
            emptyMap()
        }
    }

    /**
     * Retrieves the current time zone offset from UTC.
     *
     * @return The UTC offset as a string, for example `+0300` or `-0500`.
     * Returns `+0000` if an error occurs.
     */
    @SuppressLint("DefaultLocale")
    private fun getTimeZoneOffset(): String {
        return try {
            val timeZone: TimeZone = TimeZone.getDefault()
            val offsetInMillis: Int = timeZone.rawOffset
            val totalMinutes = offsetInMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = abs(totalMinutes) % 60
            val sign = if (totalMinutes >= 0) "+" else "-"
            String.format("%s%02d%02d", sign, abs(hours), minutes)
        } catch (e: Exception) {
            error("getTimeZoneOffset", e)
            "+0000"
        }
    }
}