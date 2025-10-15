package com.altcraft.altcraftmobile.providers.hms

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.AltcraftSDK
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

/**
 * HMS service for handling push tokens and incoming notifications.
 *
 * Extends [HmsMessageService] and overrides key HMS callback methods.
 */
class HMSService : HmsMessageService() {
    /**
     * Called when a new HMS token is generated.
     *
     * @param token The new HMS token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Called when a push message is received from HMS.
     *
     * Forwards the message with additional metadata to all receivers.
     *
     * @param message The received [RemoteMessage].
     */
    override fun onMessageReceived(message: RemoteMessage) {
        AltcraftSDK.PushReceiver.takePush(this@HMSService, message.dataOfMap)
    }
}

