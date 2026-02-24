@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.json.Converter.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PreferensesInstrumentedTest
 *
 * Positive scenarios:
 *  - test_2: setCurrentToken saves JSON; getSavedPushToken retrieves the same TokenData.
 *  - test_3: getMessageId returns 1 on first call and increments sequentially.
 *  - test_4: clear removes all stored values; getters return null/defaults; getMessageId restarts
 *  from 1.
 *  - test_5: manual JSON round-trip works with TokenData for SharedPreferences token.
 *
 * Negative scenarios:
 *  - test_7: getSavedPushToken returns null if stored JSON is malformed.
 *  - test_8: setCurrentToken(null) does not clear existing value (documents current behavior).
 */
@RunWith(AndroidJUnit4::class)
class PreferensesInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferenses.getPreferences(context).edit(commit = true) { clear() }
    }

    @After
    fun tearDown() {
        if (::context.isInitialized) {
            Preferenses.getPreferences(context).edit(commit = true) { clear() }
        }
    }

    /**
     * test_2: setCurrentToken + getSavedPushToken round-trip.
     */
    @Test
    fun setCurrentToken_and_getSavedPushToken_roundTrip_ok() = runTest {
        val td = TokenData("android-huawei", "abc-456")
        Preferenses.setCurrentToken(context, td)

        val saved = requireNotNull(Preferenses.getSavedPushToken(context))
        assertEquals(td.provider, saved.provider)
        assertEquals(td.token, saved.token)
    }

    /**
     * test_8: Current behavior: setCurrentToken(null) does not clear existing value.
     */
    @Test
    fun setCurrentToken_null_doesNotClear_existingValue() = runTest {
        val first = TokenData("android-rustore", "777")
        Preferenses.setCurrentToken(context, first)
        requireNotNull(Preferenses.getSavedPushToken(context))

        Preferenses.setCurrentToken(context, null)

        val after = requireNotNull(Preferenses.getSavedPushToken(context))
        assertEquals(first.token, after.token)
    }

    /**
     * test_7: getSavedPushToken returns null for malformed JSON.
     */
    @Test
    fun getSavedPushToken_malformedJson_returnsNull() = runTest {
        Preferenses.getPreferences(context).edit(commit = true) {
            putString(Preferenses.TOKEN_KEY, "{\"provider\":1}")
        }
        assertNull(Preferenses.getSavedPushToken(context))
    }

    /**
     * test_3: getMessageId returns 1 first, then increments sequentially.
     */
    @Test
    fun getMessageId_increments_fromOne() = runTest {
        val id1 = Preferenses.getMessageId(context)
        val id2 = Preferenses.getMessageId(context)
        val id3 = Preferenses.getMessageId(context)

        assertEquals(1, id1)
        assertEquals(2, id2)
        assertEquals(3, id3)
    }

    /**
     * test_4: clear removes all stored values; getMessageId restarts from 1.
     */
    @Test
    fun clear_storage_resets_values_and_messageId() = runTest {
        val c = TokenData("android-huawei", "tok-y")
        Preferenses.setCurrentToken(context, c)

        requireNotNull(Preferenses.getSavedPushToken(context))

        Preferenses.getPreferences(context).edit(commit = true) { clear() }

        assertNull(Preferenses.getSavedPushToken(context))
        assertEquals(1, Preferenses.getMessageId(context))
    }

    /**
     * test_5: Manual JSON round-trip for TokenData via SharedPreferences token.
     */
    @Test
    fun tokenData_manual_json_roundTrip_ok() = runTest {
        val td = TokenData("android-firebase", "tok-json")
        val raw = json.encodeToString(td)

        Preferenses.getPreferences(context).edit(commit = true) {
            putString(Preferenses.TOKEN_KEY, raw)
        }

        val loaded = requireNotNull(Preferenses.getSavedPushToken(context))
        assertEquals(td.provider, loaded.provider)
        assertEquals(td.token, loaded.token)
    }
}