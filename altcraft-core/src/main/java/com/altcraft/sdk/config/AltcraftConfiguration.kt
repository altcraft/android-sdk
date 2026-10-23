package com.altcraft.sdk.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.data.models.CoreModels.PropertyModel.AppInfo
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.events.CoreEvents
import com.altcraft.sdk.events.SDKException.exception

/**
 * Configuration for initializing the Altcraft SDK.
 *
 * Contains the API URL, optional role token, application information,
 * logging settings, and configurations of optional SDK modules.
 *
 * Configurations of optional modules are collected through
 * [Builder.addModuleConfiguration].
 *
 * @property apiUrl The URL to the Altcraft API endpoint.
 * @property rToken The optional role token.
 * @property appInfo Optional application information.
 * @property enableLogging Flag indicating whether internal SDK logging is enabled.
 */
@Keep
class AltcraftConfiguration private constructor(
    val apiUrl: String,
    val rToken: String? = null,
    val appInfo: AppInfo? = null,
    val enableLogging: Boolean? = null,
    private val moduleConfigurations: Map<
            String, ModuleConfiguration
            > = emptyMap()
) {
    internal val apiUrlIsBlank = apiUrl.isBlank()
    internal val rTokenIsBlank = rToken?.isBlank() == true
    internal val valid = !apiUrlIsBlank && !rTokenIsBlank

    /**
     * Throws an exception if the SDK configuration is invalid.
     */
    internal fun error() {
        when {
            apiUrlIsBlank -> exception(CoreEvents.apiUrlIsBlank)
            rTokenIsBlank -> exception(CoreEvents.rTokenIsBlank)
        }
    }

    /**
     * Builder for constructing an instance of [AltcraftConfiguration].
     *
     * This builder provides a declarative way to configure the Altcraft SDK by setting all
     * required parameters at construction time.
     */
    @Keep
    class Builder(
        private val apiUrl: String,
        private val rToken: String? = null,
        private val appInfo: AppInfo? = null,
        private val enableLogging: Boolean? = null,
    ) {
        /** Configurations of optional modules indexed by their identifiers. */
        private val moduleConfigurations = mutableMapOf<String, ModuleConfiguration>()


        /**
         * Adds a configuration of an optional SDK module.
         *
         * The configuration is stored and returned as an immutable snapshot
         * by [build]. An existing configuration of the same module
         * is replaced.
         *
         * @param configuration Module configuration to add.
         * @return This builder for chained calls.
         */
        @Keep
        fun addModuleConfiguration(
            configuration: ModuleConfiguration
        ): Builder {
            moduleConfigurations[configuration.moduleId] = configuration
            return this
        }

        /**
         * Builds the SDK configuration.
         *
         * @return Constructed SDK configuration.
         */
        @Keep
        fun build(): AltcraftConfiguration {
            return AltcraftConfiguration(
                apiUrl = apiUrl,
                rToken = rToken,
                appInfo = appInfo,
                enableLogging = enableLogging,
                moduleConfigurations = moduleConfigurations.toMap()
            )
        }
    }

    /**
     * Returns the configuration of the specified optional module.
     *
     * @param moduleId Unique identifier of the module.
     * @return Module configuration, or `null` if it is not provided.
     */
    internal fun getModuleConfiguration(
        moduleId: String
    ): ModuleConfiguration? = moduleConfigurations[
        moduleId
    ]

    /**
     * Converts this configuration into a [ConfigurationEntity].
     *
     * @return Room entity containing the SDK configuration data.
     */
    internal fun toEntity() = ConfigurationEntity(
        apiUrl = apiUrl,
        rToken = rToken,
        appInfo = appInfo
    )
}