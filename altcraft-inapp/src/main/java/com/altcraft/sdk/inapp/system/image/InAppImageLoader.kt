package com.altcraft.sdk.inapp.system.image

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.altcraft.sdk.inapp.util.Log.error
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Preloads images using Glide and stores them in the local cache index.
 *
 * Images are downloaded in parallel with a configurable concurrency limit.
 * Cached files are later served to WebView through [CacheIndex].
 */
internal object InAppImageLoader {

    /**
     * Result of an image preload operation.
     *
     * @property failedUrls URLs of images that failed to load.
     */
    internal data class ImageLoadResult(
        val failedUrls: List<String>
    ) {
        val success: Boolean
            get() = failedUrls.isEmpty()
    }

    /**
     * Preloads images using Glide and stores them in the local cache index.
     *
     * Images are downloaded in parallel with a configurable concurrency limit.
     * Execution is limited by the provided timeout.
     *
     * @param context Android context used to access Glide.
     * @param urls List of image URLs to preload.
     * @param parallelism Maximum number of concurrent downloads.
     * @return Image loading result with failed image URLs.
     */
    suspend fun loadImages(
        context: Context,
        urls: List<String>,
        parallelism: Int = 6
    ): ImageLoadResult = withTimeoutOrNull(15_000.milliseconds) {
        coroutineScope {
            val sem = Semaphore(parallelism)

            val failedUrls = urls.map { url ->
                async(Dispatchers.IO) {
                    sem.withPermit { process(context, url) } }
            }.awaitAll().filterNotNull()

            ImageLoadResult(
                failedUrls = failedUrls
            )
        }
    } ?: ImageLoadResult(
        failedUrls = urls
    )

    /**
     * Downloads a single image and stores it in [CacheIndex].
     *
     * The Glide future is awaited with a bounded timeout, so a stalling
     * download cannot occupy an IO thread indefinitely; on timeout the
     * image is treated as failed while Glide finishes in the background.
     *
     * @param context Android context used by Glide.
     * @param url Image URL to download.
     * @return `null` if image was downloaded and stored, otherwise failed URL.
     */
    private fun process(context: Context, url: String): String? {
        return try {
            CacheIndex.put(
                url,
                Glide.with(context)
                    .downloadOnly()
                    .load(url)
                    .submit()
                    .get(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
            null
        } catch (e: Exception) {
            error("inAppImagePreload", e)
            url
        }
    }

    /** Maximum wait for a single image download. */
    private const val LOAD_TIMEOUT_SECONDS = 15L
}