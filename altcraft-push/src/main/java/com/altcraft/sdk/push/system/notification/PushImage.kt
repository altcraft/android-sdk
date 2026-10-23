package com.altcraft.sdk.push.system.notification

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.altcraft.sdk.push.util.Log
import com.altcraft.sdk.push.util.Log.event
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.system.receiver.PushData
import com.altcraft.sdk.push.util.Utils.fromAssets
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles loading of images for push notifications.
 */
internal object PushImage {

    /**
     * Loads the small icon shown in collapsed push.
     *
     * @param context App context.
     * @param pushData Push data with icon info.
     * @return Icon bitmap or `null`.
     */
    suspend fun loadSmallImage(context: Context, pushData: PushData): Bitmap? {
        return when {
            pushData.icon.isEmpty() -> null
            pushData.icon.matches(Regex("(https?://.*)")) -> loadImage(context, pushData.icon)
            else -> fromAssets(context, pushData.icon)
        }
    }

    /**
     * Loads the banner shown in expanded push.
     *
     * @param context App context.
     * @param pushData Push data with image URL.
     * @return Banner bitmap or `null`.
     */
    suspend fun loadLargeImage(context: Context, pushData: PushData): Bitmap? {
        return if (pushData.image.isEmpty()) null else loadImage(context, pushData.image)
    }

    /**
     * Loads an image for the push notification asynchronously.
     *
     * If the loading coroutine is canceled, the Glide target is cleared
     * on the main thread, so no stale request target remains registered.
     *
     * @param context The context used to load the image.
     * @param url The URL of the image to be loaded.
     * @return Returns a `Bitmap` of the loaded image or `null` if the loading fails.
     */
    private suspend fun loadImage(context: Context, url: String): Bitmap? {
        val def = CompletableDeferred<Bitmap?>()
        val target = object : CustomTarget<Bitmap>() {
            /** Completes the deferred with the loaded bitmap. */
            override fun onResourceReady(
                bitmap: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                event("loadImage", ModuleEvents.successImgLoad)
                def.complete(bitmap)
            }

            /** Completes the deferred with `null` when the resource is cleared. */
            override fun onLoadCleared(placeholder: Drawable?) {
                def.complete(null)
            }

            /** Logs the failure and completes the deferred with `null`. */
            override fun onLoadFailed(errorDrawable: Drawable?) {
                Log.error("loadImage", ModuleEvents.errorImgLoad)
                def.complete(null)
            }
        }

        return try {
            Glide.with(context).asBitmap().load(url).into(target)
            def.await()
        } catch (e: CancellationException) {
            withContext(Dispatchers.Main) {
                Glide.with(context).clear(target)
            }
            throw e
        } catch (e: Exception) {
            Log.error("loadImage", e)
            null
        }
    }
}
