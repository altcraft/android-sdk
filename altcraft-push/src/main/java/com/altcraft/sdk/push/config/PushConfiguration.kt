package com.altcraft.sdk.push.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.push.data.Constants.MODULE_NAME
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.platform.token.TokenManager.providersValid

/**
 * Internal configuration for the push module.
 *
 * Stores push settings provided through the public
 * `AltcraftConfiguration.Builder.pushConfig(...)` API.
 */
internal class PushConfiguration(

    /** Custom notification icon resource ID. */
    val icon: Int? = null,

    /** Push provider identifiers ordered by priority. */
    val providerPriority: List<String> = emptyList(),

    /**
     * Package name prefixes where
     * [com.altcraft.sdk.push.PushReceiver]
     * implementations are located.
     */
    val pushReceiverModules: List<String> = emptyList(),

    /** Custom notification channel name. */
    val pushChannelName: String? = null,

    /** Custom notification channel description. */
    val pushChannelDescription: String? = null
) : ModuleConfiguration {

    /** Indicates whether the push configuration is valid. */
    val valid = providersValid(
        providerPriority
    )

    /** Throws an exception if the configuration is invalid. */
    fun error(): Nothing = exception(
        ModuleEvents.invalidProvider
    )

    /** Unique push module identifier. */
    override val moduleId: String = MODULE_NAME

    /** Push configuration database entity. */
    val entity = PushConfigEntity(
        icon = icon,
        pushChannelDescription = pushChannelDescription,
        receiverPackages = pushReceiverModules,
        providerPriority = providerPriority,
        pushChannelName = pushChannelName
    )
}