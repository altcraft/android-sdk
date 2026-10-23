package com.altcraft.sdk.push.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.TypeConverter
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.network.json.Converter.fromStringJson
import com.altcraft.sdk.network.json.Converter.toStringJson
import kotlinx.serialization.json.JsonElement

/**
 * Room type converters for push module entities.
 */
internal class Converter {

    /**
     * Converts a list of strings to a JSON string.
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
     * Converts a list of category data to a JSON string.
     */
    @TypeConverter
    fun fromCategoryDataList(value: List<CategoryData>?): String? = value.toStringJson(
        "RoomConverter.fromCategoryDataList"
    )

    /**
     * Converts a JSON string back to a list of category data.
     */
    @TypeConverter
    fun toCategoryDataList(value: String?): List<CategoryData>? = value.fromStringJson(
        "RoomConverter.toCategoryDataList"
    )

    /**
     * Converts a JSON element to a JSON string.
     */
    @TypeConverter
    fun fromJsonElement(value: JsonElement?): String? = value?.toString()

    /**
     * Converts a JSON string back to a JSON element.
     */
    @TypeConverter
    fun toJsonElement(value: String?): JsonElement? = value.fromStringJson(
        "RoomConverter.toJsonElement"
    )
}