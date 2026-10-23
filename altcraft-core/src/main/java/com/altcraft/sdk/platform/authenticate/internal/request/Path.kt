package com.altcraft.sdk.platform.authenticate.internal.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the authenticate feature.
 *
 * Paths are relative to the core API URL; the core
 * [com.altcraft.sdk.network.request.RequestExecutor] prepends the resolved API URL
 * before sending a request.
 */
internal object Path {

    /**
     * Builds the path for the authorization request.
     *
     * @return Authorization request path.
     */
    fun authenticatePath() = "/profile/authenticate"
}
