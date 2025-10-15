package com.altcraft.sdk.json.serializer.any

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.json.serializer.any.FromJson.toKotlinValueOrNull
import com.altcraft.sdk.json.serializer.any.ToJson.toJsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull

/**
 * JSON serializer for arbitrary non-null `Any`.
 *
 * Serialize: converts value to a JsonElement (see [ToJson.toJsonElement]);
 * uses JsonEncoder when available, otherwise writes a compact JSON string.
 * On failure, writes the string "null".
 *
 * Deserialize: with JsonDecoder returns a JsonElement (typed as Any);
 * otherwise returns JsonNull.
 */
internal object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AnyJson", PrimitiveKind.STRING)

    /**
     * Serializes arbitrary `Any` by converting it to `JsonElement`.
     * Falls back to `encodeString(element.toString())`, then to `"null"`. Never throws.
     */
    override fun serialize(encoder: Encoder, value: Any) {
        val element = runCatching { value.toJsonElement() }.getOrElse { JsonNull }

        fun encodeAsStringFallback() {
            runCatching { encoder.encodeString(element.toString()) }
                .getOrElse { encoder.encodeString("null") }
        }

        if (encoder is JsonEncoder) {
            runCatching { encoder.encodeJsonElement(element) }
                .getOrElse { encodeAsStringFallback() }
        } else {
            encodeAsStringFallback()
        }
    }

    /**
     * Deserializes via `JsonElement` and converts to nearest Kotlin value.
     * Returns `JsonNull` if not a JSON decoder or on failure. Never throws.
     */
    override fun deserialize(decoder: Decoder): Any {
        val jd = decoder as? JsonDecoder ?: return JsonNull
        val el = runCatching { jd.decodeJsonElement() }.getOrElse { JsonNull }
        return el.toKotlinValueOrNull() ?: JsonNull
    }
}