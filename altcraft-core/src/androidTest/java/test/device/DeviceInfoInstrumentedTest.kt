@file:Suppress("SpellCheckingInspection")

package test.device

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

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
 */
@RunWith(AndroidJUnit4::class)
class DeviceInfoInstrumentedTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    /** - test_1: Map contains mandatory keys with valid basic values. */
    @Test
    fun getDeviceFields_containsMandatoryKeys() {
        val map = DeviceInfo.getDeviceFields()

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

    /** - test_2: Device model and OS version are non-blank. */
    @Test
    fun getDeviceFields_includesDeviceInfo() {
        val map = DeviceInfo.getDeviceFields()
        val model = map["_device_model"] as String
        val osVer = map["_os_ver"] as String

        assertTrue("Model must not be blank", model.isNotBlank())
        assertTrue("OS version must not be blank", osVer.isNotBlank())
    }

    /** - test_3: _os_tz format is "+hhmm" or "-hhmm". */
    @Test
    fun getTimeZoneOffset_formatIsCorrect() {
        val tz = DeviceInfo.getDeviceFields()["_os_tz"] as String
        assertTrue("Timezone must match format", tz.matches(Regex("[+-][0-9]{4}")))
    }
}
