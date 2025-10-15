package com.altcraft.sdk.json.serializer.any

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.sdk_events.Events
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.ranges.contains

/**JSON → Kotlin converter (primitives, lists, maps)*/
object FromJson {

    /**
     * Converts a `JsonElement` to the nearest Kotlin value.
     *
     * * `JsonNull` → `null`
     * * `JsonPrimitive` → `Boolean` | `Int`/`Long` | `Double` | `String`
     * * `JsonArray` → typed `List<T>` if homogeneous (`String|Int|Long|Double|Boolean`),
     * else `List<Any?>`
     * * `JsonObject` → `Map<String, Any?>` (values converted recursively)
     *
     * Integers become `Int` when within range and `preferIntForSmallNumbers=true`, otherwise `Long`.
     * All errors are swallowed; returns `null` on failure.
     *
     * @param preferIntForSmallNumbers Prefer `Int` over `Long` for small integers.
     * @return Converted Kotlin value, or `null` on error.
     */
    internal fun JsonElement.toKotlinValueOrNull(
        preferIntForSmallNumbers: Boolean = true
    ): Any? = try {
        when (this) {
            is JsonNull -> null
            is JsonPrimitive -> convertPrimitive(this)
            is JsonArray -> convertArray(this, preferIntForSmallNumbers)
            is JsonObject -> {
                val out = LinkedHashMap<String, Any?>(this.size)
                for ((k, v) in this) {
                    out[k] = try {
                        v.toKotlinValueOrNull(preferIntForSmallNumbers)
                    } catch (_: Throwable) {
                        null
                    }
                }
                out
            }
        }
    } catch (e: Throwable) {
        Events.error("toKotlinValueOrNull", e)
        null
    }

    /**
     * Converts a `JsonPrimitive` to the nearest Kotlin value.
     *
     * Mapping:
     * - `true/false`  → `Boolean`
     * - integer (fits into `Int`) → `Int`, otherwise `Long`
     * - floating point            → `Double`
     * - JSON string               → `String`
     *
     * Safety:
     * - Never throws; on any failure returns the primitive's string content.
     *
     * @return The converted Kotlin value (`Boolean`, `Int`, `Long`, `Double`, or `String`).
     */
    private fun convertPrimitive(p: JsonPrimitive): Any = try {
        val content = p.content
        val isString = p.isString
        val boolVal = p.booleanOrNull
        val longVal = p.longOrNull
        val intVal = longVal?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
        val doubleVal = if (!isString && longVal == null) p.doubleOrNull else null

        when {
            boolVal != null -> boolVal
            isString -> content
            intVal != null -> intVal
            longVal != null -> longVal
            doubleVal != null -> doubleVal
            else -> content
        }
    } catch (e: Exception) {
        Events.error("convertPrimitive", e)
        p.content
    }

    /**
     * Converts a `JsonArray` to a Kotlin list.
     *
     * * Returns typed `List<T>` for homogeneous items (`String|Int|Long|Double|Boolean`).
     * * Otherwise returns `List<Any?>`.
     * * Never throws: item errors -> `null`; function errors -> empty list.
     */
    private fun convertArray(
        jsonArray: JsonArray,
        preferIntForSmallNumbers: Boolean
    ): Any = try {
        val list: List<Any?> = jsonArray.map { el ->
            try {
                el.toKotlinValueOrNull(preferIntForSmallNumbers)
            } catch (_: Throwable) {
                null
            }
        }
        when {
            list.all { it is String } -> list.map { it as String }
            list.all { it is Int } -> list.map { it as Int }
            list.all { it is Long } -> list.map { it as Long }
            list.all { it is Double } -> list.map { it as Double }
            list.all { it is Boolean } -> list.map { it as Boolean }
            else -> list
        }
    } catch (e: Exception) {
        Events.error("convertArray", e)
        emptyList<Any?>()
    }
}