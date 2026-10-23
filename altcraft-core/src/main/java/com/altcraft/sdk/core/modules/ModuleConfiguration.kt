package com.altcraft.sdk.core.modules

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo

/**
 * Configuration contract of an optional Altcraft SDK module.
 *
 * Core SDK stores module configurations without knowing their concrete
 * classes. Each module configuration is associated with its module
 * through [moduleId].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ModuleConfiguration {

    /**
     * Unique module identifier.
     *
     * Must match the corresponding [AltcraftModule.moduleId]
     * so that core can resolve the configuration of a registered module.
     */
    val moduleId: String
}
