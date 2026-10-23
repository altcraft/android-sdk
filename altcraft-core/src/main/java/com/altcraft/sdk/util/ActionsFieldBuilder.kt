package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.data.Constants.ACTION
import com.altcraft.sdk.data.Constants.ADD
import com.altcraft.sdk.data.Constants.DELETE
import com.altcraft.sdk.data.Constants.INCR
import com.altcraft.sdk.data.Constants.SET
import com.altcraft.sdk.data.Constants.UNSET
import com.altcraft.sdk.data.Constants.UPSERT
import com.altcraft.sdk.data.Constants.VALUE

/**
 * Helper class for building profile field entries that represent functional update operations.
 *
 * Supported actions: set, unset, incr, add, delete, upsert.
 *
 * @param key The profile field key (e.g., "Inc", "Name", etc.)
 */
@Keep
@Suppress("unused")
class ActionFieldBuilder(private val key: String) {
    /** Returns a profile field entry with the `set` action. */
    fun set(value: Any?) = key to mapOf(ACTION to SET, VALUE to value)

    /** Returns a profile field entry with the `unset` action. */
    fun unset(value: Any?) = key to mapOf(ACTION to UNSET, VALUE to value)

    /** Returns a profile field entry with the `incr` action. */
    fun incr(value: Any?) = key to mapOf(ACTION to INCR, VALUE to value)

    /** Returns a profile field entry with the `add` action. */
    fun add(value: Any?) = key to mapOf(ACTION to ADD, VALUE to value)

    /** Returns a profile field entry with the `delete` action. */
    fun delete(value: Any?) = key to mapOf(ACTION to DELETE, VALUE to value)

    /** Returns a profile field entry with the `upsert` action. */
    fun upsert(value: Any?) = key to mapOf(ACTION to UPSERT, VALUE to value)
}