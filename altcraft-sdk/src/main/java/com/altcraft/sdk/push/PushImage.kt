package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.sdk_events.EventList
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.sdk_events.Message.SUCCESS_IMG_LOAD
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CompletableDeferred

/**
 * Handles loading of images for push notifications.
 *
 * Provides methods to load the small icon, large banner, and fallback app icon.
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
            else -> SubFunction.fromAssets(context, pushData.icon)
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
     * @param context The context used to load the image.
     * @param url The URL of the image to be loaded.
     * @return Returns a `Bitmap` of the loaded image or `null` if the loading fails.
     */
    private suspend fun loadImage(context: Context, url: String): Bitmap? {
        val def = CompletableDeferred<Bitmap?>()
        return try {
            Glide.with(context).asBitmap().load(url).into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        bitmap: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        SubFunction.logger(SUCCESS_IMG_LOAD)
                        def.complete(bitmap)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Events.error("loadImage", EventList.errorImgLoad)
                        def.complete(null)
                    }
                })
            def.await()
        } catch (e: Exception) {
            Events.error("loadImage", e)
            null
        }
    }
}