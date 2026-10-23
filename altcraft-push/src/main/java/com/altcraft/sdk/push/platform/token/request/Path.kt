package com.altcraft.sdk.push.platform.token.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the push token feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Builds the path for updating a push subscription token.
     *
     * @return Push token update path.
     */
    fun tokenUpdatePath() = "/subscription/push/update/"
}
