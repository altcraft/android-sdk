package com.altcraft.sdk.inapp.system.presentation.bridge

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.webkit.JavascriptInterface
import androidx.annotation.RestrictTo

/**
 * WebView → native bridge for In-App events.
 *
 * @property onEvent Callback with raw event JSON.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InAppBridge(
    private val onEvent: (String) -> Unit
) {
    /**
     * Called from JavaScript with event payload.
     */
    @JavascriptInterface
    fun sendEvent(eventJson: String) {
        onEvent(eventJson)
    }
}
