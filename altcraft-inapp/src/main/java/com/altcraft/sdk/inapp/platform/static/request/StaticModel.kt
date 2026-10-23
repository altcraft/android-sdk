package com.altcraft.sdk.inapp.platform.static.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Loaded in-app static resource payload.
 *
 * @property blockCss Block renderer CSS.
 * @property blockJs Block renderer JavaScript.
 * @property inAppLibraryJs In-app library JavaScript.
 */
internal data class StaticLoadModel(
    val blockCss: String,
    val blockJs: String,
    val inAppLibraryJs: String,
)
