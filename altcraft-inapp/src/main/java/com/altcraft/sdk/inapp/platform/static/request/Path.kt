package com.altcraft.sdk.inapp.platform.static.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the in-app static resources feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Generates the path for the in-app library JS static file.
     *
     * @return The relative path for inappLibrary.js.
     */
    fun inAppLibJsPath() = "/inapp/v1/js/inappLibrary.js"

    /**
     * Generates the path for the in-app block editor JS static file.
     *
     * @return The relative path for inappBlock.js.
     */
    fun inAppBlockJsPath() = "/inapp/v1/js/inappBlock.js"

    /**
     * Generates the path for the in-app block editor CSS static file.
     *
     * @return The relative path for inappBlock.css.
     */
    fun inAppBlockCssPath() = "/inapp/v1/css/inappBlock.css"
}
