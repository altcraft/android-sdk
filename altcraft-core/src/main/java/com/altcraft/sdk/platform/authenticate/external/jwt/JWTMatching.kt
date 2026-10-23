package com.altcraft.sdk.platform.authenticate.external.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.DB_ID
import com.altcraft.sdk.data.Constants.MATCHING
import com.altcraft.sdk.data.Constants.MATCHING_ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Data model for the JWT "matching" claim.
 *
 * Represents identifiers that can be used to match a profile in the platform.
 *
 * @param dbId ID of the database used for matching.
 * @param matching Matching method.
 * @param email Matching identifier: profile email.
 * @param phone Matching identifier: profile phone number.
 * @param profileId Matching identifier: unique profile ID in the platform.
 * @param fieldName Matching identifier: custom field name (for custom matching).
 * @param fieldValue Matching identifier: custom field value (string or number).
 * @param provider Provider code, if required for matching.
 * @param subscriptionId Matching identifier: subscription id.
 */
@Serializable
internal data class JWTMatching(
    @SerialName("db_id")
    val dbId: Int,
    @SerialName("matching")
    val matching: String,
    @SerialName("email") val
    email: String? = null,
    @SerialName("phone") val
    phone: String? = null,
    @SerialName("profile_id")
    val profileId: String? = null,
    @SerialName("field_name")
    val fieldName: String? = null,
    @SerialName("field_value")
    val fieldValue: JsonPrimitive? = null,
    @SerialName("provider")
    val provider: String? = null,
    @SerialName("subscription_id")
    val subscriptionId: String? = null,
) {

    /**
     * Builds a matching string for JWT payload.
     *
     * Matching identifiers are combined in the following order:
     * 1. Email
     * 2. Phone
     * 3. Profile ID
     * 4. Field name
     * 5. Field value
     * 6. Provider
     * 7. Subscription ID
     *
     * @return JSON string containing the matching data if at least one matching
     *         identifier is present, otherwise `null`.
     */
    fun asString(): String? {
        /** Returns the string if it is not blank, otherwise `null`. */
        fun String?.valid() = this?.takeIf(
            String::isNotBlank
        )

        val matchingValue = listOfNotNull(
            email.valid(),
            phone.valid(),
            profileId.valid(),
            fieldName.valid(),
            fieldValue?.content,
            provider.valid(),
            subscriptionId.valid()
        ).takeIf(
            List<*>::isNotEmpty
        )?.joinToString("/")
            ?: return null

        return """{
        "$DB_ID": "$dbId",
        "$MATCHING": "$matching",
        "$MATCHING_ID": "$matchingValue"
    }""".trimIndent()
    }

}
