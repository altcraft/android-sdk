package com.altcraft.altcraftmobile.providers.rustore

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.AltcraftSDK
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService

/**
 * RuStore service for handling push notifications.
 *
 * Extends [RuStoreMessagingService] and overrides key callbacks.
 */
class RuStoreService : RuStoreMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Called when a push message is received.
     *
     * Forwards the message to all receivers with added metadata.
     *
     * @param message The received RemoteMessage.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        AltcraftSDK.PushReceiver.takePush(this, message.data)
    }
}





