package com.altcraft.sdk.inapp.platform.subscription.subscribe.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the in-app subscription feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Generates the path for subscribing to in-app notifications.
     *
     * @return The relative path for subscribing to in-app notifications.
     */
    fun inAppSubscribePath() = "/inapp/subscribe"
}
