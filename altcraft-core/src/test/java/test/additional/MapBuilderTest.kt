@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.util.MapBuilder
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.data.models.CoreModels.PropertyModel
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.device.DeviceInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * MapBuilderTest
 *
 * Positive scenarios:
 *  - test_1: unionMaps() merges device, custom fields, and appInfo into a single map.
 */
class MapBuilderTest {

    private companion object {
        private const val KEY_APP_ID = "_app_id"
        private const val KEY_APP_IID = "_app_iid"
        private const val KEY_APP_VER = "_app_ver"

        private const val DEVICE_KEY = "_device"
        private const val DEVICE_VAL = "Pixel"

        private const val APP_ID = "id123"
        private const val APP_IID = "iid456"
        private const val APP_VER = "1.0.0"

        private const val MSG_DEVICE_MERGED = "Device field must be merged"
        private const val MSG_CUSTOM_MERGED = "Custom field must be merged"
        private const val MSG_APP_INFO_ID = "_app_id must be filled from config.appInfo"
        private const val MSG_APP_INFO_IID = "_app_iid must be filled from config.appInfo"
        private const val MSG_APP_INFO_VER = "_app_ver must be filled from config.appInfo"
    }

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.i(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.d(any(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: unionMaps() merges device, custom fields and appInfo. */
    @Test
    fun `unionMaps merges all fields correctly`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val deviceFields = mapOf(DEVICE_KEY to DEVICE_VAL)
        val customFields = mapOf("custom" to true)
        val appInfo = PropertyModel.AppInfo(APP_ID, APP_IID, APP_VER)

        val config = ConfigurationEntity(
            id = 1,
            apiUrl = "https://api.test",
            rToken = null,
            appInfo = appInfo
        )

        mockkObject(DeviceInfo)
        every { DeviceInfo.getDeviceFields() } returns deviceFields

        mockkObject(ConfigManager)
        coEvery { ConfigManager.getConfig(any()) } returns config

        val result = MapBuilder.unionMaps(context, customFields)

        assertEquals(MSG_DEVICE_MERGED, DEVICE_VAL, result[DEVICE_KEY])
        assertEquals(MSG_CUSTOM_MERGED, true, result["custom"])
        assertEquals(MSG_APP_INFO_ID, APP_ID, result[KEY_APP_ID])
        assertEquals(MSG_APP_INFO_IID, APP_IID, result[KEY_APP_IID])
        assertEquals(MSG_APP_INFO_VER, APP_VER, result[KEY_APP_VER])
    }
}
