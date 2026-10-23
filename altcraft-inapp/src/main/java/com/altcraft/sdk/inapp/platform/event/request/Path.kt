package com.altcraft.sdk.inapp.platform.event.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the in-app event feature.
 *
 * Paths are relative to the core API URL; the core [com.altcraft.sdk.network.request.RequestExecutor]
 * prepends the resolved API URL before sending a request.
 */
internal object Path {

    /**
     * Generates the path for in-app event tracking.
     *
     * @param type The type of the in-app event.
     * @return The relative path with the event type appended.
     */
    fun eventInAppPath(type: String) = "/inapp/event/$type"
}
