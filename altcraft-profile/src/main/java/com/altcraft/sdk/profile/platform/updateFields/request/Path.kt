package com.altcraft.sdk.profile.platform.updateFields.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Provides backend paths used by the Profile module.
 *
 * Paths are relative to the core API URL; the core
 * [com.altcraft.sdk.network.request.RequestExecutor] prepends the resolved API URL
 * before sending a request.
 */
internal object Path {

    /**
     * Builds the endpoint path for updating profile fields.
     *
     * @return profile update path.
     */
    fun profileUpdatePath() = "/profile/update"
}
