package com.altcraft.sdk.platform.authenticate.external.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.util.Base64
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.SHA256
import com.altcraft.sdk.util.log.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.network.json.Converter.json
import com.altcraft.sdk.events.CoreEvents
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Processes JWT tokens: decodes the payload, extracts the "matching" claim,
 * and computes an SHA-256 hash of the matching identifier.
 */
internal object JWTProcessor {

    /** Maximum allowed decoded JWT payload size in bytes. */
    private const val MAX_JWT_SIZE = 16 * 1024

    /**
     * Checks whether a Base64URL-encoded JWT payload reaches or exceeds
     * the maximum allowed decoded size.
     *
     * @param b64 Base64URL-encoded JWT payload.
     * @return `true` if the estimated decoded size reaches or exceeds 16 KiB,
     * otherwise `false`.
     */
    private fun jwtSizeExceeded(b64: String) = ((b64.length + 3) / 4) * 3 >= MAX_JWT_SIZE

    /**
     * Decodes the payload part of a JWT string.
     *
     * Validates the estimated decoded payload size before Base64URL decoding.
     *
     * @param jwt The full JWT token string (`header.payload.signature`).
     * @return The decoded payload JSON, or `null` if extraction or decoding fails.
     */
    private fun decodeJWTPayload(jwt: String): String? = try {
        val payload = jwt.split('.').getOrNull(1) ?: exception(
            CoreEvents.payloadIsMissing
        )

        if (jwtSizeExceeded(payload)) exception(CoreEvents.jwtTooLarge)

        Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            .toString(StandardCharsets.UTF_8)
    } catch (e: Exception) {
        error("decodeJwtPayload", e)
        null
    }

    /**
     * Decodes JWT matching data from a JSON element.
     *
     * Supports both a JSON object and a JSON-encoded string.
     *
     * @param element JSON element containing matching data.
     * @return Decoded [JWTMatching].
     */
    private fun decodeJWTMatching(element: JsonElement): JWTMatching =
        json.decodeFromJsonElement(
            if (element is JsonPrimitive && element.isString) {
                json.parseToJsonElement(element.content)
            } else {
                element
            }
        )

    /**
     * Extracts and parses the "matching" claim from a JWT token.
     *
     * @param jwt The JWT token string, may be `null`.
     * @return A deserialized [JWTMatching] object, or `null` if the token
     * cannot be processed or the "matching" claim is absent.
     */
    internal fun getJWTPayload(jwt: String?): JWTMatching? = try {
        val decodeJWT = decodeJWTPayload(jwt ?: exception(CoreEvents.jwtTokenIsNull))
        json.parseToJsonElement(decodeJWT ?: exception(CoreEvents.jwtDecodeFailed))
            .jsonObject[MATCHING]?.let {
            decodeJWTMatching(it)
        }
    } catch (e: Exception) {
        error("getJWTPayload", e)
        null
    }

    /**
     * Computes an SHA-256 hash of the matching identifier extracted from a JWT token.
     *
     * @param jwt The JWT token string, may be `null`.
     * @return The lowercase hexadecimal SHA-256 hash of the matching identifier,
     * or `null` if the token cannot be processed.
     */
    internal fun createJWTHash(jwt: String?) = try {
        val payload = getJWTPayload(jwt)?.asString() ?: exception(CoreEvents.matchingIdIsNull)
        MessageDigest.getInstance(SHA256).digest(payload.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    } catch (e: Exception) {
        error("createJWTHash", e)
        null
    }
}