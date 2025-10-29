package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Activity
import android.os.Bundle
import com.altcraft.sdk.push.action.PushAction.handleExtras

/**
 * Activity that handles actions triggered by push notifications.
 *
 * Launched when the user interacts with a notification; processes intent extras via `handleExtras`
 * (open a URL or start an app component) and finishes immediately without showing UI.
 */
internal class AltcraftPushActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleExtras(this, intent?.extras)
        finish()
    }
}