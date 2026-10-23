package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Utility for building auth headers and log/error messages used across the SDK.
 */
internal object StringBuilder {

    /**
     * Builds a Bearer authorization header using the given JWT token.
     *
     * @param jwtToken The JWT token to format.
     * @return A string in the format "Bearer <jwtToken>".
     */
    fun bearerJwtToken(jwtToken: String) = "Bearer $jwtToken"

    /**
     * Constructs an authorization string in the format: "Bearer rtoken@<token>".
     *
     * @param rToken The R-token to format.
     * @return A formatted bearer token string for use in authorization headers.
     */
    fun bearerRToken(rToken: String) = "Bearer rtoken@$rToken"

    /**
     * Builds a formatted log string for an event.
     *
     * @param message The name of the function where the event occurred.
     * @param logValue An optional map providing additional details about the event.
     * @return A formatted string in the format "<function>(): <message>".
     */
    fun eventLogBuilder(
        message: String? = null,
        logValue: Map<String, Any?>? = null
    ): String {
        val details = logValue
            ?.filterValues { value ->
                value != null && value.toString().isNotBlank()
            }
            ?.entries
            ?.joinToString("; ") { "${it.key}: ${it.value}" }
            ?.takeIf { it.isNotBlank() }
            ?.let { " $it" }
            ?: ""

        return (message?.takeIf { it.isNotBlank() } ?: "") + details
    }
}
