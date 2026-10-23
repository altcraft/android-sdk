package test.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.util.Base64
import android.util.Log
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.events.SDKException
import com.altcraft.sdk.network.json.Converter
import com.altcraft.sdk.platform.authenticate.external.ExternalAuth
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTMatching
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

private const val FAKE_HEADER = """{"alg":"HS256","typ":"JWT"}"""
private const val FAKE_SIG = "signature"
private const val RTOKEN_PRIORITY = "RTOKEN_123"
private const val RTOKEN_FOR_TAG = "RTOKEN_ABC"
private const val EMAIL_A = "a@b.c"
private const val PROFILE_ID_P123 = "p123"
private const val MODE_PUSH_SUB = "push_sub"
private const val EXPECTED_HEADER_RTOKEN = "Bearer rtoken@$RTOKEN_PRIORITY"

private const val MSG_NON_NULL_WHEN_RTOKEN = "Expected non-null result when rToken is provided"
private const val MSG_RTOKEN_RETURNED = "Expected rToken to be returned"
private const val MSG_NON_NULL_HASH = "Expected non-null hash when JWT asString is valid"
private const val MSG_SHA256_MATCH = "Expected SHA-256 hash of JWTMatching.asString()"

/**
 * AuthManagerTest
 *
 * Unit tests for the lazy external authorization container [ExternalAuth]
 * and its JWT processing: header / matching / user tag resolution.
 *
 * Positive scenarios:
 * - test_1: rToken is preferred over JWT (header, matching, user tag).
 * - test_2: JWT with matching object yields JWT header and its matching mode.
 * - test_3: JWT with matching string yields JWT header and its matching mode.
 * - test_8: user tag returns the provided rToken directly.
 * - test_9: user tag is SHA-256 of JWTMatching.asString() when rToken is null.
 *
 * Negative scenarios:
 * - test_4: JWT is accepted even when the matching mode is empty.
 * - test_5: no tokens → user / header / matching accessors throw SDKException.
 * - test_6: missing payload → matching and user throw SDKException.
 * - test_7: over-sized payload → matching and user throw SDKException.
 * - test_10: user tag throws SDKException when JWTMatching.asString() is null.
 */
class AuthManagerTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.d(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            val body = firstArg<String>()
            val pad = (4 - (body.length % 4)) % 4
            val padded = body + "=".repeat(pad)
            java.util.Base64.getUrlDecoder().decode(padded)
        }
        mockkObject(Events)
        every { Events.error(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun b64Url(input: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(input.toByteArray())

    private fun jwtWithPayloadJson(payloadJson: String): String =
        "${b64Url(FAKE_HEADER)}.${b64Url(payloadJson)}.$FAKE_SIG"

    private fun jwtWithMatchingObject(matchingJson: String): String {
        val payload = """{"${Constants.MATCHING}":$matchingJson}"""
        return jwtWithPayloadJson(payload)
    }

    private fun jwtWithMatchingString(matchingJson: String): String {
        val escaped = matchingJson.replace("\"", "\\\"")
        val payload = """{"${Constants.MATCHING}":"$escaped"}"""
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
        return """{"${Constants.MATCHING}":"$big"}"""
    }

    private fun authWith(config: ConfigurationEntity, jwt: String?): ExternalAuth =
        ExternalAuth(
            configReader = { config },
            jwtReader = { jwt }
        )

    /** Asserts that the block throws an SDKException with the given event. */
    private suspend fun assertThrowsSdkException(
        expected: SDKEvent,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            throw AssertionError("Expected SDKException for ${expected.message}")
        } catch (e: SDKException.SDKException) {
            Assert.assertEquals(
                "Unexpected SDKException event",
                expected,
                e.info()
            )
        }
    }

    /** - test_1: rToken is preferred over JWT. */
    @Test
    fun `ExternalAuth - rToken has priority over JWT`() = runBlocking {
        val auth = authWith(cfgWithRToken(RTOKEN_PRIORITY), "any.jwt.value")
        val header = auth.header()
        Assert.assertNotNull(MSG_NON_NULL_WHEN_RTOKEN, header)
        Assert.assertEquals(EXPECTED_HEADER_RTOKEN, header)
        Assert.assertEquals(Constants.PUSH_SUB_MATCHING, auth.matching())
        Assert.assertEquals(RTOKEN_PRIORITY, auth.user())
    }

    /** - test_2: JWT object matching. */
    @Test
    fun `ExternalAuth - no rToken, valid JWT object matching`() = runBlocking {
        val jwtMatching = JWTMatching(dbId = 1, matching = MODE_PUSH_SUB, email = EMAIL_A)
        val matchingJson = Converter.json.encodeToString(JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        val auth = authWith(cfgWithRToken(null), jwt)
        Assert.assertEquals("Bearer $jwt", auth.header())
        Assert.assertEquals(MODE_PUSH_SUB, auth.matching())
    }

    /** - test_3: JWT string matching. */
    @Test
    fun `ExternalAuth - no rToken, valid JWT string matching`() = runBlocking {
        val jwtMatching = JWTMatching(dbId = 2, matching = MODE_PUSH_SUB, profileId = PROFILE_ID_P123)
        val matchingJson = Converter.json.encodeToString(JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingString(matchingJson)
        val auth = authWith(cfgWithRToken(null), jwt)
        Assert.assertEquals("Bearer $jwt", auth.header())
        Assert.assertEquals(MODE_PUSH_SUB, auth.matching())
    }

    /** - test_4: JWT with empty matching mode is still accepted. */
    @Test
    fun `ExternalAuth - no rToken, JWT with empty matching returns header`() = runBlocking {
        val jwtMatching = JWTMatching(dbId = 3, matching = "", profileId = PROFILE_ID_P123)
        val matchingJson = Converter.json.encodeToString(JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        val auth = authWith(cfgWithRToken(null), jwt)
        Assert.assertNotNull("Expected non-null header when JWT is valid (matching can be empty)", auth.header())
        Assert.assertEquals("", auth.matching())
    }

    /** - test_5: no tokens → all accessors throw SDKException. */
    @Test
    fun `ExternalAuth - no rToken, no JWT available throws`() = runBlocking {
        val auth = authWith(cfgWithRToken(null), null)
        assertThrowsSdkException(CoreEvents.userIsNull) { auth.user() }
        assertThrowsSdkException(CoreEvents.authHeaderIsNull) { auth.header() }
        assertThrowsSdkException(CoreEvents.matchingIsNull) { auth.matching() }

        verify(exactly = 2) {
            Events.error(
                any(),
                match { it is SDKException.SDKException && it.info() == CoreEvents.jwtTokenIsNull }
            )
        }
        verify(exactly = 1) {
            Events.error(
                any(),
                match { it is SDKException.SDKException && it.info() == CoreEvents.matchingIdIsNull }
            )
        }
    }

    /** - test_6: missing payload → header present, matching and user throw. */
    @Test
    fun `ExternalAuth - payload missing throws for matching and user`() = runBlocking {
        val jwt = jwtWithoutPayload()
        val auth = authWith(cfgWithRToken(null), jwt)
        Assert.assertEquals("Bearer $jwt", auth.header())
        assertThrowsSdkException(CoreEvents.matchingIsNull) { auth.matching() }
        assertThrowsSdkException(CoreEvents.userIsNull) { auth.user() }
    }

    /** - test_7: over-sized payload → header present, matching and user throw. */
    @Test
    fun `ExternalAuth - payload too large throws for matching and user`() = runBlocking {
        val jwt = jwtWithPayloadJson(hugeJwtPayload())
        val auth = authWith(cfgWithRToken(null), jwt)
        Assert.assertEquals("Bearer $jwt", auth.header())
        assertThrowsSdkException(CoreEvents.matchingIsNull) { auth.matching() }
        assertThrowsSdkException(CoreEvents.userIsNull) { auth.user() }
    }

    /** - test_8: user tag returns the provided rToken directly. */
    @Test
    fun `ExternalAuth - userTag returns rToken when provided`() = runBlocking {
        val auth = authWith(cfgWithRToken(RTOKEN_FOR_TAG), "any.jwt.value")
        Assert.assertEquals(MSG_RTOKEN_RETURNED, RTOKEN_FOR_TAG, auth.user())
    }

    /** - test_9: user tag is SHA-256 of JWTMatching.asString() when rToken is null. */
    @Test
    fun `ExternalAuth - userTag uses JWT when rToken is null and asString is not null`() = runBlocking {
        val jwtMatching = JWTMatching(dbId = 5, matching = MODE_PUSH_SUB, email = EMAIL_A)
        val matchingJson = Converter.json.encodeToString(JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        val auth = authWith(cfgWithRToken(null), jwt)
        val tag = auth.user()
        Assert.assertNotNull(MSG_NON_NULL_HASH, tag)
        val expectedAsString = jwtMatching.asString()!!
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(expectedAsString.toByteArray())
            .joinToString("") { b -> "%02x".format(b) }
        Assert.assertEquals(MSG_SHA256_MATCH, expected, tag)
    }

    /** - test_10: user tag throws when JWTMatching.asString() is null. */
    @Test
    fun `ExternalAuth - userTag throws when JWTMatching_asString is null`() = runBlocking {
        val jwtMatching = JWTMatching(dbId = 7, matching = MODE_PUSH_SUB)
        val matchingJson = Converter.json.encodeToString(JWTMatching.serializer(), jwtMatching)
        val jwt = jwtWithMatchingObject(matchingJson)
        val auth = authWith(cfgWithRToken(null), jwt)
        assertThrowsSdkException(CoreEvents.userIsNull) { auth.user() }
    }
}
