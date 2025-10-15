package test.auth

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.auth.JWTManager
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.PUSH_SUB_MATCHING
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.json.Converter.json
import com.altcraft.sdk.sdk_events.Events
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.Base64

// ---------- Test constants ----------
private const val FAKE_HEADER = """{"alg":"HS256","typ":"JWT"}"""
private const val FAKE_SIG = "signature"
private const val RTOKEN_PRIORITY = "RTOKEN_123"
private const val RTOKEN_FOR_TAG = "RTOKEN_ABC"
private const val EMAIL_A = "a@b.c"
private const val PROFILE_ID_P123 = "p123"
private const val MODE_PUSH_SUB = "push_sub"
private const val EXPECTED_HEADER_RTOKEN = "Bearer rtoken@$RTOKEN_PRIORITY"

// ---------- Assertion messages ----------
private const val MSG_NON_NULL_WHEN_RTOKEN = "Expected non-null result when rToken is provided"
private const val MSG_NON_NULL_VALID_JWT =
    "Expected non-null result when JWT and matching are valid"
private const val MSG_NULL_EMPTY_MATCHING = "Expected null when matching is empty"
private const val MSG_NULL_NO_TOKENS = "Expected null when neither rToken nor JWT are available"
private const val MSG_RTOKEN_RETURNED = "Expected rToken to be returned"
private const val MSG_NON_NULL_HASH = "Expected non-null hash when JWT asString is valid"
private const val MSG_SHA256_MATCH = "Expected SHA-256 hash of JWTMatching.asString()"
private const val MSG_NULL_AS_STRING_NULL = "Expected null when JWTMatching.asString() is null"
private const val MSG_NULL_PAYLOAD_MISSING = "Expected null when JWT payload part is missing"
private const val MSG_NULL_PAYLOAD_TOO_LARGE = "Expected null when JWT payload is too large"

/**
 * ## AuthManagerTest
 *
 * Unit tests for [AuthManager], validating how authentication headers and user tags
 * are generated from rToken or JWT under different conditions.
 *
 * ### Positive scenarios
 * - **test_1 — getAuthHeaderAndMatching (rToken has priority over JWT):**
 *   When both rToken and JWT exist, method must use the rToken and return:
 *   `"Bearer rtoken@<rToken>"` as header and `"push_sub"` as matching mode.
 *
 * - **test_2 — getAuthHeaderAndMatching (no rToken, valid JWT with matching object):**
 *   If rToken is null and JWT payload contains a JSON object with `"matching":"push_sub"`,
 *   returns `"Bearer <JWT>"` as header and `"push_sub"` as matching mode.
 *
 * - **test_3 — getAuthHeaderAndMatching (no rToken, valid JWT with matching string):**
 *   If JWT payload encodes `matching` as a JSON string (escaped), returns `"Bearer <JWT>"`
 *   and extracts the matching mode from that string.
 *
 * - **test_4 — getUserTag (rToken provided):**
 *   If an rToken is given directly, the method returns it as the user tag.
 *
 * - **test_5 — getUserTag (rToken is null, valid JWT, asString() not null):**
 *   When no rToken but JWT contains valid identifying fields, method builds
 *   SHA-256 hex digest of `JWTMatching.asString()` and returns it as user tag.
 *
 * ### Negative scenarios
 * - **getAuthHeaderAndMatching (empty matching → null):**
 *   JWT contains `"matching":""`; expected result is `null`.
 *
 * - **getAuthHeaderAndMatching (no rToken and no JWT):**
 *   Neither rToken nor JWT available; expected result is `null`.
 *
 * - **getAuthHeaderAndMatching (payload missing → null):**
 *   JWT missing payload part (`"header..sig"`) must return `null`.
 *
 * - **getAuthHeaderAndMatching (payload too large → null):**
 *   JWT payload exceeding internal decode limit (~16 KB) must return `null`.
 *
 * - **getUserTag (JWTMatching.asString() == null → null):**
 *   When no rToken and `JWTMatching.asString()` returns null, user tag must be `null`.
 *
 * ### Test environment
 * - Android `Log` and `Base64` functions are mocked for JVM execution.
 * - `JWTManager` and `Events.error` are stubbed to isolate [AuthManager] logic.
 * - Helper builders construct JWTs with object or string-based `"matching"` fields
 *   and simulate malformed or over sized payloads for negative cases.
 */
