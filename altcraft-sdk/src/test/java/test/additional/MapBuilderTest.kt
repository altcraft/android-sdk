@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.MapBuilder
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.MobileEventRequestData
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.device.DeviceInfo
import com.altcraft.sdk.interfaces.RequestData
import com.altcraft.sdk.data.Constants.RESPONSE_WITH_HTTP_CODE
import com.altcraft.sdk.data.Constants.UID
import com.altcraft.sdk.data.Constants.TYPE
import com.altcraft.sdk.data.Constants.NAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MapBuilderTest
 *
 * Positive scenarios:
 *  - test_1: createEventValue(PushEventRequestData) → contains "response_with_http_code", "uid", "type".
 *  - test_2: createEventValue(MobileEventRequestData) → contains "response_with_http_code", "name".
 *  - test_4: unionMaps() merges device, custom fields, and appInfo into a single map.
 *
 * Negative scenarios:
 *  - test_3: createEventValue(generic RequestData) → contains "response_with_http_code" only
 *  (no "uid"/"type"/"name").
 */
class MapBuilderTest {

    private companion object {
        private const val KEY_HTTP_CODE = RESPONSE_WITH_HTTP_CODE
        private const val KEY_UID = UID
        private const val KEY_TYPE = TYPE
        private const val KEY_NAME = NAME
        private const val KEY_APP_ID = "_app_id"
        private const val KEY_APP_IID = "_app_iid"
        private const val KEY_APP_VER = "_app_ver"

        private const val URL_EXAMPLE = "https://example.com"
        private const val UID_123 = "1234567890"
        private const val TYPE_DELIVERY = "delivery"
        private const val EVENT_NAME = "test_event"
        private const val SID_VALUE = "session123"
        private const val AUTH_BEARER = "Bearer token"
        private const val MATCHING_PUSH = "push"

        private const val DEVICE_KEY = "_device"
        private const val DEVICE_VAL = "Pixel"

        private const val APP_ID = "id123"
        private const val APP_IID = "iid456"
        private const val APP_VER = "1.0.0"

        private const val MSG_MUST_HAVE_HTTP_KEY = "Result must contain response_with_http_code key"
        private const val MSG_UID_PRESENT = "Result must contain uid for push event"
        private const val MSG_TYPE_PRESENT = "Result must contain type for push event"
        private const val MSG_NAME_PRESENT = "Result must contain name for mobile event"
        private const val MSG_UID_ABSENT = "Generic request must not contain uid"
        private const val MSG_TYPE_ABSENT = "Generic request must not contain type"
        private const val MSG_NAME_ABSENT = "Generic request must not contain name"
        private const val MSG_DEVICE_MERGED = "Device field must be merged"
        private const val MSG_CUSTOM_MERGED = "Custom field must be merged"
        private const val MSG_APP_INFO_ID = "_app_id must be filled from config.appInfo"
        private const val MSG_APP_INFO_IID = "_app_iid must be filled from config.appInfo"
        private const val MSG_APP_INFO_VER = "_app_ver must be filled from config.appInfo"
    }

    @Before
    fun setUp() {
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    /** - test_1: createEventValue(PushEventRequestData) includes "uid" and "type". */
    @Test
    fun `createEventValue with PushEventRequestData should return map with uid and type`() {
        val code = 200
        val response = DataClasses.Response(error = 0, errorText = "", profile = null)
        val requestData = PushEventRequestData(
            url = URL_EXAMPLE,
            time = System.currentTimeMillis(),
            type = TYPE_DELIVERY,
            uid = UID_123,
            authHeader = AUTH_BEARER,
            matchingMode = MATCHING_PUSH
        )

        val result = MapBuilder.createEventValue(code, response, requestData)

        assertTrue(MSG_MUST_HAVE_HTTP_KEY, result.containsKey(KEY_HTTP_CODE))
        assertEquals(MSG_UID_PRESENT, UID_123, result[KEY_UID])
        assertEquals(MSG_TYPE_PRESENT, TYPE_DELIVERY, result[KEY_TYPE])
        assertFalse(MSG_NAME_ABSENT, result.containsKey(KEY_NAME))
    }

    /** - test_2: createEventValue(MobileEventRequestData) includes "name". */
    @Test
    fun `createEventValue with MobileEventRequestData should return map with name`() {
        val code = 200
        val response = DataClasses.Response(error = 0, errorText = "", profile = null)
        val requestData = MobileEventRequestData(
            url = URL_EXAMPLE,
            sid = SID_VALUE,
            name = EVENT_NAME,
            authHeader = AUTH_BEARER
        )

        val result = MapBuilder.createEventValue(code, response, requestData)

        assertTrue(MSG_MUST_HAVE_HTTP_KEY, result.containsKey(KEY_HTTP_CODE))
        assertEquals(MSG_NAME_PRESENT, EVENT_NAME, result[KEY_NAME])
        assertFalse(MSG_UID_ABSENT, result.containsKey(KEY_UID))
        assertFalse(MSG_TYPE_ABSENT, result.containsKey(KEY_TYPE))
    }

    /** - test_3: createEventValue(generic RequestData) has no "uid"/"type"/"name". */
    @Test
    fun `createEventValue with generic RequestData should not include uid type or name`() {
        val code = 200
        val response: DataClasses.Response? = null
        val requestData = mockk<RequestData>(relaxed = true)

        val result = MapBuilder.createEventValue(code, response, requestData)

        assertTrue(MSG_MUST_HAVE_HTTP_KEY, result.containsKey(KEY_HTTP_CODE))
        assertFalse(MSG_UID_ABSENT, result.containsKey(KEY_UID))
        assertFalse(MSG_TYPE_ABSENT, result.containsKey(KEY_TYPE))
        assertFalse(MSG_NAME_ABSENT, result.containsKey(KEY_NAME))
    }

    /** - test_4: unionMaps() merges device, custom fields and appInfo. */
    @Test
    fun `unionMaps merges all fields correctly`() {
        val context = mockk<Context>()
        val deviceFields = mapOf(DEVICE_KEY to DEVICE_VAL)
        val customFields = mapOf("custom" to true)
        val appInfo = DataClasses.AppInfo(APP_ID, APP_IID, APP_VER)
        val config = ConfigurationEntity(
            id = 1,
            icon = null,
            apiUrl = "https://api.test",
            rToken = null,
            appInfo = appInfo,
            usingService = false,
            serviceMessage = null
        )

        mockkObject(DeviceInfo)
        every { DeviceInfo.getDeviceFields(context) } returns deviceFields

        val result = MapBuilder.unionMaps(context, config, customFields)

        assertEquals(MSG_DEVICE_MERGED, DEVICE_VAL, result[DEVICE_KEY])
        assertEquals(MSG_CUSTOM_MERGED, true, result["custom"])
        assertEquals(MSG_APP_INFO_ID, APP_ID, result[KEY_APP_ID])
        assertEquals(MSG_APP_INFO_IID, APP_IID, result[KEY_APP_IID])
        assertEquals(MSG_APP_INFO_VER, APP_VER, result[KEY_APP_VER])
    }
}