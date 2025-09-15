package com.altcraft.sdk.device

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.altcraft.sdk.data.Constants.ANDROID_OS
import com.altcraft.sdk.data.Constants.DEVICE_TYPE
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.altcraft.sdk.events.Events.error
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

internal object DeviceInfo {

    /**
     * Retrieves detailed device information.
     *
     * This function collects various device attributes, including:
     * - Model, name, OS version.
     * - Time zone, language settings.
     * - Advertising ID and tracking permission.
     *
     * If an error occurs, an empty map is returned.
     *
     * @param context The application context required to access system services.
     * @return A map containing device information. Keys include:
     *  - `_os` (Operating System Name)
     *  - `_os_tz` (Time Zone)
     *  - `_ad_track` (Ad Tracking Permission)
     *  - `_os_language` (Device Language)
     *  - `_device_type` (Device Type)
     *  - `_device_model` (Device Model, if available)
     *  - `_device_name` (Device Name, if available)
     *  - `_os_ver` (OS Version, if available)
     *  - `_ad_id` (Advertising ID, if available)
     */
     fun getDeviceFields(context: Context): Map<String, Any> {
        return try {
            mutableMapOf<String, Any>().apply {
                val deviceModel = Build.MODEL
                val deviceName = Build.DEVICE
                val osVersion = Build.VERSION.RELEASE
                val timeZone = getTimeZoneOffset()
                val language = Locale.getDefault().language

                val (adId, adTrack) = getAdvertisingIdInfo(context)

                this["_os"] = ANDROID_OS
                this["_os_tz"] = timeZone
                this["_ad_track"] = adTrack
                this["_os_language"] = language
                this["_device_type"] = DEVICE_TYPE
                this["_device_model"] = deviceModel
                this["_device_name"] = deviceName
                this["_os_ver"] = osVersion

                if (adId != null) {
                    this["_ad_id"] = adId
                }
            }
        } catch (e: Exception) {
            error("getDeviceFields", e)
            emptyMap()
        }
    }

    /**
     * Retrieves the advertising ID and user preference for ad tracking.
     *
     * @param context The application context.
     * @return A pair containing the advertising ID (or null if unavailable) and a boolean indicating
     *         whether ad tracking is allowed (true if tracking is enabled, false otherwise).
     */
    private fun getAdvertisingIdInfo(context: Context): Pair<String?, Boolean> {
        return try {
            AdvertisingIdClient.getAdvertisingIdInfo(context).let {
                it.id to !it.isLimitAdTrackingEnabled
            }
        } catch (e: Exception) {
            error("getAdvertisingIdInfo", e)
            Pair(null, false)
        }
    }

    /**
     * Retrieves the time zone offset in the format "+hh_mm" or "-hh_mm".
     *
     * @return A string representing the time zone offset in the specified format.
     *         Returns "+0000" in case of an error.
     */
    @SuppressLint("DefaultLocale")
    private fun getTimeZoneOffset(): String {
        return try {
            val timeZone: TimeZone = TimeZone.getDefault()
            val offsetInMillis: Int = timeZone.rawOffset
            val hours = offsetInMillis / (1000 * 60 * 60)
            val minutes = (offsetInMillis / (1000 * 60)) % 60
            val sign = if (hours >= 0) "+" else "-"
            String.format("%s%02d%02d", sign, abs(hours), abs(minutes))
        } catch (e: Exception) {
            error("getTimeZoneOffset", e)
            "+0000"
        }
    }
}