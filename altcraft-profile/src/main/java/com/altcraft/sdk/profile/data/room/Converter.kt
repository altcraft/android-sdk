package com.altcraft.sdk.profile.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.TypeConverter
import com.altcraft.sdk.network.json.Converter.fromStringJson
import kotlinx.serialization.json.JsonElement

/**
 * Room type converters for Profile module data types.
 */
@Suppress("unused")
internal class Converter {

    /**
     * Converts a [JsonElement] to its string representation for database storage.
     */
    @TypeConverter
    fun fromJsonElement(value: JsonElement?): String? = value?.toString()

    /**
     * Parses a JSON string back into a [JsonElement] for in-memory use.
     */
    @TypeConverter
    fun toJsonElement(value: String?): JsonElement? = value.fromStringJson(
        "RoomConverter.toJsonElement"
    )
}