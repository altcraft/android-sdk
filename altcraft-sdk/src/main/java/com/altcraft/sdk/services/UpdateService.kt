package com.altcraft.sdk.services

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Keep
import com.altcraft.sdk.data.Constants.STOP_SERVICE_ACTION
import com.altcraft.sdk.services.manager.ServiceManager.closedServiceHandler
import com.altcraft.sdk.services.manager.ServiceManager.checkStartForeground
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startUpdateCoroutineWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TokenUpdateService is an foreground service designed to ensure the guaranteed completion of the
 * token renewal process. The service is activated after calling the tokenUpdate() function.
 * The service shuts down after the tokenUpdate() function is completed or 60 seconds after the
 * service starts working. If the application user closes the application before the update function
 * is completed,TokenUpdateService will allow this process to be completed.
 */
@Keep
internal class UpdateService : Service() {

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
            if (!checkStartForeground(this@UpdateService)) stopSelf()
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
        if (intent?.action != STOP_SERVICE_ACTION) startUpdateCoroutineWorker(this)
        closedServiceHandler(intent, this)
        return START_NOT_STICKY
    }

    /**
     * Not used in the current implementation.
     * Returns the communication channel to the service (not implemented in this case).
     *
     * @param intent The intent used for binding to the service.
     * @return Returns an IBinder for communicating with the service, or null if binding is not required.
     */
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not used in the current implementation.")
    }
}