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

private const val FAKE_HEADER = """{"alg":"HS256","typ":"JWT"}"""
private const val FAKE_SIG = "signature"
private const val RTOKEN_PRIORITY = "RTOKEN_123"
private const val RTOKEN_FOR_TAG = "RTOKEN_ABC"
private const val EMAIL_A = "a@b.c"
private const val PROFILE_ID_P123 = "p123"
private const val MODE_PUSH_SUB = "push_sub"
private const val EXPECTED_HEADER_RTOKEN = "Bearer rtoken@$RTOKEN_PRIORITY"

private const val MSG_NON_NULL_WHEN_RTOKEN = "Expected non-null result when rToken is provided"
private const val MSG_NON_NULL_VALID_JWT = "Expected non-null result when JWT and matching are valid"
private const val MSG_NULL_EMPTY_MATCHING = "Expected null when matching is empty"
private const val MSG_NULL_NO_TOKENS = "Expected null when neither rToken nor JWT are available"
private const val MSG_RTOKEN_RETURNED = "Expected rToken to be returned"
private const val MSG_NON_NULL_HASH = "Expected non-null hash when JWT asString is valid"
private const val MSG_SHA256_MATCH = "Expected SHA-256 hash of JWTMatching.asString()"
private const val MSG_NULL_AS_STRING_NULL = "Expected null when JWTMatching.asString() is null"
private const val MSG_NULL_PAYLOAD_MISSING = "Expected null when JWT payload part is missing"
private const val MSG_NULL_PAYLOAD_TOO_LARGE = "Expected null when JWT payload is too large"

/**
 * AuthManagerTest
 *
 * Unit tests for AuthManager: authentication header + matching mode resolution and user tag generation.
 *
 * Positive scenarios:
 * - test_1: getAuthHeaderAndMatching() uses rToken over JWT when both exist.
 * - test_2: getAuthHeaderAndMatching() with valid JWT containing matching object returns "Bearer <JWT>" and mode.
 * - test_3: getAuthHeaderAndMatching() with valid JWT containing matching as string returns "Bearer <JWT>" and mode.
 * - test_8: getUserTag() returns provided rToken directly.
 * - test_9: getUserTag() computes SHA-256 of JWTMatching.asString() when rToken is null.
 *
 * Negative scenarios:
 * - test_4: getAuthHeaderAndMatching() returns null when matching is empty.
 * - test_5: getAuthHeaderAndMatching() returns null when neither rToken nor JWT available.
 * - test_6: getAuthHeaderAndMatching() returns null when payload part is missing.
 * - test_7: getAuthHeaderAndMatching() returns null when payload is too large.
 * - test_10: getUserTag() returns null when JWTMatching.asString() is null.
 */
class AuthManagerTest {

    @Before
    fun setUp() {
        mockkObject(JWTManager)
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
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val body = firstArg<String>()
            val pad = (4 - (body.length % 4)) % 4
            val padded = body + "=".repeat(pad)
            Base64.getUrlDecoder().decode(padded)
        }
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun b64Url(input: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(input.toByteArray())

    private fun jwtWithPayloadJson(payloadJson: String): String =
        "${b64Url(FAKE_HEADER)}.${b64Url(payloadJson)}.$FAKE_SIG"

    private fun jwtWithMatchingObject(matchingJson: String): String {
        val payload = """{"$MATCHING":$matchingJson}"""
        return jwtWithPayloadJson(payload)
    }

    private fun jwtWithMatchingString(matchingJson: String): String {
        val escaped = matchingJson.replace("\"", "\\\"")
        val payload = """{"$MATCHING":"$escaped"}"""
        return jwtWithPayloadJson(payload)
    }

    private fun cfgWithRToken(rToken: String?): ConfigurationEntity {
        val cfg = mockk<ConfigurationEntity>(relaxed = true)
        every { cfg.rToken } returns rToken
        return cfg
    }

    private fun jwtWithoutPayload(): String = "${b64Url(FAKE_HEADER)}..$FAKE_SIG"

    private fun hugeJwtPayload(minBytes: Int = 17 * 1024): String {
        val big = "x".repeat(minBytes)
        return """{"$MATCHING":"$big"}"""
    }

    /** - test_1: getAuthHeaderAndMatching() uses rToken when both rToken and JWT exist. */
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

    /** - test_2: getAuthHeaderAndMatching() returns "Bearer <JWT>" and mode when JWT has matching object. */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, valid JWT object matching`() {
        val config = cfgWithRToken(null)
        val jwtMatching = DataClasses.JWTMatching(dbId = 1, matching = MODE_PUSH_SUB, email = EMAIL_A)
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt
        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_VALID_JWT, result)
        val (header, mode) = result!!
        assertEquals("Bearer $jwt", header)
        assertEquals(MODE_PUSH_SUB, mode)
    }

    /** - test_3: getAuthHeaderAndMatching() returns "Bearer <JWT>" and mode when matching is a string. */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, valid JWT string matching`() {
        val config = cfgWithRToken(null)
        val jwtMatching = DataClasses.JWTMatching(dbId = 2, matching = MODE_PUSH_SUB, profileId = PROFILE_ID_P123)
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingString(matchingJson)
        every { JWTManager.instance.getJWT() } returns jwt
        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_VALID_JWT, result)
        val (header, mode) = result!!
        assertEquals("Bearer $jwt", header)
        assertEquals(MODE_PUSH_SUB, mode)
    }

    /** - test_4: getAuthHeaderAndMatching() returns null when matching is empty. */
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

    /** - test_5: getAuthHeaderAndMatching() returns null when neither rToken nor JWT available. */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, no JWT available`() {
        val config = cfgWithRToken(null)
        every { JWTManager.instance.getJWT() } returns null
        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_NO_TOKENS, result)
    }

    /** - test_6: getAuthHeaderAndMatching() returns null when JWT payload is missing. */
    @Test
    fun `getAuthHeaderAndMatching - payload missing returns null`() {
        val config = cfgWithRToken(null)
        val jwt = jwtWithoutPayload()
        every { JWTManager.instance.getJWT() } returns jwt
        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_PAYLOAD_MISSING, result)
    }

    /** - test_7: getAuthHeaderAndMatching() returns null when JWT payload is too large. */
    @Test
    fun `getAuthHeaderAndMatching - payload too large returns null`() {
        val config = cfgWithRToken(null)
        val huge = hugeJwtPayload()
        val jwt = jwtWithPayloadJson(huge)
        every { JWTManager.instance.getJWT() } returns jwt
        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_PAYLOAD_TOO_LARGE, result)
    }

    /** - test_8: getUserTag() returns provided rToken directly. */
    @Test
    fun `getUserTag - returns rToken when provided`() {
        val tag = AuthManager.getUserTag(RTOKEN_FOR_TAG)
        assertEquals(MSG_RTOKEN_RETURNED, RTOKEN_FOR_TAG, tag)
    }

    /** - test_9: getUserTag() uses SHA-256 of JWTMatching.asString() when rToken is null. */
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

    /** - test_10: getUserTag() returns null when JWTMatching.asString() is null. */
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
