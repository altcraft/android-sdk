package com.altcraft.altcraftmobile.data.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.util.*

/**
 * JSON serializer for arbitrary non-null `Any`.
 *
 * Serialize: converts value to a JsonElement (see [toJsonElement]);
 * uses JsonEncoder when available, otherwise writes a compact JSON string.
 * On failure, writes the string "null".
 *
 * Deserialize: with JsonDecoder returns a JsonElement (typed as Any);
 * otherwise returns JsonNull.
 */
internal object AnyJsonSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AnyJson", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) {
        val element: JsonElement = try {
            value.toJsonElement()
        } catch (_: Throwable) {
            JsonNull
        }

        val je = encoder as? JsonEncoder
        if (je != null) {
            try {
                je.encodeJsonElement(element)
            } catch (_: Throwable) {
                try {
                    encoder.encodeString(element.toString())
                } catch (_: Throwable) {
                    encoder.encodeString("null")
                }
            }
        } else {
            try {
                encoder.encodeString(element.toString())
            } catch (_: Throwable) {
                encoder.encodeString("null")
            }
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val jd = decoder as? JsonDecoder ?: return JsonNull
        val el = try {
            jd.decodeJsonElement()
        } catch (_: Throwable) {
            return JsonNull
        }
        return el.toKotlinValueOrNull() ?: JsonNull
    }

    private fun JsonElement.toKotlinValueOrNull(
        preferIntForSmallNumbers: Boolean = true
    ): Any? = try {
        when (this) {
            is JsonNull -> null
            is JsonPrimitive -> when {
                booleanOrNull != null -> boolean
                !isString && content.indexOf('.') < 0 && content.indexOf(
                    'e',
                    ignoreCase = true
                ) < 0 -> {
                    val l = longOrNull ?: content.toLongOrNull()
                    when {
                        l == null -> content
                        preferIntForSmallNumbers && l in Int.MIN_VALUE..Int.MAX_VALUE -> l.toInt()
                        else -> l
                    }
                }
                !isString -> doubleOrNull ?: content.toDoubleOrNull() ?: content
                else -> content
            }

            is JsonArray -> {
                val list =
                    map { runCatching {
                        it.toKotlinValueOrNull(preferIntForSmallNumbers) }.getOrNull()
                    }
                when {
                    list.all { it is String } -> list.map { it as String }
                    list.all { it is Int } -> list.map { it as Int }
                    list.all { it is Long } -> list.map { it as Long }
                    list.all { it is Double } -> list.map { it as Double }
                    list.all { it is Boolean } -> list.map { it as Boolean }
                    else -> list
                }
            }

            is JsonObject -> entries.associate { (k, v) ->
                k to runCatching {
                    v.toKotlinValueOrNull(preferIntForSmallNumbers)
                }.getOrNull()
            }
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Safely converts `Any?` to a JsonElement.
     *
     * Supports primitives, enums, String/Char, Maps (String keys), Iterables, Sequences,
     * arrays and primitive arrays. Prevents cycles via IdentityHashMap and limits by
     * [maxDepth] and [maxElements]. Never throws; falls back to JsonNull.
     */
    private fun Any?.toJsonElement(
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
            } catch (_: Throwable) {
                JsonNull
            }
        }

        return this.toJson(depth = 0)
    }
}
