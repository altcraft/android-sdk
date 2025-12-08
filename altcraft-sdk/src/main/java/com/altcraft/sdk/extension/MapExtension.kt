package com.altcraft.sdk.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.work.Data
import com.altcraft.sdk.sdk_events.Events.error
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.*
import java.util.Collections
import java.util.IdentityHashMap

/**
 * `MapExtension` provides helpers to convert maps to WorkManager `Data` and JSON structures.
 */
internal object MapExtension {

    /**
     * Converts a [Map] of [String] keys and [String] values into a [Data] object
     * suitable for WorkManager, or returns `null` if building the [Data] fails.
     *
     * @receiver Map of key-value pairs to encode.
     * @return [Data] if conversion is successful; `null` otherwise.
     */
    fun Map<String, String>.toWorkDataOrNull(): Data? {
        return try {
            Data.Builder().apply {
                forEach { (k, v) -> putString(k, v) }
            }.build()
        } catch (e: Exception) {
            error("toWorkDataOrNull", e)
            null
        }
    }

    /**
     * Converts a [Map] with `String` keys and arbitrary values into a [JsonElement].
     *
     * Supports nested maps, collections, sequences, arrays, primitives, enums, and nulls.
     * Cyclic references are safely ignored, and non-string keys are skipped.
     *
     * @receiver The source map to convert.
     * @param maxDepth Maximum recursion depth before returning [JsonNull].
     * @param maxCollectionElements Maximum number of elements per collection or array.
     * @return A [JsonElement] representing the entire map structure.
     */
    fun Map<String, Any?>.mapToJson(
        maxDepth: Int = 64,
        maxCollectionElements: Int = 10_000
    ): JsonElement {
        val visiting = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

        fun Any?.toJson(depth: Int): JsonElement {
            if (depth >= maxDepth) return JsonNull

            return try {
                when (this) {
                    null -> JsonNull
                    is Boolean -> JsonPrimitive(this)
                    is Number -> JsonPrimitive(this)
                    is String -> JsonPrimitive(this)
                    is Char -> JsonPrimitive(this.toString())
                    is Enum<*> -> JsonPrimitive(this.name)

                    // --- Maps ---
                    is Map<*, *> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonObject {
                                var count = 0
                                for ((k, v) in this@toJson) {
                                    if (count++ >= maxCollectionElements) break
                                    if (k is String) put(k, v.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    // --- Iterable (covers List, Set, etc.) ---
                    is Iterable<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                for ((count, item) in this@toJson.withIndex()) {
                                    if (count >= maxCollectionElements) break
                                    add(item.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    // --- Sequence (may be infinite) ---
                    is Sequence<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                var count = 0
                                val it = this@toJson.iterator()
                                while (count < maxCollectionElements && it.hasNext()) {
                                    add(it.next().toJson(depth + 1))
                                    count++
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    // --- Arrays (reference arrays) ---
                    is Array<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                for ((count, item) in this@toJson.withIndex()) {
                                    if (count >= maxCollectionElements) break
                                    add(item.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    // --- Primitive arrays ---
                    is BooleanArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is ByteArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is ShortArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is IntArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is LongArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is FloatArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is DoubleArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x))
                        }
                    }

                    is CharArray -> buildJsonArray {
                        for ((c, x) in this@toJson.withIndex()) {
                            if (c >= maxCollectionElements) break; add(JsonPrimitive(x.toString()))
                        }
                    }

                    // Fallback: safe string representation
                    else -> JsonPrimitive(runCatching { this.toString() }.getOrDefault("null"))
                }
            } catch (t: Throwable) {
                error("mapToJson", t)
                JsonNull
            }
        }

        return buildJsonObject {
            for ((k, v) in this@mapToJson) {
                put(k, v.toJson(depth = 0))
            }
        }
    }
}