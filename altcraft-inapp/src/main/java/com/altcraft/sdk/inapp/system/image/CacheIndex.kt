package com.altcraft.sdk.inapp.system.image

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory index mapping image URLs
 * to cached files on disk.
 *
 * Used by the in-app image loader to quickly resolve
 * cached files for WebView resource interception.
 */
internal object CacheIndex {

    private val map = ConcurrentHashMap<String, File>()

    /**
     * Stores a cached file by URL key.
     *
     * @param key Image URL used as cache key.
     * @param file Cached file on disk.
     */
    fun put(key: String, file: File) {
        map[key] = file
    }

    /**
     * Retrieves cached file for the given URL.
     *
     * @param key Image URL used as cache key.
     * @return Cached file or `null` if not found.
     */
    fun get(key: String): File? = map[key]

    /**
     * Clears the entire cache index.
     */
    fun clear() = map.clear()
}