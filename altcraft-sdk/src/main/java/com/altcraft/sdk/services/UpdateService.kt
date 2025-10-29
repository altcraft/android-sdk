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
 * Foreground service that keeps a stable network connection during a token update flow
 * for up to 60 seconds, even if the app is closed or running in the background.
 *
 * A persistent notification is shown while the service is active (Android requirement).
 * Therefore, using this service is optional and can be configured in the SDK settings.
 *
 * Purpose: ensure token refresh requests and related network I/O can finish reliably
 * by keeping the process alive for a short window.
 */
@Keep
internal class UpdateService : Service() {

    /**
     * Promotes the service to the foreground; stops itself if the start fails.
     * Automatically stops after 60 seconds.
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
     * Handles the start command — runs the token update worker if needed
     * and processes stop requests from the intent.
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
     * Binding is not supported.
     *
     * @return Always returns null to indicate that binding is not supported.
     */
    override fun onBind(intent: Intent?): IBinder? = null
}
