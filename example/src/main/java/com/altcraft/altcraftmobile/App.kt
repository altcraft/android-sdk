package com.altcraft.altcraftmobile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Application
import com.altcraft.altcraftmobile.event.EventReceiver
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.initAltcraft
import com.altcraft.altcraftmobile.providers.fcm.FCMProvider
import com.altcraft.altcraftmobile.providers.hms.HMSProvider
import com.altcraft.altcraftmobile.providers.jwt.JWTProvider
import com.altcraft.altcraftmobile.providers.rustore.RuStoreProvider
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.push.push
import ru.rustore.sdk.pushclient.common.logger.DefaultLogger
import ru.rustore.sdk.universalpush.RuStoreUniversalPushClient
import ru.rustore.sdk.universalpush.rustore.providers.RuStorePushProvider

val icon = R.drawable.ic_altcraft_push_icon

//set the JWT value for the anonymous user as anonJWT or in the application interface (config)
internal val anonJWT: String? = null

//set the JWT value for the registered user as regJWT or in the application interface (config)
internal val regJWT: String? = null

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * Altcraft push setup
         *
         * 1. Place provider config files in the app module root:
         * * app/google-services.json  (Firebase Cloud Messaging)
         * * app/agconnect-services.json  (Huawei Push Kit)
         *
         * 2. In app/build.gradle.kts, uncomment the plugins and sync Gradle:
         * plugins {
         * ```
        alias(libs.plugins.android.application)
        ```
         * ```
        alias(libs.plugins.kotlin.android)
        ```
         * ```
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
        ```
         * ```
        alias(libs.plugins.google.gms.google.services) // uncomment
        ```
         * ```
        id("com.huawei.agconnect") // uncomment
        ```
         * }
         *
         * 3. Sync Gradle to apply changes.
         */

        RuStoreUniversalPushClient.init(
            context = this,
            rustore = RuStorePushProvider(
                application = this,
                projectId = "your-rustore-project-id",
                logger = DefaultLogger(tag = "rustore")
            )
        )

        EventReceiver.init(this)
        AltcraftSDK.auth.setJWTProvider(JWTProvider(this))
        AltcraftSDK.push.token.setFCMTokenProvider(FCMProvider())
        AltcraftSDK.push.token.setHMSTokenProvider(HMSProvider())
        AltcraftSDK.push.token.setRuStoreTokenProvider(RuStoreProvider())

        //Initialization of the SDK that accepts parameters from the application interface
        initAltcraft(this)

        /**
         * Configures the Altcraft SDK programmatically.
         *
         * The SDK can also be configured through the application UI (Config).
         *
         * Usage:
         * ```kotlin
         * val config = AltcraftConfiguration.Builder(
         *     apiUrl = "https://pxl-example.altcraft.com",
         *     rToken = null,
         *     appInfo = AppInfo(
         *         appID = "com.example.app",
         *         appIID = "8b91f3a0-1111-2222-3333-c1a2c1a2c1a2",
         *         appVer = "1.0.0"
         *     )
         * )
         *     .pushConfig(
         *         icon = R.drawable.ic_notification,
         *         providerPriority = listOf(
         *             FCM_PROVIDER,
         *             HMS_PROVIDER,
         *             RUS_PROVIDER
         *         ),
         *         pushReceiverModules = listOf(
         *             context.packageName,
         *             "com.example.push_receiver",
         *             "com.example.feature.test"
         *         ),
         *         pushChannelName = "Altcraft",
         *         pushChannelDescription = "Altcraft notifications channel"
         *     )
         *     .inAppConfig(
         *         inAppAutoRequest = true,
         *         placementsRequestInterval = 10.minutes,
         *         maxPlacements = 10
         *     )
         *     .build()
         *
         * AltcraftSDK.initialization(context, config)
         * ```
         */
    }
}