package com.altcraft.sdk.services.manager

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE
import androidx.core.app.ServiceCompat.stopForeground
import com.altcraft.altcraftsdk.R
import com.altcraft.sdk.additional.StringBuilder.serviceExceptionMessage
import com.altcraft.sdk.additional.SubFunction.checkingNotificationPermission
import com.altcraft.sdk.additional.SubFunction.isAppInForegrounded
import com.altcraft.sdk.additional.SubFunction.isOnline
import com.altcraft.sdk.additional.SubFunction.isServiceRunning
import com.altcraft.sdk.additional.SubFunction.logger
import com.altcraft.sdk.config.ConfigSetup.getConfig
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.Constants.DEFAULT_SERVICE
import com.altcraft.sdk.data.Constants.PUSH_SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.STOP_SERVICE_ACTION
import com.altcraft.sdk.data.Constants.SUBSCRIBE_SERVICE
import com.altcraft.sdk.data.Constants.TOKEN_UPDATE_SERVICE
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE
import com.altcraft.sdk.data.Constants.SERVICE_TYPE_DATA
import com.altcraft.sdk.data.Constants.DEFAULT_SERVICES_MESSAGE_BODY
import com.altcraft.sdk.data.Constants.SUB_SERVICE_MSG_ID
import com.altcraft.sdk.data.Constants.UNKNOWN_SERVICE_MSG_ID
import com.altcraft.sdk.data.Constants.UPDATE_SERVICE_MSG_ID
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.sdk_events.EventList.channelNotCreated
import com.altcraft.sdk.sdk_events.EventList.noInternetConnect
import com.altcraft.sdk.sdk_events.EventList.notificationErr
import com.altcraft.sdk.sdk_events.Events.error
import com.altcraft.sdk.extension.ExceptionExtension.exception
import com.altcraft.sdk.push.PushChannel.getChannelInfo
import com.altcraft.sdk.push.PushChannel.isChannelCreated
import com.altcraft.sdk.push.PushChannel.selectAndCreateChannel
import com.altcraft.sdk.push.PushChannel.versionsSupportChannels
import com.altcraft.sdk.push.PushPresenter.createNotification
import com.altcraft.sdk.services.SubscribeService
import com.altcraft.sdk.services.UpdateService
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startSubscribeCoroutineWorker
import com.altcraft.sdk.workers.coroutine.LaunchFunctions.startUpdateCoroutineWorker
import com.altcraft.sdk.workers.coroutine.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * `ServiceManager` object contains utility functions for managing and shutting down foreground services.
 * It provides methods to create intents to stop services, handle service shutdowns, and close services directly.
 */
internal object ServiceManager {

    /**
     * Determines whether a service can be started based on multiple conditions.
     *
     * If all conditions are met, the function returns `true`, indicating that the service can be started.
     * If an exception occurs during execution, it is logged and the function returns `false`.
     *
     * @param context The application context used for checking service and notification permissions.
     * @param config The configuration entity that determines whether the service usage is enabled.
     * @param service The class of the service to check for its running status.
     * @return `true` if the service can be started, `false` otherwise.
     */
    private fun startServicePrecondition(
        context: Context, config: ConfigurationEntity, service: Class<out Any>
    ): Boolean {
        return try {
            config.usingService &&
                    isAppInForegrounded() &&
                    !isServiceRunning(context, service) &&
                    checkingNotificationPermission(context)
        } catch (e: Exception) {
            error("startServicePrecondition", e)
            false
        }
    }

    /**
     * Starts the `PushSubscribeService` or schedules a corresponding `ListenableWorker` if necessary.
     *
     * @param context The application context used for service or worker initialization.
     * @param config The current configuration entity, which determines whether to use foreground services.
     */
    fun startSubscribeWorker(context: Context, config: ConfigurationEntity) {
        try {
            val service = SubscribeService::class.java
            when (startServicePrecondition(context, config, service)) {
                true -> context.startService(Intent(context, service))
                else -> startSubscribeCoroutineWorker(context)
            }
        } catch (e: Exception) {
            error("startSubscribeWorker", e)
        }
    }

    /**
     * Starts the `TokenUpdateService` or schedules a corresponding `ListenableWorker` if necessary.
     *
     * @param context The application context used for service or worker initialization.
     * @param config The current configuration entity, which determines whether to use foreground services.
     */
    fun startUpdateWorker(context: Context, config: ConfigurationEntity) {
        try {
            val service = UpdateService::class.java
            when (startServicePrecondition(context, config, service)) {
                true -> context.startService(Intent(context, service))
                else -> startUpdateCoroutineWorker(context)
            }
        } catch (e: Exception) {
            error("startUpdateWorker", e)
        }
    }

    /**
     * Creates an intent to close a specified foreground service.
     * The service class to be closed is specified as a function parameter.
     *
     * @param context The `Context` used to start the service.
     * @param service The `Class` of the service to be closed.
     */
    fun stopService(
        context: Context,
        service: Class<*>
    ) {
        try {
            if (isServiceRunning(context, service)) context.startService(
                Intent(context, service).apply { action = STOP_SERVICE_ACTION })
        } catch (e: Exception) {
            val serviceName = getServiceName(service)
            error("stopService", serviceExceptionMessage(serviceName, e))
        }
    }

