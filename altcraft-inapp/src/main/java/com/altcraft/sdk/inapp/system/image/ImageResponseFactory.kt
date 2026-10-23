package com.altcraft.sdk.inapp.system.image

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import com.altcraft.sdk.inapp.util.Log.error

private const val BUFFER_SIZE = 32 * 1024

/**
 * Factory for creating WebView responses
 * from cached image files.
 *
 * Used by WebViewClient interception logic.
 */
internal object ImageResponseFactory {

    /**
     * Creates a [WebResourceResponse] for a cached image request.
     *
     * @param request WebView resource request.
     * @return Cached image response or `null` if not found.
     */
    fun response(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val key = request.url.toString()
            val file = CacheIndex.get(key) ?: return null
            val mime = getMimeType(file)

            WebResourceResponse(mime, null, stream(file))
        } catch (e: Exception) {
            error("shouldInterceptRequest", e)
            null
        }
    }

    /**
     * Resolves MIME type based on file extension.
     *
     * @param file Cached image file.
     * @return Corresponding MIME type.
     */
    private fun getMimeType(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    /**
     * Creates a buffered stream for the cached image.
     *
     * @param file Cached image file.
     * @return Stream used by [WebResourceResponse].
     */
    private fun stream(file: File) = BufferedInputStream(
        FileInputStream(file), BUFFER_SIZE
    )
}