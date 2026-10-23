package com.altcraft.sdk.inapp.platform.subscription.status.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the in-app subscription status feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Generates the path for requesting in-app notification subscription status.
     *
     * @return The relative path for getting in-app subscription status.
     */
    fun inAppStatusPath() = "/inapp/status"
}
