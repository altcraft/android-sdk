package com.altcraft.sdk.inapp.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.TypeConverter
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Availability
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Policy
import com.altcraft.sdk.network.json.Converter.fromStringJson
import com.altcraft.sdk.network.json.Converter.toStringJson
import kotlinx.serialization.json.JsonElement

/**
 * Room type converters for in-app module data types.
 */
@Suppress("unused")
internal class Converter {

    /**
     * Converts a list of strings to a JSON string for database storage.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value.toStringJson(
        "RoomConverter.fromStringList"
    )

    /**
     * Converts a JSON string back to a list of strings.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? = value.fromStringJson(
        "RoomConverter.toStringList"
    )

    /**
     * Serializes a list of [CategoryData] to JSON for Room.
     */
    @TypeConverter
    fun fromCategoryDataList(value: List<CategoryData>?): String? = value.toStringJson(
        "RoomConverter.fromCategoryDataList"
    )

    /**
     * Deserializes JSON to a list of [CategoryData].
     */
    @TypeConverter
    fun toCategoryDataList(value: String?): List<CategoryData>? = value.fromStringJson(
        "RoomConverter.toCategoryDataList"
    )

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

    /**
     * Converts [Policy] to JSON string.
     */
    @TypeConverter
    fun fromPolicy(value: Policy?): String? = value.toStringJson(
        "RoomConverter.fromPolicy"
    )

    /**
     * Converts JSON string back to [Policy].
     */
    @TypeConverter
    fun toPolicy(value: String?): Policy? = value.fromStringJson(
        "RoomConverter.toPolicy"
    )

    /**
     * Converts [Availability] to JSON string.
     */
    @TypeConverter
    fun fromAvailability(value: Availability?): String? = value.toStringJson(
        "RoomConverter.fromAvailability"
    )

    /**
     * Converts JSON string back into [Availability].
     */
    @TypeConverter
    fun toAvailability(value: String?): Availability? = value.fromStringJson(
        "RoomConverter.toAvailability"
    )
}