class AuthManagerTest {

    /**
     * Initializes mocks for Android logging, Base64 decoding, and static SDK components.
     * This ensures the tests can run in a JVM environment without Android runtime.
     */
    @Before
    fun setUp() {
        mockkObject(JWTManager)

        // Stub Android Log methods to prevent real logging.
        mockkStatic(android.util.Log::class)
        every { android.util.Log.v(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Replace Android Base64 with Java Base64 for decoding in tests.
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val body = firstArg<String>()
            val pad = (4 - (body.length % 4)) % 4
            val padded = body + "=".repeat(pad)
            Base64.getUrlDecoder().decode(padded)
        }

        // Suppress Events.error calls to avoid side effects.
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)
    }

    /**
     * Cleans up all mocks after each test run.
     */
    @After
    fun tearDown() = unmockkAll()

    // ---------- Helper functions ----------

    /** Encodes a string to Base64 URL format without padding. */
    private fun b64Url(input: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(input.toByteArray())

    /** Constructs a valid JWT with the provided payload JSON string. */
    private fun jwtWithPayloadJson(payloadJson: String): String =
        "${b64Url(FAKE_HEADER)}.${b64Url(payloadJson)}.$FAKE_SIG"

    /** Constructs a JWT with a `matching` object as JSON. */
    private fun jwtWithMatchingObject(matchingJson: String): String {
        val payload = """{"$MATCHING":$matchingJson}"""
        return jwtWithPayloadJson(payload)
    }

    /** Constructs a JWT with a `matching` field as an escaped string. */
    private fun jwtWithMatchingString(matchingJson: String): String {
        val escaped = matchingJson.replace("\"", "\\\"")
        val payload = """{"$MATCHING":"$escaped"}"""
        return jwtWithPayloadJson(payload)
    }

    /** Creates a mocked [ConfigurationEntity] with a given rToken. */
    private fun cfgWithRToken(rToken: String?): ConfigurationEntity {
        val cfg = mockk<ConfigurationEntity>(relaxed = true)
        every { cfg.rToken } returns rToken
        return cfg
    }

    /** Constructs a JWT missing its payload part (format: "header..sig"). */
    private fun jwtWithoutPayload(): String = "${b64Url(FAKE_HEADER)}..$FAKE_SIG"

    /** Constructs a very large JWT payload to simulate exceeding decode limits. */
    private fun hugeJwtPayload(minBytes: Int = 17 * 1024): String {
        val big = "x".repeat(minBytes)
        return """{"$MATCHING":"$big"}"""
    }

    // ========================= getAuthHeaderAndMatching =========================

    /**
     * Verifies that when both rToken and JWT are available, rToken has higher priority.
     */
    @Test
    fun `getAuthHeaderAndMatching - rToken has priority over JWT`() {
        val config = cfgWithRToken(RTOKEN_PRIORITY)
        every { JWTManager.instance.getJWT() } returns "any.jwt.value"

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_WHEN_RTOKEN, result)

        val (header, mode) = result!!
        assertEquals(EXPECTED_HEADER_RTOKEN, header)
        assertEquals(PUSH_SUB_MATCHING, mode)
    }

