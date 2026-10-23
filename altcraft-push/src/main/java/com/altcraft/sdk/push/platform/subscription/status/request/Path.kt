package com.altcraft.sdk.push.platform.subscription.status.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the push subscription status feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Builds the path for retrieving push subscription status.
     *
     * @return Push subscription status path.
     */
    fun pushStatusPath() = "/subscription/push/status/"
}
