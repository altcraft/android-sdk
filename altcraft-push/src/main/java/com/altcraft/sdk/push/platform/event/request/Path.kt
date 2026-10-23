package com.altcraft.sdk.push.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the push event feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Builds the path for sending a push event.
     *
     * @param type Push event type, such as `open` or `delivery`.
     * @return Push event path.
     */
    fun eventPushPath(type: String) = "/event/push/$type"
}
