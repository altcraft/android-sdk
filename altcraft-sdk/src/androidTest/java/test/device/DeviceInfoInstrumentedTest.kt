@file:Suppress("SpellCheckingInspection")

package test.device

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.device.DeviceInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DeviceInfoInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: getDeviceFields → map is not empty, contains mandatory keys, and basic values are valid.
 *  - test_2: getDeviceFields → device model and OS version are non-blank.
 *  - test_3: _os_tz format is "+hhmm" or "-hhmm".
 *  - test_4: Advertising ID is optional — when present it's non-blank; _ad_track is always present.
 *  - test_5: getTimeZoneForMobEvent is consistent with _os_tz (returns opposite sign offset in minutes).
 */
@RunWith(AndroidJUnit4::class)
class DeviceInfoInstrumentedTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    /** - test_1: Map contains mandatory keys with valid basic values. */
    @Test
    fun getDeviceFields_containsMandatoryKeys() {
        val map = DeviceInfo.getDeviceFields(ctx)

        assertTrue("Map must not be empty", map.isNotEmpty())
        assertTrue(map.containsKey("_os"))
        assertTrue(map.containsKey("_os_tz"))
        assertTrue(map.containsKey("_os_language"))
        assertTrue(map.containsKey("_device_type"))
        assertTrue(map.containsKey("_device_model"))
        assertTrue(map.containsKey("_device_name"))
        assertTrue(map.containsKey("_os_ver"))
        assertTrue(map.containsKey("_ad_track"))

        assertEquals(Constants.ANDROID_OS, map["_os"])
        assertEquals(Constants.DEVICE_TYPE, map["_device_type"])
        assertTrue("ad_track must be Boolean", map["_ad_track"] is Boolean)
    }

    /** - test_2: Device model and OS version are non-blank. */
    @Test
    fun getDeviceFields_includesDeviceInfo() {
        val map = DeviceInfo.getDeviceFields(ctx)
        val model = map["_device_model"] as String
        val osVer = map["_os_ver"] as String

        assertTrue("Model must not be blank", model.isNotBlank())
        assertTrue("OS version must not be blank", osVer.isNotBlank())
    }

    /** - test_3: _os_tz format is "+hhmm" or "-hhmm". */
    @Test
    fun getTimeZoneOffset_formatIsCorrect() {
        val tz = DeviceInfo.getDeviceFields(ctx)["_os_tz"] as String
        assertTrue("Timezone must match format", tz.matches(Regex("[+-][0-9]{4}")))
    }

    /** - test_4: _ad_id is optional (when present non-blank); _ad_track is always present. */
    @Test
    fun getDeviceFields_handlesAdIdGracefully() {
        val map = DeviceInfo.getDeviceFields(ctx)
        if (map.containsKey("_ad_id")) {
            val adId = map["_ad_id"] as String
            assertTrue("Ad ID must not be blank", adId.isNotBlank())
        }
        assertTrue(map.containsKey("_ad_track"))
    }

    /** - test_5: getTimeZoneForMobEvent returns minutes offset opposite to _os_tz. */
    @Test
    fun timeZoneForMobEvent_isConsistentWith_os_tz() {
        val map = DeviceInfo.getDeviceFields(ctx)
        val tzStr = map["_os_tz"] as String
        assertTrue("Timezone must match format", tzStr.matches(Regex("[+-][0-9]{4}")))

        val sign = if (tzStr[0] == '-') -1 else 1
        val hours = tzStr.substring(1, 3).toInt()
        val minutes = tzStr.substring(3, 5).toInt()
        val tzMinutes = sign * (hours * 60 + minutes)

        val mobEventMinutes = DeviceInfo.getTimeZoneForMobEvent()
        assertEquals(
            "getTimeZoneForMobEvent must be -_os_tz(in minutes)",
            -tzMinutes,
            mobEventMinutes
        )
    }
}