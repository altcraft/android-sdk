package com.altcraft.sdk.auth

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.StringBuilder.bearerJwtToken
import com.altcraft.sdk.additional.StringBuilder.bearerRToken
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.PUSH_SUB_MATCHING
import com.altcraft.sdk.data.Constants.SHA256
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.sdk_events.EventList.jwtIsNull
import com.altcraft.sdk.sdk_events.EventList.matchingIdIsNull
import com.altcraft.sdk.sdk_events.EventList.matchingIsNull
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.json.Converter.json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import android.util.Base64
import com.altcraft.sdk.sdk_events.EventList.jwtTooLarge
import com.altcraft.sdk.sdk_events.EventList.payloadIsMissing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Manages authentication-related operations, including retrieving user tags,
 * validating authentication tokens, and generating authorization headers.
 */
internal object AuthManager {

    /**
     * Returns a user tag based on JWT or the provided `rToken`.
     *
     * @param rToken The role token.
     * @return The user tag, or `null` if an error occurs.
     */
    fun getUserTag(rToken: String?): String? {
        return try {
            rToken ?: extractJWTDataHash(JWTManager.instance.getJWT())
        } catch (e: Exception) {
            error("getUserTag", e)
            null
        }
    }

    /**
     * Checks that a Base64URL-encoded JWT payload fits the 16 KiB decoded limit.
     *
     * The function estimates decoded size without allocating:
     * it adds Base64 padding to a multiple of 4, then applies the (len * 3 / 4) rule.
     *
     * @param b64 Base64URL (no padding) encoded JWT payload (the middle part of JWT).
     * @return `true` if the estimated decoded size ≤ 16 KiB, otherwise `false`.
     */
    private fun jwtSizeExceeded(
        b64: String
    ): Boolean = (((b64.length + ((4 - (b64.length % 4)) % 4)) * 3) / 4) >= 16 * 1024

    /**
     * Decodes the payload part of a JWT string.
     *
     * Splits the token into its Base64URL-encoded parts, decodes the payload,
     * and returns it as a UTF-8 JSON string.
     *
     * @param jwt The full JWT token string (header.payload.signature).
     * @return The decoded payload JSON as a string, or `null` if decoding fails.
     */
    private fun decodeJwtPayload(jwt: String): String? {
        return try {
            val body = jwt.split('.').getOrNull(1) ?: exception(payloadIsMissing)
            if (jwtSizeExceeded(body)) exception(jwtTooLarge)

            val pad = (4 - (body.length % 4)) % 4

            String(
                Base64.decode(body + "=".repeat(pad), Base64.URL_SAFE or Base64.NO_WRAP),
                StandardCharsets.UTF_8
            )
        } catch (e: Exception) {
            error("decodeJwtPayload", e)
            null
        }
    }

    /**
     * Extracts and parses the "matching" claim from a JWT token.
     *
     * Supports both JSON objects and JSON encoded as a string inside the claim.
     *
     * @param jwt The JWT token string, may be `null`.
     * @return A deserialized [DataClasses.JWTMatching] object, or `null` if parsing fails.
     */
    private fun getMatchingDataFromJWT(jwt: String?): DataClasses.JWTMatching? {
        return try {
            val payload = decodeJwtPayload(jwt ?: exception(jwtIsNull)) ?: return null

            val root = json.parseToJsonElement(payload).jsonObject
            val raw = root[MATCHING] ?: return null
            val matchingObj = when {
                raw is JsonObject -> raw
                raw is JsonPrimitive && raw.isString ->
                    json.parseToJsonElement(raw.content).jsonObject

                else -> return null
            }

            json.decodeFromJsonElement<DataClasses.JWTMatching>(matchingObj)
        } catch (e: Exception) {
            error("getMatchingDataFromJWT", e)
            null
        }
    }

    /**
     * Computes a SHA-256 hash of the raw "matching" claim from a JWT token.
     *
     * @param jwt The JWT token containing a JSON-encoded "matching" claim.
     * @return The hexadecimal SHA-256 hash of the claim content, or `null` if parsing fails.
     */
    private fun extractJWTDataHash(jwt: String?): String? {
        return try {
            (getMatchingDataFromJWT(jwt)?.asString() ?: exception(matchingIdIsNull)).let {
                MessageDigest.getInstance(SHA256).digest(it.toByteArray()).joinToString("") {
                    "%02x".format(it)
                }
            }
        } catch (e: Exception) {
            error("extractJWTDataHash", e)
            null
        }
    }

    /**
     * Returns an `Authorization` header and matching mode based on available tokens.
     *
     * Priority:
     * 1. If `rToken` is present → `"Bearer rtoken@{rToken}"` with `push_sub` mode.
     * 2. Else if JWT is present → `"Bearer {JWT}"` with extracted matching mode.
     * 3. Else → `null`.
     *
     * @param config Configuration containing tokens.
     * @return A pair of the auth header and matching mode, or `null` if no token is available.
     */
    fun getAuthHeaderAndMatching(config: ConfigurationEntity): Pair<String, String>? {
        return try {
            val rToken = config.rToken
            val jwtToken by lazy { JWTManager.instance.getJWT() ?: exception(jwtIsNull) }
            val matchingMode by lazy {
                getMatchingDataFromJWT(jwtToken)?.matching ?: exception(matchingIsNull)
            }
            when {
                !rToken.isNullOrEmpty() -> bearerRToken(rToken) to PUSH_SUB_MATCHING
                matchingMode.isNotEmpty() -> bearerJwtToken(jwtToken) to matchingMode
                else -> null
            }
        } catch (e: Exception) {
            error("getAuthHeaderAndMatching", e)
            null
        }
    }
}