    /**
     * Verifies that when rToken is null and JWT contains a valid `matching` object,
     * AuthManager correctly returns the JWT and extracted matching mode.
     */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, valid JWT object matching`() {
        val config = cfgWithRToken(null)

        val jwtMatching = DataClasses.JWTMatching(
            dbId = 1, matching = MODE_PUSH_SUB, email = EMAIL_A
        )
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_VALID_JWT, result)

        val (header, mode) = result!!
        assertEquals("Bearer $jwt", header)
        assertEquals(MODE_PUSH_SUB, mode)
    }

    /**
     * Verifies JWT parsing when the `matching` field is encoded as a JSON string.
     */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, valid JWT string matching`() {
        val config = cfgWithRToken(null)

        val jwtMatching = DataClasses.JWTMatching(
            dbId = 2, matching = MODE_PUSH_SUB, profileId = PROFILE_ID_P123
        )
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingString(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_VALID_JWT, result)

        val (header, mode) = result!!
        assertEquals("Bearer $jwt", header)
        assertEquals(MODE_PUSH_SUB, mode)
    }

    /**
     * Ensures AuthManager returns null when JWT contains an empty `matching` field.
     */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, JWT with empty matching returns null`() {
        val config = cfgWithRToken(null)
        val jwtMatchingEmptyMode = DataClasses.JWTMatching(dbId = 3, matching = "", profileId = PROFILE_ID_P123)
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatchingEmptyMode)
        val jwt = jwtWithMatchingObject(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_EMPTY_MATCHING, result)
    }

    /**
     * Ensures AuthManager returns null when neither rToken nor JWT are available.
     */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, no JWT available`() {
        val config = cfgWithRToken(null)
        every { JWTManager.instance.getJWT() } returns null

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_NO_TOKENS, result)
    }

    /**
     * Negative test — JWT missing payload part ("header..sig") must result in null.
     */
    @Test
    fun `getAuthHeaderAndMatching - payload missing returns null`() {
        val config = cfgWithRToken(null)
        val jwt = jwtWithoutPayload()
        every { JWTManager.instance.getJWT() } returns jwt

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_PAYLOAD_MISSING, result)
    }

    /**
     * Negative test — JWT payload too large must trigger internal limit and return null.
     */
    @Test
    fun `getAuthHeaderAndMatching - payload too large returns null`() {
        val config = cfgWithRToken(null)
        val huge = hugeJwtPayload()
        val jwt = jwtWithPayloadJson(huge)
        every { JWTManager.instance.getJWT() } returns jwt

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_PAYLOAD_TOO_LARGE, result)
    }

    // ================================ getUserTag ================================

    /**
     * Verifies that when rToken is explicitly provided, it is returned directly.
     */
    @Test
    fun `getUserTag - returns rToken when provided`() {
        val tag = AuthManager.getUserTag(RTOKEN_FOR_TAG)
        assertEquals(MSG_RTOKEN_RETURNED, RTOKEN_FOR_TAG, tag)
    }

    /**
     * Verifies that when rToken is null, user tag is generated as SHA-256 hash
     * of JWTMatching.asString() when it's available.
     */
    @Test
    fun `getUserTag - uses JWT when rToken is null and asString is not null (object)`() {
        val jwtMatching = DataClasses.JWTMatching(dbId = 5, matching = MODE_PUSH_SUB, email = EMAIL_A)
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt

        val tag = AuthManager.getUserTag(null)
        assertNotNull(MSG_NON_NULL_HASH, tag)

        val expectedAsString = jwtMatching.asString()!!
        val expected = java.security.MessageDigest.getInstance("SHA-256")
            .digest(expectedAsString.toByteArray())
            .joinToString("") { b -> "%02x".format(b) }

        assertEquals(MSG_SHA256_MATCH, expected, tag)
    }

    /**
     * Verifies that when JWTMatching.asString() is null,
     * AuthManager correctly returns null as user tag.
     */
    @Test
    fun `getUserTag - returns null when JWTMatching_asString is null`() {
        val jwtMatchingNoIds = DataClasses.JWTMatching(dbId = 7, matching = MODE_PUSH_SUB)
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatchingNoIds)
        val jwt = jwtWithMatchingObject(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt

        val tag = AuthManager.getUserTag(null)
        assertNull(MSG_NULL_AS_STRING_NULL, tag)
    }
}