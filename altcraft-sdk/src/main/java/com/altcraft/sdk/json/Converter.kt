package com.altcraft.sdk.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.json.serializer.any.AnySerializer
import kotlinx.serialization.json.*
import com.altcraft.sdk.sdk_events.Events.error
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule

/**
 * Converter – utility for safe JSON serialization and deserialization
 * using Kotlinx Serialization with predefined Json configuration.
 *
 * Handles errors gracefully and logs them with function context.
 */
internal object Converter {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(Any::class, AnySerializer)
        }
    }

    /**
     * Serializes a `@Serializable` object to JSON.
     *
     * Logs any error and returns `null` if serialization fails.
     *
     * @param functionName Name for error logging.
     */
    inline fun <reified T> T?.toStringJson(functionName: String): String? {
        return try {
            this?.let { json.encodeToString(it) }
        } catch (e: Exception) {
            error(functionName, e)
            null
        }
    }

    /**
     * Deserializes a JSON string into an object of type [T] using Kotlin Serialization.
     *
     * Logs any error and returns `null` if deserialization fails.
     *
     * @param functionName Name for error logging.
     * @return Deserialized object of type [T], or `null` on failure.
     */
    inline fun <reified T> String?.fromStringJson(functionName: String): T? {
        return try {
            this?.let { json.decodeFromString(it) }
        } catch (e: Exception) {
            error(functionName, e)
            null
        }
    }
}