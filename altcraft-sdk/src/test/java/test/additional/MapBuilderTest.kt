package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.MapBuilder
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.device.DeviceInfo
import com.altcraft.sdk.interfaces.RequestData
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
 *  - test_1: Valid PushEventRequestData → createEventValue includes generic key
 *            "response_with_http_code" and also specific keys "uid" and "type"
 *            with expected values.
 *  - test_3: unionMaps merges fields from three sources:
 *            (a) DeviceInfo (e.g., "_device"),
 *            (b) custom map (e.g., "custom"),
 *            (c) config.appInfo (→ "_app_id", "_app_iid", "_app_ver").
 *
 * Negative scenarios:
 *  - test_2: Generic RequestData (non-push event) → createEventValue still
 *            contains "response_with_http_code" but does NOT include "uid" or "type".
 *
 * Notes:
 *  - android.util.Log is mocked in @Before to avoid "not mocked" issues in JVM tests.
 *  - Assertion messages and keys are defined as constants above for consistency.
 */

// ---------- Keys / test data ----------
private const val KEY_HTTP_CODE = "response_with_http_code"
private const val KEY_UID = "uid"
private const val KEY_TYPE = "type"
private const val KEY_APP_ID = "_app_id"
private const val KEY_APP_IID = "_app_iid"
private const val KEY_APP_VER = "_app_ver"

private const val URL_EXAMPLE = "https://example.com"
private const val UID_123 = "1234567890"
private const val TYPE_DELIVERY = "delivery"
private const val AUTH_BEARER = "Bearer token"
private const val MATCHING_PUSH = "push"

private const val DEVICE_KEY = "_device"
private const val DEVICE_VAL = "Pixel"

private const val APP_ID = "id123"
private const val APP_IID = "iid456"
private const val APP_VER = "1.0.0"

// ---------- Assertion messages ----------
private const val MSG_MUST_HAVE_HTTP_KEY = "Result must contain response_with_http_code key"
private const val MSG_UID_PRESENT = "Result must contain uid for push event"
private const val MSG_TYPE_PRESENT = "Result must contain type for push event"
private const val MSG_UID_ABSENT = "Generic request must not contain uid"
private const val MSG_TYPE_ABSENT = "Generic request must not contain type"
private const val MSG_DEVICE_MERGED = "Device field must be merged"
private const val MSG_CUSTOM_MERGED = "Custom field must be merged"
private const val MSG_APP_INFO_ID = "_app_id must be filled from config.appInfo"
private const val MSG_APP_INFO_IID = "_app_iid must be filled from config.appInfo"
private const val MSG_APP_INFO_VER = "_app_ver must be filled from config.appInfo"

class MapBuilderTest {

    @Before
    fun setUp() {
        // Silence android.util.Log in JVM tests to avoid "not mocked" crashes
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

    /** createEventValue: with PushEventRequestData → includes uid & type with expected values */
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
    }

    /** createEventValue: with generic RequestData → does NOT include uid or type */
    @Test
    fun `createEventValue with generic RequestData should not include uid or type`() {
        val code = 200
        val response: DataClasses.Response? = null
        val requestData = mockk<RequestData>(relaxed = true)

        val result = MapBuilder.createEventValue(code, response, requestData)

        assertTrue(MSG_MUST_HAVE_HTTP_KEY, result.containsKey(KEY_HTTP_CODE))
        assertFalse(MSG_UID_ABSENT, result.containsKey(KEY_UID))
        assertFalse(MSG_TYPE_ABSENT, result.containsKey(KEY_TYPE))
    }

    /** unionMaps: merges device fields, custom fields, and app info from config */
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