    /**
     * Handles the closure status of a service when an intent is used to stop it.
     * It checks the intent for the stop action and a flag indicating if the service
     * closed successfully, then stops the service accordingly.
     *
     * @param intent The `Intent` that initiated the service closure. Can be `null`
     * if no intent was provided.
     * @param service The `Service` instance that is being stopped.
     */
    fun closedServiceHandler(
        intent: Intent?,
        service: Service
    ) {
        try {
            if (intent != null && intent.action == STOP_SERVICE_ACTION) {
                stopForeground(service, STOP_FOREGROUND_REMOVE)
                logger("${getServiceName(service)} is closed")
                service.stopSelf()
            }
        } catch (e: Exception) {
            error("closedServiceHandler", e)
        }
    }

    /**
     * Retrieves the predefined service name based on the provided service class.
     *
     * @param service The `Class` of the service to identify.
     * @return A string representing the service name.
     */
    private fun getServiceName(service: Class<*>): String {
        return getServiceName(service.javaClass)
    }

    /**
     * Retrieves the predefined service name based on the provided service class.
     *
     * @param service The `Class` of the service to identify.
     * @return A string representing the service name.
     */
    private fun getServiceName(service: Service): String {
        return when (service.javaClass) {
            SubscribeService::class.java -> PUSH_SUBSCRIBE_SERVICE
            UpdateService::class.java -> TOKEN_UPDATE_SERVICE
            else -> DEFAULT_SERVICE
        }
    }

    /**
     * Retrieves a unique identifier for a given service.
     *
     * This function assigns a distinct integer ID to each foreground service type,
     * which is used when starting the service in the foreground.
     *
     * @param service The service instance for which an ID is required.
     * @return An integer representing the unique ID for the given service.
     */
    private fun getId(service: Service): Int {
        return when (service) {
            is SubscribeService -> SUB_SERVICE_MSG_ID
            is UpdateService -> UPDATE_SERVICE_MSG_ID
            else -> UNKNOWN_SERVICE_MSG_ID
        }
    }

    /**
     * Creates a notification for starting foreground services.
     *
     * Uses configuration to get channel info, creates the channel if needed,
     * checks its existence, and builds the service notification.
     *
     * @param context Application context for accessing configuration and system services.
     * @return A [Notification] for foreground services, or `null` if creation fails.
     */
    internal suspend fun createServiceNotification(context: Context): Notification? {
        return try {
            val config = getConfig(context)
            val info = getChannelInfo(config)
            val icon = config?.icon ?: R.drawable.icon
            val body = config?.serviceMessage ?: DEFAULT_SERVICES_MESSAGE_BODY
            if (versionsSupportChannels) selectAndCreateChannel(context, info)
            if (!isChannelCreated(context, info)) exception(channelNotCreated)

            createNotification(context, info.first, body, icon)
        } catch (e: Exception) {
            error("createServiceNotification", e)
            null
        }
    }

    /**
     * Checks network connectivity and starts the given service in the foreground.
     *
     * Validates internet connection, builds a foreground notification, and starts the
     * service with it. Logs any error encountered and returns a result.
     *
     * @param service The [Service] instance to start in the foreground.
     * @return `true` if the service was successfully started; `false` if an error occurred.
     */
    suspend fun checkStartForeground(service: Service): Boolean {
        return try {
            if (!isOnline(service)) exception(noInternetConnect)
            val push = createServiceNotification(service) ?: exception(notificationErr)

            startForegroundService(service, getId(service), push)

            logger("${getServiceName(service)} is started")
            true
        } catch (e: Exception) {
            error("checkStartForeground", e)
            false
        }
    }

    /**
     * Starts the given service in the foreground with the appropriate method
     * depending on the Android version.
     *
     * @param service The service to start in the foreground.
     * @param id The notification ID.
     * @param notification The notification to display.
     */
    private fun startForegroundService(service: Service, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(id, notification, SERVICE_TYPE_DATA)
        } else {
            service.startForeground(id, notification)
        }
    }

    /**
     * Stops the specified background service and resets its retry counter.
     *
     * @param context The application context.
     * @param service The logical service name alias (e.g. [PUSH_SUBSCRIBE_SERVICE]).
     * @param delay If true, delays the shutdown by 500ms before stopping the service.
     */
    fun closeService(context: Context, service: String, delay: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            if (delay) delay(500)
            when (service) {
                PUSH_SUBSCRIBE_SERVICE -> {
                    Worker.retrySubscribe = 0
                    stopService(context, SUBSCRIBE_SERVICE)
                }

                TOKEN_UPDATE_SERVICE -> {
                    Worker.retryUpdate = 0
                    stopService(context, UPDATE_SERVICE)
                }
            }
        }
    }

    /**
     * Checks whether a service should be closed based on retry count and app visibility.
     * If the retry count reaches [Constants.COUNT_SERVICE_CLOSED], initiates delayed shutdown.
     *
     * @param context The application context.
     * @param service The logical service name alias to evaluate.
     * @param count Current retry count for the service operation.
     */
    fun checkServiceClosed(context: Context, service: String, count: Int) {
        when (isAppInForegrounded()) {
            true -> if (count == Constants.COUNT_SERVICE_CLOSED) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    closeService(context, service)
                }
            }

            false -> {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    closeService(context, service)
                }
            }
        }
    }
}