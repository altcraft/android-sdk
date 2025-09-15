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
 *  - test_1: getDeviceFields returns non-empty map with mandatory keys.
 *  - test_2: getDeviceFields includes device/OS information from Build and Locale.
 *  - test_3: getTimeZoneOffset returns a string in expected "+hhmm" format.
 *
 * Negative scenarios:
 *  - test_4: getAdvertisingIdInfo fails gracefully (returns null/false if not available).
 *
 * Notes:
 *  - Runs on Android (instrumented test).
 *  - AdvertisingId may be unavailable on emulators; in this case only basic fields are checked.
 */
@RunWith(AndroidJUnit4::class)
class DeviceInfoInstrumentedTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    /** test_1: getDeviceFields returns non-empty map with mandatory keys */
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

        assertEquals(Constants.ANDROID_OS, map["_os"])
        assertEquals(Constants.DEVICE_TYPE, map["_device_type"])
    }

    /** test_2: getDeviceFields includes device/OS information */
    @Test
    fun getDeviceFields_includesDeviceInfo() {
        val map = DeviceInfo.getDeviceFields(ctx)

        val model = map["_device_model"] as String
        val osVer = map["_os_ver"] as String

        assertTrue("Model must not be blank", model.isNotBlank())
        assertTrue("OS version must not be blank", osVer.isNotBlank())
    }

    /** test_3: getTimeZoneOffset returns "+hhmm" or "-hhmm" */
    @Test
    fun getTimeZoneOffset_formatIsCorrect() {
        val tz = DeviceInfo.getDeviceFields(ctx)["_os_tz"] as String
        assertTrue("Timezone must match format", tz.matches(Regex("[+-][0-9]{4}")))
    }

    /** test_4: Advertising ID is optional but must not crash */
    @Test
    fun getDeviceFields_handlesAdIdGracefully() {
        val map = DeviceInfo.getDeviceFields(ctx)
        // ad_id may be absent, but if present must be non-blank string
        if (map.containsKey("_ad_id")) {
            val adId = map["_ad_id"] as String
            assertTrue("Ad ID must not be blank", adId.isNotBlank())
        }
        // _ad_track always present, but may be true/false
        assertTrue(map.containsKey("_ad_track"))
    }
}
