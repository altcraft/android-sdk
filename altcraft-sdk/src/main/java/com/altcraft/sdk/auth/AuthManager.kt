package com.altcraft.sdk.auth

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.additional.StringBuilder.bearerJwtToken
import com.altcraft.sdk.additional.StringBuilder.bearerRToken
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.R_TOKEN_MATCHING
import com.altcraft.sdk.data.Constants.SHA256
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.events.Events.error
import com.altcraft.sdk.events.EventList.jwtIsNull
import com.altcraft.sdk.events.EventList.matchingIdIsNull
import com.altcraft.sdk.events.EventList.matchingIsNull
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.json.Converter.json
import com.auth0.jwt.JWT
import java.security.MessageDigest

/**
 * Manages authentication-related operations, including retrieving user tags,
 * validating authentication tokens, and generating authorization headers.
 */
internal object AuthManager {

    /**
     * Returns a user tag based on JWT or the provided `rToken`.
     *
     * @param rToken The fallback token if JWT is unavailable.
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
     * Retrieves the raw "matching" claim from a JWT token with keys ordered deterministically.
     *
     * @param jwt The JWT token string (nullable).
     * @return A JSON string with ordered keys or `null` on failure.
     */
    private fun getMatchingDataFromJWT(jwt: String?): DataClasses.JWTMatching? {
        return try {
            JWT.decode(jwt ?: exception(jwtIsNull)).getClaim(MATCHING).let {
                json.decodeFromString(it.asString())
            }
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
                !rToken.isNullOrEmpty() -> bearerRToken(rToken) to R_TOKEN_MATCHING
                matchingMode.isNotEmpty() -> bearerJwtToken(jwtToken) to matchingMode
                else -> null
            }
        } catch (e: Exception) {
            error("getAuthHeaderAndMatching", e)
            null
        }
    }
}