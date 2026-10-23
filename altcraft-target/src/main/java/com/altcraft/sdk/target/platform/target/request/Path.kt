package com.altcraft.sdk.target.platform.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the Target module.
 *
 * Paths are relative to the core API URL; the core
 * [com.altcraft.sdk.network.request.RequestExecutor] prepends the resolved API URL
 * before sending a request.
 */
internal object Path {

    /**
     * Builds the endpoint path for sending a target event.
     *
     * @return target event path.
     */
    fun targetPath() = "/event/post"
}
