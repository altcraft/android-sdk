package com.altcraft.sdk.push.system.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Activity that handles actions triggered by push notifications.
 *
 * Re-deliveries routed to a live instance (SINGLE_TOP launch mode) are
 * handled via [onNewIntent], so a quick consecutive tap on another push
 * is never silently dropped.
 */
internal class AltcraftPushActionActivity : Activity() {
    /** Handles the launch extras and finishes the activity. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushAction.handleExtras(this, intent?.extras)
        finish()
    }

    /** Handles actions re-delivered to the live instance. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        PushAction.handleExtras(this, intent.extras)
        finish()
    }
}