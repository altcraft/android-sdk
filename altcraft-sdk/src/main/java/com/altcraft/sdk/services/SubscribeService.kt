package com.altcraft.sdk.services

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Keep
import com.altcraft.sdk.data.Constants.STOP_SERVICE_ACTION
import com.altcraft.sdk.services.manager.ServiceManager.checkStartForeground
import com.altcraft.sdk.services.manager.ServiceManager.closedServiceHandler
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startSubscribeCoroutineWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ImageLoaderService - the foreground service used to access the Internet
 * during the receipt of a medium priority push notification for devices with API level < 31.
 * During the operation of the service, images are uploaded for push notifications.
 */
@Keep
internal class SubscribeService : Service() {

    /**
     * Called when the service is first created.
     *
     * This method initializes the service and immediately attempts to start it in the foreground.
     * If the foreground start fails, the service stops itself.
     * Additionally, a coroutine is launched to automatically stop the service after 60 seconds.
     *
     */
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Main).launch {
            if (!checkStartForeground(this@SubscribeService)) stopSelf()
            delay(60000)
            stopSelf()
        }
    }

    /**
     * Called when the service is started. This method handles the start command, checks the service's
     * completion status, and starts an asynchronous task that stops the service after 60 seconds.
     *
     * @param intent The intent that was used to start the service.
     * @param flags Flags associated with the start request.
     * @param startId An identifier for the start request.
     * @return Returns a flag indicating the service's behavior on completion.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != STOP_SERVICE_ACTION) startSubscribeCoroutineWorker(this)
        closedServiceHandler(intent, this)
        return START_NOT_STICKY
    }

    /**
     * Called when a client binds to the service with `bindService()`.
     * The implementation of a communication channel for the service is not required.
     *
     * @param intent The `Intent` that was used to bind to this service.
     * @return The `IBinder` interface for interacting with the service.
     */
    override fun onBind(intent: Intent): IBinder {
        TODO("Not used in the current implementation.")
    }
}