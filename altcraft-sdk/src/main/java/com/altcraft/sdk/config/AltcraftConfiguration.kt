package com.altcraft.sdk.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved..

import androidx.annotation.Keep
import com.altcraft.sdk.additional.StringBuilder.invalidConfigMsg
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.sdk_events.Events.error

/**
 * Altcraft SDK configuration initialization class.
 *
 * This class is responsible for initializing parameters for the Altcraft SDK,
 * including the API URL, role token authentication, app information, icon ID,
 * whether foreground services are used, and push notification settings.
 *
 * @property apiUrl The URL to the Altcraft API endpoint.
 * @property icon The value of the drawable resource; the ID of the icon that will be used when
 * displaying notifications.
 * @property rToken The role token.
 * @property usingService Flag indicating whether foreground services are used during the
 * subscription process and updating the device token values for profiles. Default is `true`.
 * @property serviceMessage The message text that will be displayed as the content of the notification
 * when foreground services are active. Default is `null`.
 * @property providerPriorityList A list of provider names in priority order for push notifications.
 * @property appInfo Contains app details used by Firebase Analytics (App ID, instance ID, version).
 * @property pushReceiverModules A list of strings representing package names of modules where the
 * `PushReceiver` class can be overridden.
 * @property pushChannelName The name of the push notification channel shown to the user.
 * @property pushChannelDescription The description of the push notification channel.
 */
@Suppress("MemberVisibilityCanBePrivate")
@Keep
class AltcraftConfiguration private constructor(
    private val apiUrl: String,
    private val icon: Int? = null,
    private val rToken: String? = null,
    private val usingService: Boolean = false,
    private val serviceMessage: String? = null,
    private val appInfo: DataClasses.AppInfo? = null,
    private val providerPriorityList: List<String>? = null,
    private val pushReceiverModules: List<String>? = null,
    private val pushChannelName: String? = null,
    private val pushChannelDescription: String? = null,
) {
    /**
     * Builder for constructing an instance of [AltcraftConfiguration].
     *
     * This builder provides a declarative way to configure the Altcraft SDK by setting all required
     * parameters at construction time.
     */
    @Keep
    class Builder(
        private val apiUrl: String,
        private val icon: Int? = null,
        private val rToken: String? = null,
        private val usingService: Boolean = false,
        private val serviceMessage: String? = null,
        private val appInfo: DataClasses.AppInfo? = null,
        private val providerPriorityList: List<String>? = null,
        private val pushReceiverModules: List<String>? = null,
        private val pushChannelName: String? = null,
        private val pushChannelDescription: String? = null,
    ) {
        fun build(): AltcraftConfiguration {
            return AltcraftConfiguration(
                apiUrl,
                icon,
                rToken,
                usingService,
                serviceMessage,
                appInfo,
                providerPriorityList,
                pushReceiverModules,
                pushChannelName,
                pushChannelDescription
            )
        }
    }

    /** Returns the Altcraft API base URL. */
    fun getApiUrl(): String = apiUrl

    /** Returns the icon resource ID used in push notifications. */
    fun getIcon(): Int? = icon

    /** Returns the optional resource token if set. */
    fun getRToken(): String? = rToken

    /** Indicates whether foreground services are enabled. */
    fun getUsingService(): Boolean = usingService

    /** Returns the custom message for foreground service notifications. */
    fun getServiceMessage(): String? = serviceMessage

    /** Returns app metadata used by Firebase Analytics, if provided. */
    fun getAppInfo(): DataClasses.AppInfo? = appInfo

    /** Defines the priority order of providers for push notifications. */
    fun getProviderPriorityList(): List<String>? = providerPriorityList

    /** Returns the list of modules supporting custom PushReceiver overrides. */
    fun getPushReceiverModules(): List<String>? = pushReceiverModules

    /** Returns the custom channel name for push notifications. */
    fun getPushChannelName(): String? = pushChannelName

    /** Returns the custom channel description for push notifications. */
    fun getPushChannelDescription(): String? = pushChannelDescription

    /**
     * Converts this configuration into a Room entity.
     *
     * This is used internally by the SDK to persist configuration in the local database.
     * Returns `null` if configuration is invalid.
     */
    internal fun toEntity(): ConfigurationEntity? {
        val entity = ConfigurationEntity(
            id = 1,
            icon = getIcon(),
            apiUrl = getApiUrl(),
            rToken = getRToken(),
            appInfo = getAppInfo(),
            usingService = getUsingService(),
            serviceMessage = getServiceMessage(),
            providerPriorityList = getProviderPriorityList(),
            pushReceiverModules = getPushReceiverModules(),
            pushChannelName = getPushChannelName(),
            pushChannelDescription = getPushChannelDescription()
        )
        return if (entity.isValid()) entity else {
            error("configToEntity", 470 to invalidConfigMsg(entity))
            null
        }
    }
}
