package com.altcraft.sdk.push.platform.subscription.unsuspend.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the push subscription restore feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Builds the path for restoring a suspended push subscription.
     *
     * @return Push subscription unsuspend path.
     */
    fun unSuspendPath() = "/subscription/push/unsuspend/"
}
