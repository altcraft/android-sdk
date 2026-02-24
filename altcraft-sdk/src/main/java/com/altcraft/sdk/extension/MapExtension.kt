package com.altcraft.sdk.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.sdk_events.Events.error
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.*
import java.util.Collections
import java.util.IdentityHashMap

/**
 * `MapExtension` provides helpers for converting maps to JSON structures.
 */
internal object MapExtension {

    /**
     * Converts a Map<String, Any?> to [JsonElement].
     *
     * Supports nested structures. Cycles and values beyond [maxDepth]
     * are represented as [JsonNull]. In nested maps, only String keys are included.
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