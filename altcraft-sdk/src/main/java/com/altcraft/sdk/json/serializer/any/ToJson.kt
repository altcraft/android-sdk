package com.altcraft.sdk.json.serializer.any

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.Collections
import java.util.IdentityHashMap
import com.altcraft.sdk.sdk_events.Events.error

object ToJson {

    /**
     * Safely converts `Any?` to a JsonElement.
     *
     * Supports primitives, enums, String/Char, Maps (String keys), Iterables, Sequences,
     * arrays and primitive arrays. Prevents cycles via IdentityHashMap and limits by
     * [maxDepth] and [maxElements]. Never throws; falls back to JsonNull.
     */
    internal fun Any?.toJsonElement(
        maxDepth: Int = 64,
        maxElements: Int = 10_000
    ): JsonElement {
        val visiting = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

        fun Any?.toJson(depth: Int): JsonElement {
            if (depth >= maxDepth) return JsonNull
            return try {
                when (this) {
                    is JsonElement -> this

                    null -> JsonNull
                    is Boolean -> JsonPrimitive(this)
                    is Number -> JsonPrimitive(this)
                    is String -> JsonPrimitive(this)
                    is Char -> JsonPrimitive(toString())
                    is Enum<*> -> JsonPrimitive(name)

                    is Map<*, *> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonObject {
                                var c = 0
                                for ((k, v) in this@toJson) {
                                    if (c++ >= maxElements) break
                                    if (k is String) put(k, v.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    is Iterable<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                var i = 0
                                for (item in this@toJson) {
                                    if (i++ >= maxElements) break
                                    add(item.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    is Sequence<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                var i = 0
                                val it = this@toJson.iterator()
                                while (i < maxElements && it.hasNext()) {
                                    add(it.next().toJson(depth + 1)); i++
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    is Array<*> -> {
                        if (!visiting.add(this)) return JsonNull
                        try {
                            buildJsonArray {
                                var i = 0
                                for (item in this@toJson) {
                                    if (i++ >= maxElements) break
                                    add(item.toJson(depth + 1))
                                }
                            }
                        } finally {
                            visiting.remove(this)
                        }
                    }

                    is BooleanArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is ByteArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is ShortArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is IntArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is LongArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is FloatArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is DoubleArray -> JsonArray(this.map { JsonPrimitive(it) })
                    is CharArray -> JsonArray(this.map { JsonPrimitive(it.toString()) })

                    else -> JsonPrimitive(runCatching { toString() }.getOrDefault("null"))
                }
            } catch (e: Exception) {
                error("createMobileEventParts", e)
                JsonNull
            }
        }

        return this.toJson(depth = 0)
    }
}