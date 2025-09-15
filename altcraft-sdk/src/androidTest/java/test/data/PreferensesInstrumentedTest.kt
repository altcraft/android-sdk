package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.json.Converter.json
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.runner.RunWith

/**
 * PreferensesInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: setPushToken saves JSON; getManualToken retrieves the same TokenData.
 *  - test_2: setCurrentToken saves JSON; getSavedToken retrieves the same TokenData.
 *  - test_3: getMessageId increments sequentially and persists across calls.
 *  - test_4: clear() removes all stored values; getters return null/defaults.
 *  - test_5: manual JSON round-trip works with TokenData.
 *
 * Negative scenarios:
 *  - test_6: getManualToken returns null if stored JSON is malformed.
 *  - test_7: getSavedToken returns null if stored JSON is malformed.
 *  - test_8: setCurrentToken(null) does not clear value (documents current behavior).
 *
 * Notes:
 *  - Instrumented Android tests (androidTest).
 *  - Run with real SharedPreferences from Application context.
 *  - Events.error is not mocked; real logging may appear in logs.
 */
@RunWith(AndroidJUnit4::class)
class PreferensesInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferenses.getPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        Preferenses.getPreferences(context).edit().clear().commit()
    }

    /** Checks setPushToken + getManualToken round-trip. */
    @Test
    fun setPushToken_and_getManualToken_roundTrip_ok() {
        val td = TokenData("android-firebase", "tok-123")
        Preferenses.setPushToken(context, td.provider, td.token)

        val loaded = Preferenses.getManualToken(context)
        Assert.assertNotNull(loaded)
        Assert.assertEquals(td.provider, loaded!!.provider)
        Assert.assertEquals(td.token, loaded.token)
    }

    /** Checks that malformed JSON leads to null in getManualToken. */
    @Test
    fun getManualToken_malformedJson_returnsNull() {
        val key = getPrivateField("MANUAL_TOKEN_KEY")
        Preferenses.getPreferences(context).edit().putString(key, "{not-json").commit()
        Assert.assertNull(Preferenses.getManualToken(context))
    }

    /** Checks setCurrentToken + getSavedToken round-trip. */
    @Test
    fun setCurrentToken_and_getSavedToken_roundTrip_ok() {
        val td = TokenData("android-huawei", "abc-456")
        Preferenses.setCurrentToken(context, td)

        val saved = Preferenses.getSavedToken(context)
        Assert.assertNotNull(saved)
        Assert.assertEquals(td.provider, saved!!.provider)
        Assert.assertEquals(td.token, saved.token)
    }

    /** Documents current behavior: setCurrentToken(null) does not clear existing value. */
    @Test
    fun setCurrentToken_null_doesNotClear_existingValue() {
        val first = TokenData("android-rustore", "777")
        Preferenses.setCurrentToken(context, first)
        Assert.assertNotNull(Preferenses.getSavedToken(context))

        Preferenses.setCurrentToken(context, null)
        val after = Preferenses.getSavedToken(context)
        Assert.assertNotNull(after)
        Assert.assertEquals(first.token, after!!.token)
    }

    /** Checks that malformed JSON leads to null in getSavedToken. */
    @Test
    fun getSavedToken_malformedJson_returnsNull() {
        val key = getPrivateField("TOKEN_KEY")
        Preferenses.getPreferences(context).edit().putString(key, "{\"provider\":1}").commit()
        Assert.assertNull(Preferenses.getSavedToken(context))
    }

    /** Checks that getMessageId increments and persists across calls. */
    @Test
    fun getMessageId_increments_and_persists() {
        val id1 = Preferenses.getMessageId(context)
        val id2 = Preferenses.getMessageId(context)
        val id3 = Preferenses.getMessageId(context)

        Assert.assertEquals(5, id1)
        Assert.assertEquals(6, id2)
        Assert.assertEquals(7, id3)
    }

    /** Checks that clear() resets stored values. */
    @Test
    fun clear_storage_resets_values() {
        val m = TokenData("android-firebase", "tok-x")
        val c = TokenData("android-huawei", "tok-y")
        Preferenses.setPushToken(context, m.provider, m.token)
        Preferenses.setCurrentToken(context, c)

        Assert.assertNotNull(Preferenses.getManualToken(context))
        Assert.assertNotNull(Preferenses.getSavedToken(context))

        Preferenses.getPreferences(context).edit().clear().commit()

        Assert.assertNull(Preferenses.getManualToken(context))
        Assert.assertNull(Preferenses.getSavedToken(context))
        Assert.assertEquals(5, Preferenses.getMessageId(context))
    }

    /** Checks manual JSON round-trip of TokenData via preferences. */
    @Test
    fun tokenData_manual_json_roundTrip_ok() {
        val td = TokenData("android-firebase", "tok-json")
        val key = getPrivateField("TOKEN_KEY")

        val raw = json.encodeToString(td)
        Preferenses.getPreferences(context).edit().putString(key, raw).commit()

        val loaded = Preferenses.getSavedToken(context)
        Assert.assertNotNull(loaded)
        Assert.assertEquals(td.provider, loaded!!.provider)
        Assert.assertEquals(td.token, loaded.token)
    }

    // Helper to access private constants in Preferenses
    private fun getPrivateField(fieldName: String): String {
        val field = Preferenses::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(null) as String
    }
}