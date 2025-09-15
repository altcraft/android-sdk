package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.altcraft.sdk.data.Constants.allSignal
import com.altcraft.sdk.data.Constants.soundless
import com.altcraft.sdk.data.Constants.onlySound
import com.altcraft.sdk.data.room.ConfigurationEntity

/**
 * `PushChannel` manages creation and selection of Android notification channels
 * with different sound and vibration configurations, based on push notification settings.
 */
internal object PushChannel {

    internal val versionsSupportChannels = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /**
     * Creates a notification channel if the Android version supports it.
     *
     * @param context The application context used to access the notification service.
     * @param channel The notification channel to be created.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context, channel: NotificationChannel) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.createNotificationChannel(channel)
    }

    /**
     * Creates a notification channel that has both sound and vibration enabled.
     *
     * @param context The context used to create the notification channel.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAllSignalChannel(context: Context, info: Pair<String, String>) {
        createChannel(
            context,
            info,
            NotificationManager.IMPORTANCE_HIGH, enableSound = true, enableVibration = true
        )
    }

    /**
     * Creates a notification channel where both sound and vibration are disabled.
     *
     * @param context The context used to create the notification channel.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSoundlessChannel(context: Context, info: Pair<String, String>) {
        createChannel(
            context,
            info,
            NotificationManager.IMPORTANCE_LOW, enableSound = false, enableVibration = false
        )
    }

    /**
     * Creates a notification channel where sound is enabled but vibration is disabled.
     *
     * @param context The context used to create the notification channel.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOnlySoundChannel(context: Context, info: Pair<String, String>) {
        createChannel(
            context,
            info,
            NotificationManager.IMPORTANCE_DEFAULT, enableSound = true, enableVibration = false
        )
    }

    /**
     * Selects and creates a notification channel based on the given name.
     *
     * This function determines which predefined notification channel should be created
     * based on the provided name and then invokes the corresponding creation function.
     *
     * @param context The application context used to create the notification channel.
     * @param info The name and description for the notification channel.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun selectAndCreateChannel(context: Context, info: Pair<String, String>) {
        when {
            info.first.contains(allSignal.first) -> createAllSignalChannel(context, info)
            info.first.contains(onlySound.first) -> createOnlySoundChannel(context, info)
            info.first.contains(soundless.first) -> createSoundlessChannel(context, info)
        }
    }

    /**
     * Returns the channel name and description based on push settings and configuration.
     *
     * @param config Optional SDK configuration containing custom channel name and description.
     * @param pushData Push notification data with sound and vibration flags.
     * @return A pair of channel name and description.
     */
    fun getChannelInfo(
        config: ConfigurationEntity?,
        pushData: PushData? = null
    ): Pair<String, String> {
        val (name, descr) = config?.pushChannelName to config?.pushChannelDescription

        val (mode, description) = when {
            pushData == null || pushData.soundless -> soundless
            pushData.vibration -> allSignal
            else -> onlySound
        }

        val fullName = name?.let { "${it}_$mode" } ?: mode
        val fullDescription = descr?.let { "$it ($description)."} ?: description

        return fullName to fullDescription
    }

    /**
     * Creates a notification channel with custom sound and vibration settings.
     *
     * @param context App context.
     * @param info Channel name and description.
     * @param importance Channel importance level.
     * @param enableSound Enables sound if `true` (default `true`).
     * @param enableVibration Enables vibration if `true` (default `true`).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(
        context: Context,
        info: Pair<String, String>,
        importance: Int,
        enableSound: Boolean = true,
        enableVibration: Boolean = true
    ) {
        createChannel(
            context, NotificationChannel(info.first, info.first, importance).apply {
                description = info.second

                if (!enableSound) setSound(null, null)

                enableVibration(enableVibration)

                vibrationPattern = if (enableVibration) longArrayOf(0, 250, 250, 250) else null
            }
        )
    }

    /**
     * Checks if a notification channel with the given ID exists.
     *
     * @param context Application context.
     * @param channelInfo A pair of channel ID and name; only the ID is used for the check.
     * @return `true` if the channel exists or channels are not supported; `false` otherwise.
     */
    fun isChannelCreated(context: Context, channelInfo: Pair<String, String>): Boolean {
        if (!versionsSupportChannels) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(channelInfo.first) != null
    }
}