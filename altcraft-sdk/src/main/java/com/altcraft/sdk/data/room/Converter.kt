package com.altcraft.sdk.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep
import androidx.room.TypeConverter
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.json.Converter.fromStringJson
import com.altcraft.sdk.json.Converter.toStringJson
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
    fun fromStringList(list: List<String>?): String? = list.toStringJson("fromStringList")

    /**
     * Converts a JSON string back to a list of strings.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? = value.fromStringJson("toStringList")

    /**
     * Converts a [DataClasses.AppInfo] object to a JSON string for database storage.
     */
    @TypeConverter
    fun fromAppInfo(appInfo: DataClasses.AppInfo?): String? = appInfo.toStringJson("fromAppInfo")

    /**
     * Converts a JSON string back to an [DataClasses.AppInfo] object.
     */
    @TypeConverter
    fun toAppInfo(jsonStr: String?): DataClasses.AppInfo? = jsonStr.fromStringJson("toAppInfo")

    /**
     * Serializes a list of [DataClasses.CategoryData] to JSON for Room.
     */
    @TypeConverter
    fun fromCategoryDataList(list: List<DataClasses.CategoryData>?): String? =
        list.toStringJson("fromCategoryDataList")

    /**
     * Deserializes JSON to a list of [DataClasses.CategoryData].
     */
    @TypeConverter
    fun toCategoryDataList(jsonStr: String?): List<DataClasses.CategoryData>? =
        jsonStr.fromStringJson("toCategoryDataList")

    /**
     * Converts a [JsonElement] to its string representation for database storage.
     */
    @TypeConverter
    fun fromJsonElement(json: JsonElement?): String? = json?.toString()

    /**
     * Parses a JSON string back into a [JsonElement] for in-memory use.
     */
    @TypeConverter
    fun toJsonElement(jsonStr: String?): JsonElement? = jsonStr.fromStringJson("fromJsonElement")
}