package com.altcraft.sdk.inapp.system.webview

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.platform.static.StaticProcess
import com.altcraft.sdk.inapp.system.image.ImageResponseFactory

/**
 * WebViewClient for in-app messages.
 *
 * Serves cached images and injects static JS/CSS after page load.
 */
internal class Client(
    private val staticBundle: InAppModel.InAppStatic,
    private val onPageLoaded: (() -> Unit)? = null,
) : WebViewClient() {

    /**
     * Intercepts resource requests to return cached images if available.
     */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return ImageResponseFactory.response(request)
    }

    /**
     * Called when the page has finished loading.
     *
     * Injects cached in-app JS/CSS resources and invokes [onPageLoaded]
     * after injection is completed.
     */
    override fun onPageFinished(
        view: WebView,
        url: String
    ) {
        super.onPageFinished(view, url)
        StaticProcess.inject(view, staticBundle) {
            onPageLoaded?.invoke()
        }
    }
}
