package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * AltcraftConfigurationProvidersTest
 *
 * Positive scenarios:
 *  - test_1: All known providers (FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER) →
 *            toEntity() returns non-null; providerPriorityList preserved 1:1.
 *  - test_2: Subset of known providers → toEntity() returns non-null; list preserved 1:1.
 *  - test_3: Empty providerPriorityList → toEntity() returns non-null; list is empty.
 *
 * Negative scenarios:
 *  - test_4: Single unknown provider ("firebase") → toEntity() returns null; error logged
 *            via Events.error with code 470 and provider name in message.
 *  - test_5: Mixed known + unknown ("firebase", "unknown") → toEntity() returns null; error
 *            lists all invalid providers.
 *  - test_6: Case mismatch against constants (strict match) → toEntity() returns null; error
 *            mentions the mismatched provider.
 *  - test_7: Empty apiUrl even with valid providers → toEntity() returns null; error mentions "apiUrl".
 *
 * Notes:
 *  - Valid providers are fixed constants: android-firebase (FCM_PROVIDER),
 *    android-huawei (HMS_PROVIDER), android-rustore (RUS_PROVIDER).
 *  - android.util.Log and Events.error are mocked to avoid side effects in JVM tests.
 *  - Verifications assert Events.error("configToEntity", Pair(470, "<message>")) with expected fragments.
 */

// -------- Test constants --------
private const val API_URL = "https://api.example.com"
private const val EMPTY_API_URL = ""
private const val INVALID_FIREBASE = "firebase"
private const val INVALID_UNKNOWN = "unknown"

// ---------- Assertion messages ----------
private const val MSG_EXPECT_ENTITY_VALID = "Expected non-null entity for valid providers"
private const val MSG_EXPECT_ENTITY_VALID_SUBSET = "Expected non-null entity for valid subset"
private const val MSG_EXPECT_ENTITY_EMPTY_LIST =
    "Expected non-null entity when providerPriorityList is empty"
private const val MSG_EXPECT_LIST_MAPPED_1_TO_1 = "Expected providerPriorityList to be mapped 1:1"
private const val MSG_EXPECT_NULL_UNKNOWN_PROVIDER =
    "Expected null when providerPriorityList contains unknown provider"
private const val MSG_EXPECT_NULL_MIXED = "Expected null for mixed list with unknown providers"
private const val MSG_EXPECT_NULL_CASE_MISMATCH =
    "Expected null when provider case does not match constants"
private const val MSG_EXPECT_NULL_EMPTY_API_URL =
    "Expected null when apiUrl is empty even if providers are valid"

class AltcraftConfigurationTest {

    @Before
    fun setUp() {
        // Silence android.util.Log for JVM tests
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Mock Events.error to return proper type
        mockkObject(Events)
        every { Events.error(any(), any<Pair<Int, String>>()) } returns mockk<DataClasses.Error>(
            relaxed = true
        )
        every { Events.error(any(), any<Throwable>()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** Valid: all known providers → entity created, list mapped 1:1 */
    @Test
    fun toEntity_valid_all_known_providers_pass() {
        val providers = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = providers
        ).build()

        val entity: ConfigurationEntity? = cfg.toEntity()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, entity)
        assertEquals(MSG_EXPECT_LIST_MAPPED_1_TO_1, providers, entity!!.providerPriorityList)
    }

    /** Valid: subset of known providers → entity created */
    @Test
    fun toEntity_valid_subset_of_known_providers_pass() {
        val providers = listOf(FCM_PROVIDER, HMS_PROVIDER)
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = providers
        ).build()

        val entity = cfg.toEntity()
        assertNotNull(MSG_EXPECT_ENTITY_VALID_SUBSET, entity)
        assertEquals(MSG_EXPECT_LIST_MAPPED_1_TO_1, providers, entity!!.providerPriorityList)
    }

    /** Valid: empty providerPriorityList → entity created, list is empty */
    @Test
    fun toEntity_valid_empty_list_pass() {
        val providers = emptyList<String>()
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = providers
        ).build()

        val entity = cfg.toEntity()
        assertNotNull(MSG_EXPECT_ENTITY_EMPTY_LIST, entity)
        assertEquals(MSG_EXPECT_LIST_MAPPED_1_TO_1, providers, entity!!.providerPriorityList)
    }

    /** Invalid: single unknown provider ("firebase") → null + error contains name */
    @Test
    fun toEntity_invalid_single_unknown_provider_fails_and_logs() {
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = listOf(INVALID_FIREBASE)
        ).build()

        val entity = cfg.toEntity()
        assertNull(MSG_EXPECT_NULL_UNKNOWN_PROVIDER, entity)

        verify(atLeast = 1) {
            Events.error(
                "configToEntity",
                match<Pair<Int, String>> { (code, msg) ->
                    code == 470 &&
                            msg.contains("invalid config", ignoreCase = true) &&
                            msg.contains(INVALID_FIREBASE)
                }
            )
        }
    }

    /** Invalid: mixed known + unknown ("firebase", "unknown") → null + error lists all invalid */
    @Test
    fun toEntity_invalid_mixed_known_and_unknown_fails_and_logs_all_invalid() {
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = listOf(
                FCM_PROVIDER,
                INVALID_FIREBASE,
                RUS_PROVIDER,
                INVALID_UNKNOWN
            )
        ).build()

        val entity = cfg.toEntity()
        assertNull(MSG_EXPECT_NULL_MIXED, entity)

        verify(atLeast = 1) {
            Events.error(
                "configToEntity",
                match<Pair<Int, String>> { (code, msg) ->
                    code == 470 &&
                            msg.contains("invalid config", ignoreCase = true) &&
                            msg.contains(INVALID_FIREBASE) && msg.contains(INVALID_UNKNOWN)
                }
            )
        }
    }

    /** Invalid: case mismatch against constants (strict match) → null + error */
    @Test
    fun toEntity_invalid_case_mismatch_fails() {
        // Force a case mismatch relative to FCM_PROVIDER
        val candidate = FCM_PROVIDER.lowercase()
        val providers =
            if (candidate == FCM_PROVIDER) listOf(FCM_PROVIDER + "_") else listOf(candidate)

        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            providerPriorityList = providers
        ).build()

        val entity = cfg.toEntity()
        assertNull(MSG_EXPECT_NULL_CASE_MISMATCH, entity)

        verify(atLeast = 1) {
            Events.error(
                "configToEntity",
                match<Pair<Int, String>> { (code, msg) ->
                    code == 470 && msg.contains(providers.first())
                }
            )
        }
    }

    /** Invalid even with valid providers when apiUrl is empty */
    @Test
    fun toEntity_invalid_empty_apiUrl_fails_even_with_valid_providers() {
        val providers = listOf(FCM_PROVIDER, HMS_PROVIDER)
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = EMPTY_API_URL,
            providerPriorityList = providers
        ).build()

        val entity = cfg.toEntity()
        assertNull(MSG_EXPECT_NULL_EMPTY_API_URL, entity)

        verify(atLeast = 1) {
            Events.error(
                "configToEntity",
                match<Pair<Int, String>> { (code, msg) ->
                    code == 470 && msg.contains("apiUrl", ignoreCase = true)
                }
            )
        }
    }
}

