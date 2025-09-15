package com.altcraft.sdk.push.action

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Activity
import android.os.Bundle
import com.altcraft.sdk.push.action.PushAction.handleExtras

/**
 * `AltcraftPushActionActivity` is an `AppCompatActivity` subclass responsible for handling actions
 * triggered by push notifications.
 * This activity is launched when a user interacts with a push notification, and it processes
 * the intent extras to perform specific actions, such as opening a URL or launching an app component.
 */
internal class AltcraftPushActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleExtras(this, intent?.extras)
        finish()
    }
}

