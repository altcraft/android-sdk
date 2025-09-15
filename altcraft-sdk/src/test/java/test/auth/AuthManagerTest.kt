package test.auth

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.auth.JWTManager
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.R_TOKEN_MATCHING
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.json.Converter.json
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * AuthManagerTest
 *
 * Positive scenarios:
 *  - test_1: getAuthHeaderAndMatching → rToken has priority over JWT:
 *            returns "Bearer rtoken@<rToken>" with R_TOKEN_MATCHING.
 *  - test_2: getAuthHeaderAndMatching → no rToken, valid JWT with non-empty matching:
 *            returns "Bearer <JWT>" and matching mode from JWT.
 *  - test_5: getUserTag → when rToken is provided, returns the rToken itself.
 *  - test_6: getUserTag → when rToken is null and JWT.asString() is non-null:
 *            returns SHA-256 hash of JWTMatching.asString().
 *
 * Negative scenarios:
 *  - test_3: getAuthHeaderAndMatching → no rToken, JWT matching is empty:
 *            returns null.
 *  - test_4: getAuthHeaderAndMatching → no rToken and no JWT available:
 *            returns null.
 *  - test_7: getUserTag → JWTMatching.asString() is null (no IDs present):
 *            returns null.
 *
 * Notes:
 *  - JWT.decode(...) is statically mocked; JWTManager singleton is mocked.
 *  - android.util.Log is mocked to avoid "not mocked" crashes in JVM tests.
 *  - Events.error(...) is mocked to a relaxed object to prevent side effects.
 *  - Test constants (inputs/expected values & assertion messages) are defined above.
 */

// ---------- Test constants (inputs/expected values) ----------
private const val FAKE_JWT = "header.payload.signature"
private const val RTOKEN_PRIORITY = "RTOKEN_123"
private const val RTOKEN_FOR_TAG = "RTOKEN_ABC"
private const val EMAIL_A = "a@b.c"
private const val PROFILE_ID_P123 = "p123"
private const val MODE_PUSH_SUB = "push_sub"
private const val EXPECTED_HEADER_RTOKEN = "Bearer rtoken@$RTOKEN_PRIORITY"
private const val EXPECTED_HEADER_JWT = "Bearer $FAKE_JWT"

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

class AuthManagerTest {

    @Before
    fun setUp() {
        mockkStatic(JWT::class)
        mockkObject(JWTManager)

        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk<DataClasses.Error>(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    @Suppress("SameParameterValue")
    private fun givenJwtDecoded(fakeJwt: String, matchingJson: String) {
        val decoded = mockk<DecodedJWT>()
        val claim = mockk<Claim>()
        every { JWT.decode(fakeJwt) } returns decoded
        every { decoded.getClaim(MATCHING) } returns claim
        every { claim.asString() } returns matchingJson
    }

    private fun cfgWithRToken(rToken: String?): ConfigurationEntity {
        val cfg = mockk<ConfigurationEntity>(relaxed = true)
        every { cfg.rToken } returns rToken
        return cfg
    }

    // ========================= getAuthHeaderAndMatching =========================

    /** Ensures rToken has priority over JWT */
    @Test
    fun `getAuthHeaderAndMatching - rToken has priority over JWT`() {
        val config = cfgWithRToken(RTOKEN_PRIORITY)
        every { JWTManager.instance.getJWT() } returns "any.jwt.value"

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_WHEN_RTOKEN, result)

        val (header, mode) = result!!
        assertEquals(EXPECTED_HEADER_RTOKEN, header)
        assertEquals(R_TOKEN_MATCHING, mode)
    }

    /** Ensures JWT is used when rToken is absent and matching is valid */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, valid JWT and non-empty matching`() {
        val config = cfgWithRToken(null)

        val jwtMatching = DataClasses.JWTMatching(
            dbId = 1,
            matching = MODE_PUSH_SUB,
            email = EMAIL_A
        )
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)

        every { JWTManager.instance.getJWT() } returns FAKE_JWT
        givenJwtDecoded(FAKE_JWT, matchingJson)

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNotNull(MSG_NON_NULL_VALID_JWT, result)

        val (header, mode) = result!!
        assertEquals(EXPECTED_HEADER_JWT, header)
        assertEquals(MODE_PUSH_SUB, mode)
    }

    /** Ensures null is returned when matching is empty */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, JWT with empty matching returns null`() {
        val config = cfgWithRToken(null)

        val jwtMatchingEmptyMode = DataClasses.JWTMatching(
            dbId = 2,
            matching = "",
            profileId = PROFILE_ID_P123
        )
        val matchingJson =
            json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatchingEmptyMode)

        every { JWTManager.instance.getJWT() } returns FAKE_JWT
        givenJwtDecoded(FAKE_JWT, matchingJson)

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_EMPTY_MATCHING, result)
    }

    /** Ensures null is returned when neither rToken nor JWT is available */
    @Test
    fun `getAuthHeaderAndMatching - no rToken, no JWT available`() {
        val config = cfgWithRToken(null)
        every { JWTManager.instance.getJWT() } returns null

        val result = AuthManager.getAuthHeaderAndMatching(config)
        assertNull(MSG_NULL_NO_TOKENS, result)
    }

    // ================================ getUserTag ================================

    /** Ensures getUserTag returns rToken when provided */
    @Test
    fun `getUserTag - returns rToken when provided`() {
        val tag = AuthManager.getUserTag(RTOKEN_FOR_TAG)
        assertEquals(MSG_RTOKEN_RETURNED, RTOKEN_FOR_TAG, tag)
    }

    /** Ensures getUserTag uses JWT and returns SHA-256 hash of asString() */
    @Test
    fun `getUserTag - uses JWT when rToken is null and asString is not null`() {
        val jwtMatching = DataClasses.JWTMatching(
            dbId = 5,
            matching = MODE_PUSH_SUB,
            email = EMAIL_A
        )
        val matchingJson = json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatching)

        every { JWTManager.instance.getJWT() } returns FAKE_JWT
        givenJwtDecoded(FAKE_JWT, matchingJson)

        val tag = AuthManager.getUserTag(null)
        assertNotNull(MSG_NON_NULL_HASH, tag)

        val expectedAsString = jwtMatching.asString()!!
        val expected = java.security.MessageDigest.getInstance("SHA-256")
            .digest(expectedAsString.toByteArray())
            .joinToString("") { b -> "%02x".format(b) }

        assertEquals(MSG_SHA256_MATCH, expected, tag)
    }

    /** Ensures null is returned when JWTMatching.asString() is null */
    @Test
    fun `getUserTag - returns null when JWTMatching_asString is null`() {
        val jwtMatchingNoIds = DataClasses.JWTMatching(
            dbId = 7,
            matching = MODE_PUSH_SUB
        )
        val matchingJson =
            json.encodeToString(DataClasses.JWTMatching.serializer(), jwtMatchingNoIds)

        every { JWTManager.instance.getJWT() } returns FAKE_JWT
        givenJwtDecoded(FAKE_JWT, matchingJson)

        val tag = AuthManager.getUserTag(null)
        assertNull(MSG_NULL_AS_STRING_NULL, tag)
    }
}
