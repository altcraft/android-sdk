package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

object Converter {
    fun mapToJsonString(map: Map<String, Any>): String = JSONObject(map).toString()


    object Parser {
        internal fun parseJsonToMap(jsonString: String?): Map<String, Any> {
            if (jsonString.isNullOrBlank()) return emptyMap()
            return try {
                Json.parseToJsonElement(jsonString).jsonObject.mapValues { (_, value) ->
                    when (value) {
                        is JsonPrimitive -> {
                            when {
                                value.booleanOrNull != null -> value.boolean
                                value.intOrNull != null -> value.int
                                value.doubleOrNull != null -> value.double
                                else -> value.content
                            }
                        }

                        else -> value.toString()
                    }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}