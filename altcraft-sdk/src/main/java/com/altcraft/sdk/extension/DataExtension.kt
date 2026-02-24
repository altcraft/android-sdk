package com.altcraft.sdk.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.work.Data

/**
 * Helpers to convert WorkManager [Data] to a [Map]<String, String>.
 */
internal object DataExtension {

    /**
     * Converts [Data] to a [Map] of [String] to [String], filtering out nulls and
     * converting all non-null values to strings.
     *
     * Useful for safely extracting all WorkManager input data as strings.
     */
    fun Data.toStringMap() =
        keyValueMap.mapNotNull { (key, value) -> value?.let { key to it.toString() } }.toMap()
}