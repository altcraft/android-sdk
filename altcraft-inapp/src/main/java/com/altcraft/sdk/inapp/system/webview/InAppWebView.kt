package com.altcraft.sdk.inapp.system.webview

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView

private const val httpLoadSetting = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

/**
 * Factory for creating configured WebView
 * instances used for in-app content.
 */
internal object InAppWebView {

    /**
     * Creates a WebView configured for
     * displaying in-app HTML content.
     *
     * @param activity Activity used as WebView context.
     * @return Configured WebView instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        activity: Activity
    ): WebView {
        return WebView(activity).apply {
            settings.apply {
                useWideViewPort = true
                allowFileAccess = true
                javaScriptEnabled = true
                domStorageEnabled = true
                blockNetworkImage = false
                allowContentAccess = true
                loadWithOverviewMode = true
                loadsImagesAutomatically = true
                setSupportMultipleWindows(false)
                mixedContentMode = httpLoadSetting
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            setBackgroundColor(Color.TRANSPARENT)
        }
    }
}