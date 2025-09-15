package com.altcraft.altcraftmobile.deeplink

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Intent
import android.net.Uri

/**
 * altcraft.main://home
 *
 * altcraft.main://example
 *
 * altcraft.main://logs
 *
 * altcraft.main://config
 *
 */

object DeepLink {
    fun resolveStartDestination(intent: Intent?): String {
        val uri = intent?.data ?: return "home"
        return uri.toDeepLinkDestination() ?: "home"
    }

    private fun Uri.toDeepLinkDestination(): String? {
        if (scheme != "altcraft.main") return null
        return when (host?.lowercase()) {
            "home" -> "home"
            "example" -> "example"
            "logs" -> "logs"
            "config" -> "config"
            else -> null
        }
    }
}