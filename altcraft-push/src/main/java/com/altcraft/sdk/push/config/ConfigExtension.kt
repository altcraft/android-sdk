package com.altcraft.sdk.push.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.config.AltcraftConfiguration

/**
 * Adds push module configuration to the SDK builder.
 *
 * @param icon Custom notification icon resource ID.
 * @param providerPriority Push provider identifiers ordered by priority.
 * @param pushReceiverModules Package name prefixes containing push receiver implementations.
 * @param pushChannelName Custom notification channel name.
 * @param pushChannelDescription Custom notification channel description.
 * @return Builder for chained calls.
 */
fun AltcraftConfiguration.Builder.pushConfig(
    icon: Int? = null,
    providerPriority: List<String> = emptyList(),
    pushReceiverModules: List<String> = emptyList(),
    pushChannelName: String? = null,
    pushChannelDescription: String? = null
): AltcraftConfiguration.Builder = addModuleConfiguration(
    PushConfiguration(
        icon = icon,
        providerPriority = providerPriority,
        pushReceiverModules = pushReceiverModules,
        pushChannelName = pushChannelName,
        pushChannelDescription = pushChannelDescription
    )
)