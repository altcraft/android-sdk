package com.altcraft.sdk.device

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.altcraft.sdk.data.Constants.ANDROID_OS
import com.altcraft.sdk.data.Constants.DEVICE_TYPE
import com.altcraft.sdk.sdk_events.Events.error
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import com.altcraft.sdk.data.Constants.CLS_ADS_ID_CLIENT
import com.altcraft.sdk.data.Constants.CLS_ADS_ID_INFO
import com.altcraft.sdk.data.Constants.M_GET_ID
import com.altcraft.sdk.data.Constants.M_GET_INFO
import com.altcraft.sdk.data.Constants.M_IS_LIMIT

/** Collects device information and system attributes. */
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
     * Retrieves the advertising ID and the user's preference for ad tracking.
     *
     * Safe on API < 26 without core desugaring: the AdsIdentifier class is never loaded.
     *
     * @param context Application context used to access Google Play Services.
     * @return A pair where:
     *   - first = the advertising ID string (or null if unavailable),
     *   - second = true if tracking is allowed, false otherwise.
     */
    private fun getAdvertisingIdInfo(context: Context): Pair<String?, Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null to false

        return try {
            val client = Class.forName(CLS_ADS_ID_CLIENT)
            val getInfo = client.getMethod(M_GET_INFO, Context::class.java)
            val infoObj = getInfo.invoke(null, context)

            val infoCls = Class.forName(CLS_ADS_ID_INFO)
            val id = infoCls.getMethod(M_GET_ID).invoke(infoObj) as String?
            val limit = infoCls.getMethod(M_IS_LIMIT).invoke(infoObj) as Boolean

            id to !limit
        } catch (t: Throwable) {
            error("getAdvertisingIdInfo", t)
            null to false
        }
    }

    /**
     * Retrieves the time zone offset in the format "+hhmm" or "-hhmm".
     *
     * @return A string representing the time zone offset in the specified format.
     *         Returns "+0000" in case of an error.
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

    /**
     * Returns mobile-event timezone offset in minutes as a signed integer.
     *
     * On error: returns 0.
     */
    fun getTimeZoneForMobEvent(): Int {
        return try {
            val timeZone: TimeZone = TimeZone.getDefault()
            val offsetInMillis: Int = timeZone.rawOffset
            val hours: Int = offsetInMillis / (1000 * 60 * 60)
            val minutes: Int = (offsetInMillis / (1000 * 60)) % 60
            -(hours * 60 + minutes)
        } catch (e: Exception) {
            error("getTimeZoneForMobEvent", e)
            0
        }
    }
}