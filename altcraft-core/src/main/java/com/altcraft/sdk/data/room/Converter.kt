package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import androidx.room.TypeConverter
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.data.models.CoreModels.PropertyModel.AppInfo
import com.altcraft.sdk.network.json.Converter.fromStringJson
import com.altcraft.sdk.network.json.Converter.toStringJson
import kotlinx.serialization.json.JsonElement

/**
 * Room type converters for SDK-specific data types.
 */
@Keep
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
     * Converts a list of Int to JSON string.
     */
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value.toStringJson(
        "RoomConverter.fromIntList"
    )

    /**
     * Converts a JSON string back to a list of Int.
     */
    @TypeConverter
    fun toIntList(value: String?): List<Int>? = value.fromStringJson(
        "RoomConverter.toIntList"
    )

    /**
     * Converts an [AppInfo] object to a JSON string for database storage.
     */
    @TypeConverter
    fun fromAppInfo(value: AppInfo?): String? = value.toStringJson(
        "RoomConverter.fromAppInfo"
    )

    /**
     * Converts a JSON string back to an [AppInfo] object.
     */
    @TypeConverter
    fun toAppInfo(value: String?): AppInfo? = value.fromStringJson(
        "RoomConverter.toAppInfo"
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
}